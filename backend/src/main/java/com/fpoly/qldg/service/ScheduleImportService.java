package com.fpoly.qldg.service;

import com.fpoly.qldg.domain.*;
import com.fpoly.qldg.repo.Repos.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ScheduleImportService {
    private final ImportJobRepo jobRepo;
    private final ImportRowRepo rowRepo;
    private final NewSubjectAlertRepo alertRepo;
    private final NotificationRepo notiRepo;
    private final SemesterRepo semesterRepo;
    private final UserRepo userRepo;
    private final SubjectRepo subjectRepo;
    private final TeachingRepo teachingRepo;
    private final CalendarSessionRepo calendarRepo;
    private final FacultyRepo facultyRepo;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public ScheduleImportService(
            ImportJobRepo jobRepo, ImportRowRepo rowRepo, NewSubjectAlertRepo alertRepo,
            NotificationRepo notiRepo, SemesterRepo semesterRepo, UserRepo userRepo,
            SubjectRepo subjectRepo, TeachingRepo teachingRepo, CalendarSessionRepo calendarRepo,
            FacultyRepo facultyRepo) {
        this.jobRepo = jobRepo;
        this.rowRepo = rowRepo;
        this.alertRepo = alertRepo;
        this.notiRepo = notiRepo;
        this.semesterRepo = semesterRepo;
        this.userRepo = userRepo;
        this.subjectRepo = subjectRepo;
        this.teachingRepo = teachingRepo;
        this.calendarRepo = calendarRepo;
        this.facultyRepo = facultyRepo;
    }

    /** Tạo task vào hàng đợi queue_tasks (UC-07) */
    public ImportJob enqueueImport(MultipartFile file, AppUser uploader) throws Exception {
        String name = file.getOriginalFilename();
        if (name == null || (!name.endsWith(".xlsx") && !name.endsWith(".xls"))) {
            throw new IllegalArgumentException("Chỉ chấp nhận file Excel định dạng .xlsx hoặc .xls");
        }
        Path dir = Paths.get(uploadDir);
        Files.createDirectories(dir);
        String storedName = UUID.randomUUID() + "_" + name;
        Path target = dir.resolve(storedName);
        file.transferTo(target);

        ImportJob job = new ImportJob();
        job.fileName = name;
        job.storedPath = target.toString();
        job.fileSize = file.getSize();
        job.campus = uploader.campus;
        job.uploader = uploader;
        job.status = "PENDING";
        job = jobRepo.save(job);

        // Chạy bất đồng bộ
        processJobAsync(job.id);
        return job;
    }

    /** Xử lý import ngầm (UC-07, UC-08, UC-09) */
    public void processJobAsync(Long jobId) {
        new Thread(() -> processJob(jobId)).start();
    }

    public synchronized void processJob(Long jobId) {
        ImportJob job = jobRepo.findById(jobId).orElse(null);
        if (job == null) return;
        job.status = "PROCESSING";
        job.progress = 5;
        jobRepo.save(job);

        List<Semester> semesters = semesterRepo.findAll();
        Map<String, NewSubjectAlert> alerts = new HashMap<>();

        try (InputStream is = new FileInputStream(job.storedPath);
             Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();
            job.totalRows = Math.max(0, lastRow); // trừ dòng header

            // Dòng 0: Header (Class, Subject, SubjectName, Teacher, Date, Shift, Room, SessionNo)
            for (int r = 1; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                processSingleRow(job, row, r, semesters, alerts);
                job.progress = (int) Math.min(95, 5 + ((double) r / lastRow) * 90);
                if (r % 10 == 0) jobRepo.save(job);
            }

            // Lưu cảnh báo môn học mới
            for (NewSubjectAlert alert : alerts.values()) {
                alertRepo.save(alert);
            }

            job.progress = 100;
            job.status = (job.errors > 0) ? "DONE_WITH_ERRORS" : "DONE";
            job.finishedAt = LocalDateTime.now();
            job.message = String.format("Thành công: %d, Cập nhật: %d, Bỏ qua: %d, Lỗi: %d",
                    job.inserted, job.updated, job.skipped, job.errors);
            jobRepo.save(job);

            // Bắn notification cho TC&QLĐT
            Notification noti = new Notification();
            noti.user = job.uploader;
            noti.message = "File lịch dạy '" + job.fileName + "' đã xử lý xong. " +
                           (job.errors > 0 ? "Có " + job.errors + " dòng lỗi." : "Hoàn thành không lỗi.");
            noti.link = "/import/" + job.id;
            notiRepo.save(noti);

            if (!alerts.isEmpty()) {
                Notification alertNoti = new Notification();
                alertNoti.user = job.uploader;
                alertNoti.message = "Phát hiện " + alerts.size() + " môn học mới. Vui lòng vào Môn & Bộ môn để cập nhật tên chính thức.";
                alertNoti.link = "/import/" + job.id;
                notiRepo.save(alertNoti);
            }
        } catch (Exception e) {
            job.status = "FAILED";
            job.message = "Lỗi xử lý file: " + e.getMessage();
            job.finishedAt = LocalDateTime.now();
            jobRepo.save(job);
        }
    }

    private void processSingleRow(
            ImportJob job, Row row, int r, List<Semester> semesters,
            Map<String, NewSubjectAlert> alerts) {
        ImportRow ir = new ImportRow();
        ir.job = job;
        ir.rowNo = r;

        String classCode = getCellString(row.getCell(0));
        String subjectCode = getCellString(row.getCell(1));
        String subjectName = getCellString(row.getCell(2));
        String teacherAcc = getCellString(row.getCell(3));
        String dateStr = getCellString(row.getCell(4));
        int shift = (int) getCellNumber(row.getCell(5), 1);
        String room = getCellString(row.getCell(6));
        Integer sessionNo = (int) getCellNumber(row.getCell(7), 0);
        if (sessionNo == 0) sessionNo = null;

        ir.classCode = classCode;
        ir.subjectCode = subjectCode;
        ir.teacherAccount = teacherAcc;
        ir.sessionNo = sessionNo;

        if (classCode.isBlank() || subjectCode.isBlank() || teacherAcc.isBlank()) {
            ir.result = "ERROR";
            ir.message = "Thiếu thông tin bắt buộc: Lớp, Môn hoặc Giảng viên";
            job.errors++;
            rowRepo.save(ir);
            return;
        }

        LocalDate sessionDate = parseDate(dateStr);
        if (sessionDate == null) {
            ir.result = "ERROR";
            ir.message = "Ngày học không hợp lệ: '" + dateStr + "'";
            job.errors++;
            rowRepo.save(ir);
            return;
        }

        // 1. Xác định kỳ & block
        Semester matchedSemester = null;
        int block = 1;
        for (Semester s : semesters) {
            if (!sessionDate.isBefore(s.startDate) && !sessionDate.isAfter(s.endDate)) {
                matchedSemester = s;
                block = !sessionDate.isAfter(s.block1End) ? 1 : 2;
                break;
            }
        }
        if (matchedSemester == null) {
            ir.result = "ERROR";
            ir.message = "Ngày " + sessionDate + " không thuộc kỳ học nào đã cấu hình";
            job.errors++;
            rowRepo.save(ir);
            return;
        }

        // 2. Tìm giảng viên
        Optional<AppUser> optTeacher = userRepo.findByAccountAndCampusId(teacherAcc, job.campus.id);
        if (optTeacher.isEmpty()) {
            ir.result = "ERROR";
            ir.message = "Không tìm thấy giảng viên '" + teacherAcc + "' tại cơ sở " + job.campus.name;
            job.errors++;
            rowRepo.save(ir);
            return;
        }
        AppUser teacher = optTeacher.get();

        // 3. Tìm hoặc tự tạo môn học (UC-09)
        Subject subject = subjectRepo.findByCode(subjectCode).orElse(null);
        if (subject == null) {
            subject = new Subject();
            subject.code = subjectCode;
            subject.autoCreated = true;

            // Kiểm tra mã bản cập nhật (VD COM1081 của COM108)
            String baseCode = findBaseSubjectCode(subjectCode);
            Optional<Subject> baseOpt = baseCode != null ? subjectRepo.findByCode(baseCode) : Optional.empty();

            if (baseOpt.isPresent()) {
                subject.name = baseOpt.get().name;
                subject.faculty = baseOpt.get().faculty;
                alerts.computeIfAbsent(subjectCode, k -> {
                    NewSubjectAlert a = new NewSubjectAlert();
                    a.job = job;
                    a.subject = null;
                    a.kind = "UPDATE_VERSION";
                    a.baseCode = baseCode;
                    return a;
                }).classCount++;
            } else {
                subject.name = (subjectName.isBlank()) ? "[Chưa đặt tên - " + subjectCode + "]" : subjectName;
                // Gán tạm bộ môn đầu tiên của hệ thống
                subject.faculty = facultyRepo.findAll().stream().findFirst().orElseThrow();
                alerts.computeIfAbsent(subjectCode, k -> {
                    NewSubjectAlert a = new NewSubjectAlert();
                    a.job = job;
                    a.subject = null;
                    a.kind = "NEW";
                    return a;
                }).classCount++;
            }
            subject = subjectRepo.save(subject);
            NewSubjectAlert alert = alerts.get(subjectCode);
            if (alert != null) alert.subject = subject;
        }

        // 4. Tìm hoặc tạo Teaching (phân công)
        Teaching teaching = teachingRepo.findByClassCodeAndSubjectIdAndSemesterIdAndBlockAndTeacherId(
                classCode, subject.id, matchedSemester.id, block, teacher.id).orElse(null);
        if (teaching == null) {
            teaching = new Teaching();
            teaching.classCode = classCode;
            teaching.subject = subject;
            teaching.semester = matchedSemester;
            teaching.block = block;
            teaching.teacher = teacher;
            teaching.campus = job.campus;
            teaching = teachingRepo.save(teaching);
        }

        // 5. Đối soát buổi học (UC-08: Idempotency & Reconciliation)
        CalendarSession existing = null;
        if (sessionNo != null && sessionNo > 0) {
            List<CalendarSession> byNo = calendarRepo.findByTeachingIdAndSessionNo(teaching.id, sessionNo);
            if (!byNo.isEmpty()) existing = byNo.get(0);
        }
        if (existing == null) {
            List<CalendarSession> byDateShift = calendarRepo.findByTeachingIdAndDateAndShift(
                    teaching.id, sessionDate, shift);
            if (!byDateShift.isEmpty()) existing = byDateShift.get(0);
        }

        if (existing == null) {
            // Buổi mới -> INSERTED
            CalendarSession s = new CalendarSession();
            s.teaching = teaching;
            s.teacher = teacher;
            s.date = sessionDate;
            s.shift = shift;
            s.room = room.isBlank() ? "N/A" : room;
            s.sessionNo = sessionNo;
            calendarRepo.save(s);
            ir.result = "INSERTED";
            ir.newValue = sessionDate + " (Ca " + shift + ", " + room + ")";
            job.inserted++;
        } else {
            // Đối soát sự thay đổi
            boolean sameRoom = existing.room.equalsIgnoreCase(room);
            boolean sameDate = existing.date.equals(sessionDate);
            boolean sameShift = existing.shift == shift;

            if (sameRoom && sameDate && sameShift) {
                // Trùng hoàn toàn -> Bỏ qua
                ir.result = "SKIPPED";
                ir.oldValue = existing.date + " Ca " + existing.shift + " " + existing.room;
                ir.message = "Trùng hoàn toàn buổi học đã có";
                job.skipped++;
            } else if (!sameRoom && sameDate && sameShift) {
                // Đổi phòng
                ir.result = "UPDATED_ROOM";
                ir.oldValue = existing.room;
                ir.newValue = room;
                existing.room = room;
                calendarRepo.save(existing);
                job.updated++;
            } else {
                // Đổi ngày/ca hoặc hoán đổi
                ir.result = "UPDATED_TIME";
                ir.oldValue = existing.date + " Ca " + existing.shift;
                ir.newValue = sessionDate + " Ca " + shift;
                existing.date = sessionDate;
                existing.shift = shift;
                if (!room.isBlank()) existing.room = room;
                calendarRepo.save(existing);
                job.updated++;
            }
        }
        rowRepo.save(ir);
    }

    private String findBaseSubjectCode(String code) {
        // COM1081 -> COM108
        if (code.matches(".*\\d{4,}$")) {
            return code.substring(0, code.length() - 1);
        }
        return null;
    }

    private String getCellString(Cell c) {
        if (c == null) return "";
        DataFormatter df = new DataFormatter();
        return df.formatCellValue(c).trim();
    }

    private double getCellNumber(Cell c, double defVal) {
        if (c == null) return defVal;
        try {
            if (c.getCellType() == CellType.NUMERIC) return c.getNumericCellValue();
            String s = getCellString(c);
            return Double.parseDouble(s);
        } catch (Exception e) {
            return defVal;
        }
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        DateTimeFormatter[] fmtList = new DateTimeFormatter[]{
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd")
        };
        for (DateTimeFormatter f : fmtList) {
            try { return LocalDate.parse(s, f); } catch (Exception ignored) {}
        }
        return null;
    }

    /** Tạo file Excel mẫu lịch dạy có đầy đủ cột */
    public byte[] createSampleTemplate() throws Exception {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("LichDay");
            Row h = sheet.createRow(0);
            String[] cols = {"Mã lớp", "Mã môn", "Tên môn", "Tài khoản GV", "Ngày dạy (yyyy-MM-dd)", "Ca (1-6)", "Phòng", "Số thứ tự buổi"};
            for (int i = 0; i < cols.length; i++) h.createCell(i).setCellValue(cols[i]);

            Object[][] sample = {
                    {"WD18301", "COM108", "Nhập môn lập trình", "namnv", "2026-10-20", 2, "F201", 1},
                    {"WD18301", "COM108", "Nhập môn lập trình", "namnv", "2026-10-22", 2, "F201", 2},
                    {"WD18301", "COM108", "Nhập môn lập trình", "namnv", "2026-10-24", 2, "F201", 3},
                    {"CP18301", "COM1081", "Nhập môn lập trình nâng cao", "linhdt", "2026-10-21", 3, "F202", 1},
                    {"CP18301", "COM1081", "Nhập môn lập trình nâng cao", "linhdt", "2026-10-23", 3, "F202", 2},
                    {"PR18301", "PRO2113", "Dự án tốt nghiệp", "tunglt", "2026-10-25", 1, "L102", 1}
            };
            for (int r = 0; r < sample.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < sample[r].length; c++) {
                    Object val = sample[r][c];
                    if (val instanceof Number n) row.createCell(c).setCellValue(n.doubleValue());
                    else row.createCell(c).setCellValue(String.valueOf(val));
                }
            }
            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);
            wb.write(out);
            return out.toByteArray();
        }
    }
}
