package com.fpoly.qldg.controller;

import com.fpoly.qldg.domain.*;
import com.fpoly.qldg.repo.Repos.*;
import com.fpoly.qldg.service.ScheduleImportService;
import com.fpoly.qldg.service.SecurityScopeService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.*;

@RestController
@RequestMapping("/api/import")
@CrossOrigin(origins = "*")
public class ImportController {
    private final ScheduleImportService importService;
    private final SecurityScopeService scopeService;
    private final ImportJobRepo jobRepo;
    private final ImportRowRepo rowRepo;
    private final NewSubjectAlertRepo alertRepo;

    public ImportController(
            ScheduleImportService importService, SecurityScopeService scopeService,
            ImportJobRepo jobRepo, ImportRowRepo rowRepo, NewSubjectAlertRepo alertRepo) {
        this.importService = importService;
        this.scopeService = scopeService;
        this.jobRepo = jobRepo;
        this.rowRepo = rowRepo;
        this.alertRepo = alertRepo;
    }

    /** Tải file mẫu lịch dạy */
    @GetMapping("/sample-template")
    public ResponseEntity<byte[]> downloadSampleTemplate() throws Exception {
        byte[] bytes = importService.createSampleTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Mau_LichDay_QLDG.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    /** UC-07: Upload file lịch dạy vào hàng đợi */
    @PostMapping("/upload")
    public ResponseEntity<?> upload(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            AppUser uploader = scopeService.resolveUser(authHeader);
            if (uploader == null) return ResponseEntity.status(401).body(Map.of("message", "Chưa xác thực"));
            ImportJob job = importService.enqueueImport(file, uploader);
            return ResponseEntity.ok(Map.of(
                    "jobId", job.id,
                    "message", "Đã tiếp nhận file vào hàng đợi xử lý nền. Bạn có thể đóng trình duyệt",
                    "fileName", job.fileName,
                    "status", job.status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
        }
    }

    /** Lịch sử import (phân trang) */
    @GetMapping("/jobs")
    public ResponseEntity<?> getJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        AppUser u = scopeService.resolveUser(authHeader);
        if (u == null) return ResponseEntity.status(401).body("Chưa xác thực");
        Page<ImportJob> p = jobRepo.findByCampusIdOrderByCreatedAtDesc(u.campus.id, PageRequest.of(page, size));
        return ResponseEntity.ok(p);
    }

    /** Chi tiết một lần import: Tab Tổng quan (UC-08) */
    @GetMapping("/jobs/{id}")
    public ResponseEntity<?> getJobDetail(@PathVariable Long id) {
        ImportJob job = jobRepo.findById(id).orElse(null);
        if (job == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(job);
    }

    /** Tab Chi tiết đối soát (lọc theo result, phân trang) */
    @GetMapping("/jobs/{id}/rows")
    public ResponseEntity<?> getJobRows(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String result,
            @RequestParam(required = false) String search) {
        // Lấy danh sách để lọc linh hoạt
        List<ImportRow> all = rowRepo.findByJobId(id);
        var stream = all.stream();
        if (result != null && !result.isBlank() && !"ALL".equalsIgnoreCase(result)) {
            stream = stream.filter(r -> result.equalsIgnoreCase(r.result));
        }
        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase().trim();
            stream = stream.filter(r ->
                    (r.classCode != null && r.classCode.toLowerCase().contains(q)) ||
                    (r.subjectCode != null && r.subjectCode.toLowerCase().contains(q)) ||
                    (r.teacherAccount != null && r.teacherAccount.toLowerCase().contains(q)));
        }
        List<ImportRow> filtered = stream.toList();
        int total = filtered.size();
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        List<ImportRow> pageContent = filtered.subList(from, to);

        Map<String, Object> res = new HashMap<>();
        res.put("content", pageContent);
        res.put("totalElements", total);
        res.put("totalPages", (int) Math.ceil((double) total / size));
        res.put("currentPage", page);
        return ResponseEntity.ok(res);
    }

    /** Tab Môn học mới (UC-09) */
    @GetMapping("/jobs/{id}/alerts")
    public ResponseEntity<?> getJobAlerts(@PathVariable Long id) {
        List<NewSubjectAlert> alerts = alertRepo.findByJobId(id);
        List<Map<String, Object>> res = alerts.stream().map(a -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", a.id);
            m.put("subjectCode", a.subject != null ? a.subject.code : "");
            m.put("subjectName", a.subject != null ? a.subject.name : "");
            m.put("kind", a.kind);
            m.put("baseCode", a.baseCode);
            m.put("classCount", a.classCount);
            return m;
        }).toList();
        return ResponseEntity.ok(res);
    }

    /** Xuất file Excel các dòng bị lỗi (UC-08) */
    @GetMapping("/jobs/{id}/export-errors")
    public ResponseEntity<byte[]> exportErrors(@PathVariable Long id) throws Exception {
        List<ImportRow> errors = rowRepo.findByJobId(id).stream()
                .filter(r -> "ERROR".equalsIgnoreCase(r.result)).toList();

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet s = wb.createSheet("Dong_Loi");
            Row h = s.createRow(0);
            String[] headers = {"Dòng", "Mã lớp", "Mã môn", "Tài khoản GV", "Số TT buổi", "Lý do lỗi"};
            for (int i = 0; i < headers.length; i++) h.createCell(i).setCellValue(headers[i]);

            int rowIdx = 1;
            for (ImportRow r : errors) {
                Row row = s.createRow(rowIdx++);
                row.createCell(0).setCellValue(r.rowNo);
                row.createCell(1).setCellValue(r.classCode != null ? r.classCode : "");
                row.createCell(2).setCellValue(r.subjectCode != null ? r.subjectCode : "");
                row.createCell(3).setCellValue(r.teacherAccount != null ? r.teacherAccount : "");
                row.createCell(4).setCellValue(r.sessionNo != null ? String.valueOf(r.sessionNo) : "");
                row.createCell(5).setCellValue(r.message != null ? r.message : "");
            }
            for (int i = 0; i < headers.length; i++) s.autoSizeColumn(i);
            wb.write(out);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Danh_sach_dong_loi_Job" + id + ".xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(out.toByteArray());
        }
    }
}
