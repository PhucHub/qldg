package com.fpoly.qldg.service;

import com.fpoly.qldg.domain.*;
import com.fpoly.qldg.repo.Repos.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InspectionPlanService {
    private final TeachingRepo teachingRepo;
    private final CalendarSessionRepo calendarRepo;
    private final InspectionRepo inspectionRepo;
    private final InspectionDetailRepo detailRepo;
    private final InspectionHistoryRepo historyRepo;
    private final UserRepo userRepo;
    private final FacultyRepo facultyRepo;
    private final SemesterRepo semesterRepo;
    private final NotificationRepo notiRepo;

    public InspectionPlanService(
            TeachingRepo teachingRepo, CalendarSessionRepo calendarRepo,
            InspectionRepo inspectionRepo, InspectionDetailRepo detailRepo,
            InspectionHistoryRepo historyRepo, UserRepo userRepo,
            FacultyRepo facultyRepo, SemesterRepo semesterRepo,
            NotificationRepo notiRepo) {
        this.teachingRepo = teachingRepo;
        this.calendarRepo = calendarRepo;
        this.inspectionRepo = inspectionRepo;
        this.detailRepo = detailRepo;
        this.historyRepo = historyRepo;
        this.userRepo = userRepo;
        this.facultyRepo = facultyRepo;
        this.semesterRepo = semesterRepo;
        this.notiRepo = notiRepo;
    }

    // --- DTOs ---
    public record QuotaDto(int totalTeachers, int requiredQuota, int plannedTeachers, int percent) {}

    public record ClassItemDto(
            Long teachingId, String classCode, String subjectCode, String subjectName,
            Long facultyId, String facultyName, String teacherAccount, String teacherName,
            int sessionCount, String groupType, String reasonBadge, String existingPlanCode) {}

    public record ClassificationResultDto(
            QuotaDto quota,
            List<ClassItemDto> mandatory,
            List<ClassItemDto> others,
            List<ClassItemDto> alreadyPlanned) {}

    public record ValidSessionDto(Long sessionId, LocalDate date, int shift, String room, Integer sessionNo, String label) {}

    public record InspectorCandidateDto(
            Long id, String account, String fullName, String staffCode,
            String facultyCode, String facultyName, String campusCode, String campusName,
            boolean sameCampus) {}

    /** UC-09 & UC-10: Tính chỉ tiêu 15% và phân loại 3 nhóm lớp */
    public ClassificationResultDto classifyClasses(
            Long campusId, Long semesterId, int block, Long facultyId, List<Long> scopeFacultyIds) {
        Semester resolvedSemester;
        if (semesterId == null) {
            resolvedSemester = semesterRepo.findAllByOrderByOrdinalDesc().stream().findFirst().orElseThrow();
            semesterId = resolvedSemester.id;
        } else {
            resolvedSemester = semesterRepo.findById(semesterId).orElse(null);
            if (resolvedSemester == null) {
                resolvedSemester = semesterRepo.findAllByOrderByOrdinalDesc().stream().findFirst().orElseThrow();
                semesterId = resolvedSemester.id;
            }
        }
        final int curOrdinal = resolvedSemester.ordinal;
        List<Long> allowedScopeIds = (scopeFacultyIds != null && !scopeFacultyIds.isEmpty())
                ? scopeFacultyIds
                : facultyRepo.findAll().stream().map(f -> f.id).toList();

        List<Long> targetFacultyIds;
        if (facultyId != null && facultyId > 0) {
            targetFacultyIds = allowedScopeIds.contains(facultyId) ? List.of(facultyId) : List.of(facultyId);
        } else {
            targetFacultyIds = allowedScopeIds;
        }

        List<Teaching> teachings;
        if (targetFacultyIds.isEmpty()) {
            teachings = teachingRepo.findByCampusIdAndSemesterIdAndBlock(campusId, semesterId, block);
        } else {
            teachings = teachingRepo.findForFaculties(campusId, semesterId, block, targetFacultyIds);
        }

        // Lọc chỉ các lớp có lịch dạy thật (ca 1-6) và giảng viên cùng cơ sở (overview mục 5 & GBR-03)
        teachings = teachings.stream()
                .filter(t -> calendarRepo.existsValidSessionByTeachingId(t.id))
                .filter(t -> t.teacher != null && t.teacher.campus != null && t.teacher.campus.id.equals(campusId))
                .toList();

        // 1. Mẫu số: Tổng số GV phân biệt có lớp dạy trong nhóm bộ môn trong block
        Set<Long> teacherIdsWithClasses = teachings.stream().map(t -> t.teacher.id).collect(Collectors.toSet());
        int totalTeachers = teacherIdsWithClasses.size();
        int requiredQuota = (int) Math.ceil(totalTeachers * 0.15);

        // Giảng viên đã có trong kế hoạch của cơ sở & bộ môn đang xem trong block
        List<InspectionDetail> activeDetails = detailRepo.findActiveInCampusAndBlock(campusId, semesterId, block);
        Map<Long, String> classToPlanCode = new HashMap<>();
        Set<Long> plannedTeacherIds = new HashSet<>();
        Set<Long> targetFacSet = new HashSet<>(targetFacultyIds);

        for (InspectionDetail d : activeDetails) {
            classToPlanCode.put(d.teaching.id, d.inspection.code);
            if (d.teaching.subject != null && d.teaching.subject.faculty != null
                    && targetFacSet.contains(d.teaching.subject.faculty.id)
                    && teacherIdsWithClasses.contains(d.teaching.teacher.id)) {
                plannedTeacherIds.add(d.teaching.teacher.id);
            }
        }
        int plannedCount = plannedTeacherIds.size();
        int percent = (requiredQuota == 0) ? 100 : (int) Math.min(100, Math.round(((double) plannedCount / requiredQuota) * 100));
        QuotaDto quota = new QuotaDto(totalTeachers, requiredQuota, plannedCount, percent);

        List<ClassItemDto> mandatory = new ArrayList<>();
        List<ClassItemDto> others = new ArrayList<>();
        List<ClassItemDto> alreadyPlanned = new ArrayList<>();

        for (Teaching t : teachings) {
            List<CalendarSession> sessions = calendarRepo.findByTeachingIdOrderByDateAscShiftAsc(t.id);
            int count = sessions.size();

            if (classToPlanCode.containsKey(t.id)) {
                alreadyPlanned.add(new ClassItemDto(
                        t.id, t.classCode, t.subject.code, t.subject.name,
                        t.subject.faculty.id, t.subject.faculty.name,
                        t.teacher.account, t.teacher.fullName,
                        count, "ALREADY_PLANNED", null, classToPlanCode.get(t.id)));
                continue;
            }

            // Thuật toán: Lần đầu dạy / Nghỉ 6 kỳ liên tiếp
            List<Teaching> history = teachingRepo.findHistoryByTeacherAndSubject(t.teacher.id, t.subject.code);
            // Lọc ra các kỳ trước kỳ hiện tại
            List<Teaching> past = history.stream().filter(h -> h.semester.ordinal < curOrdinal).toList();

            if (past.isEmpty()) {
                // Chưa từng dạy mã này -> Lần đầu dạy (COM1081 khác COM108 tính là mới)
                mandatory.add(new ClassItemDto(
                        t.id, t.classCode, t.subject.code, t.subject.name,
                        t.subject.faculty.id, t.subject.faculty.name,
                        t.teacher.account, t.teacher.fullName,
                        count, "MANDATORY", "Lần đầu dạy", null));
            } else {
                int lastOrdinal = past.get(0).semester.ordinal;
                int gap = curOrdinal - lastOrdinal;
                if (gap >= 7) {
                    mandatory.add(new ClassItemDto(
                            t.id, t.classCode, t.subject.code, t.subject.name,
                            t.subject.faculty.id, t.subject.faculty.name,
                            t.teacher.account, t.teacher.fullName,
                            count, "MANDATORY", "Khác – nghỉ 6 kỳ liên tiếp", null));
                } else {
                    others.add(new ClassItemDto(
                            t.id, t.classCode, t.subject.code, t.subject.name,
                            t.subject.faculty.id, t.subject.faculty.name,
                            t.teacher.account, t.teacher.fullName,
                            count, "OTHERS", null, null));
                }
            }
        }

        return new ClassificationResultDto(quota, mandatory, others, alreadyPlanned);
    }

    /** UC-12: Danh sách lớp cho kế hoạch đột xuất (gom theo GV, hiện nhãn) */
    public List<Map<String, Object>> getUnscheduledCandidates(
            Long campusId, Long semesterId, int block, Long facultyId, List<Long> scopeFacultyIds) {
        if (semesterId == null) {
            semesterId = semesterRepo.findAllByOrderByOrdinalDesc().stream().map(s -> s.id).findFirst().orElse(1L);
        }
        List<Long> targetFacultyIds = (facultyId != null && facultyId > 0) ? List.of(facultyId) : scopeFacultyIds;
        if (targetFacultyIds == null || targetFacultyIds.isEmpty()) {
            targetFacultyIds = facultyRepo.findAll().stream().map(f -> f.id).toList();
        }

        List<Teaching> teachings;
        if (targetFacultyIds.isEmpty()) {
            teachings = teachingRepo.findByCampusIdAndSemesterIdAndBlock(campusId, semesterId, block);
        } else {
            teachings = teachingRepo.findForFaculties(campusId, semesterId, block, targetFacultyIds);
        }
        List<InspectionDetail> activeDetails = detailRepo.findActiveInCampusAndBlock(campusId, semesterId, block);
        Map<Long, String> classToPlan = activeDetails.stream()
                .collect(Collectors.toMap(d -> d.teaching.id, d -> d.inspection.code, (a, b) -> a));

        // Gom theo giảng viên (chỉ lớp có lịch dạy ca 1-6 và GV cùng cơ sở)
        Map<AppUser, List<Teaching>> byTeacher = teachings.stream()
                .filter(t -> calendarRepo.existsValidSessionByTeachingId(t.id))
                .filter(t -> t.teacher != null && t.teacher.campus != null && t.teacher.campus.id.equals(campusId))
                .collect(Collectors.groupingBy(t -> t.teacher, LinkedHashMap::new, Collectors.toList()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (var entry : byTeacher.entrySet()) {
            AppUser teacher = entry.getKey();
            List<Map<String, Object>> classList = new ArrayList<>();
            for (Teaching t : entry.getValue()) {
                String plan = classToPlan.get(t.id);
                Map<String, Object> c = new HashMap<>();
                c.put("teachingId", t.id);
                c.put("classCode", t.classCode);
                c.put("subjectCode", t.subject.code);
                c.put("subjectName", t.subject.name);
                c.put("facultyId", t.subject.faculty.id);
                c.put("facultyName", t.subject.faculty.name);
                c.put("alreadyPlanned", plan != null);
                c.put("existingPlanCode", plan);
                classList.add(c);
            }
            Map<String, Object> group = new HashMap<>();
            group.put("teacherId", teacher.id);
            group.put("teacherAccount", teacher.account);
            group.put("teacherName", teacher.fullName);
            group.put("classes", classList);
            result.add(group);
        }
        return result;
    }

    /** Lấy danh sách buổi học THẬT (ca 1-6) để chọn ngày dự kiến (UC-10) */
    public List<ValidSessionDto> getValidSessionsForTeaching(Long teachingId) {
        List<CalendarSession> list = calendarRepo.findByTeachingIdOrderByDateAscShiftAsc(teachingId);
        return list.stream()
                .filter(s -> s.shift >= 1 && s.shift <= 6)
                .map(s -> new ValidSessionDto(
                        s.id, s.date, s.shift, s.room, s.sessionNo,
                        String.format("%s – Ca %d (%s, P.%s)", s.date, s.shift,
                                (s.sessionNo != null ? "Buổi " + s.sessionNo : ""), s.room)))
                .toList();
    }

    /** UC-11: Tìm người dự chéo toàn hệ thống (nếu rỗng thì trả về toàn bộ giảng viên) */
    public List<InspectorCandidateDto> searchInspectorsGlobal(String keyword, Long currentCampusId) {
        String kw = (keyword == null) ? "" : keyword.trim();
        List<AppUser> users = userRepo.searchInspectorsGlobal(kw);
        // Giảng viên cùng cơ sở xếp lên đầu, sau đó theo account
        return users.stream()
                .sorted(Comparator.comparing((AppUser u) -> !u.campus.id.equals(currentCampusId))
                        .thenComparing(u -> u.account != null ? u.account : ""))
                .map(u -> new InspectorCandidateDto(
                        u.id, u.account, u.fullName, u.staffCode,
                        u.faculty != null ? u.faculty.code : "",
                        u.faculty != null ? u.faculty.name : "N/A",
                        u.campus.code, u.campus.name,
                        u.campus.id.equals(currentCampusId)))
                .toList();
    }

    // --- LƯU / GỬI KẾ HOẠCH (UC-10, UC-12, UC-13) ---
    public record DetailSaveReq(
            Long teachingId, LocalDate plannedDate, Integer shift,
            Long inspector1Id, Long inspector2Id, Long inspector3Id, String reason) {}

    public record PlanSaveReq(
            Long facultyId, Long semesterId, int block, boolean unscheduled,
            String submitAction, // "DRAFT" hoặc "SUBMIT"
            List<DetailSaveReq> items) {}

    @Transactional
    public Inspection savePlan(Long planId, PlanSaveReq req, AppUser actor) {
        Long targetFacultyId = req.facultyId();
        if (targetFacultyId == null && req.items() != null && !req.items().isEmpty()) {
            Teaching firstTeaching = teachingRepo.findById(req.items().get(0).teachingId()).orElse(null);
            if (firstTeaching != null && firstTeaching.subject != null && firstTeaching.subject.faculty != null) {
                targetFacultyId = firstTeaching.subject.faculty.id;
            }
        }
        if (targetFacultyId == null) {
            targetFacultyId = actor.faculty != null ? actor.faculty.id : 1L;
        }
        Faculty faculty = facultyRepo.findById(targetFacultyId).orElseThrow();

        Long targetSemesterId = req.semesterId();
        if (targetSemesterId == null) {
            targetSemesterId = semesterRepo.findAllByOrderByOrdinalDesc().stream().map(s -> s.id).findFirst().orElse(1L);
        }
        Semester semester = semesterRepo.findById(targetSemesterId).orElseThrow();

        int targetBlock = req.block() > 0 ? req.block() : 1;
        if (req.items() != null && !req.items().isEmpty()) {
            Teaching firstTeaching = teachingRepo.findById(req.items().get(0).teachingId()).orElse(null);
            if (firstTeaching != null && firstTeaching.block > 0) {
                targetBlock = firstTeaching.block;
            }
        }

        Inspection plan;
        if (planId != null && planId > 0) {
            plan = inspectionRepo.findById(planId).orElseThrow();
            if ("APPROVED".equals(plan.status) || "CANCELLED".equals(plan.status)) {
                throw new IllegalArgumentException("Không thể chỉnh sửa kế hoạch đã duyệt hoặc đã từ chối");
            }
            // Xóa chi tiết cũ để ghi mới
            List<InspectionDetail> old = detailRepo.findByInspectionId(plan.id);
            detailRepo.deleteAll(old);
        } else {
            plan = new Inspection();
            plan.campus = actor.campus;
            plan.faculty = faculty;
            plan.semester = semester;
            plan.block = targetBlock;
            plan.creator = actor;
            plan.unscheduled = req.unscheduled;

            // Sinh mã kế hoạch: KH-DNA-CNTT-FA26-B1 (Kế hoạch đột xuất luôn có hậu tố -DX ở cuối)
            String base = String.format("KH-%s-%s-%s-B%d",
                    actor.campus.code.toUpperCase(), faculty.code.toUpperCase(),
                    semester.shortCode(), targetBlock);
            String code = req.unscheduled ? (base + "-DX") : base;
            int seq = 1;
            while (inspectionRepo.findByCode(code).isPresent()) {
                seq++;
                code = req.unscheduled ? (base + "-" + seq + "-DX") : (base + "-" + seq);
            }
            plan.code = code;
        }

        boolean submit = "SUBMIT".equalsIgnoreCase(req.submitAction);
        plan.status = submit ? "SUBMITTED" : "DRAFT";
        if (submit) plan.submittedAt = LocalDateTime.now();
        plan = inspectionRepo.save(plan);

        // Lưu chi tiết
        for (DetailSaveReq it : req.items) {
            Teaching t = teachingRepo.findById(it.teachingId).orElseThrow();
            InspectionDetail d = new InspectionDetail();
            d.inspection = plan;
            d.teaching = t;
            d.plannedDate = it.plannedDate;
            d.shift = it.shift;

            if (it.inspector1Id != null) d.inspector1 = userRepo.findById(it.inspector1Id).orElse(null);
            if (it.inspector2Id != null) d.inspector2 = userRepo.findById(it.inspector2Id).orElse(null);
            if (it.inspector3Id != null) d.inspector3 = userRepo.findById(it.inspector3Id).orElse(null);
            d.reason = it.reason;

            // Ràng buộc 1: Chặn GV tự dự giờ lớp của chính mình
            if ((d.inspector1 != null && d.inspector1.id.equals(t.teacher.id))
                    || (d.inspector2 != null && d.inspector2.id.equals(t.teacher.id))
                    || (d.inspector3 != null && d.inspector3.id.equals(t.teacher.id))) {
                throw new IllegalArgumentException("Giảng viên " + t.teacher.account + " đứng lớp " + t.classCode + ", không thể tự dự giờ lớp của chính mình");
            }

            // Ràng buộc 2: Chặn chọn trùng giảng viên giữa các vị trí GV1, GV2, GV3 trong cùng 1 lớp
            Set<Long> assignedIds = new HashSet<>();
            for (AppUser ins : Arrays.asList(d.inspector1, d.inspector2, d.inspector3)) {
                if (ins != null && !assignedIds.add(ins.id)) {
                    throw new IllegalArgumentException("Lớp " + t.classCode + " có giảng viên dự giờ (" + ins.account + ") bị phân công trùng lặp");
                }
            }

            // Ràng buộc khi Submit: GV1 và GV2 bắt buộc, ngày dự kiến bắt buộc, đột xuất bắt buộc có lý do
            if (submit) {
                if (d.inspector1 == null || d.inspector2 == null) {
                    throw new IllegalArgumentException("Lớp " + t.classCode + " bắt buộc phân công cả GV1 và GV2");
                }
                if (d.plannedDate == null) {
                    throw new IllegalArgumentException("Lớp " + t.classCode + " chưa chọn ngày dự kiến");
                }
                if (plan.unscheduled && (d.reason == null || d.reason.isBlank())) {
                    throw new IllegalArgumentException("Kế hoạch đột xuất: Lớp " + t.classCode + " bắt buộc nhập lý do");
                }
            }
            detailRepo.save(d);
        }

        // Ghi lịch sử
        InspectionHistory h = new InspectionHistory();
        h.inspection = plan;
        h.action = submit ? "SUBMITTED" : (planId == null ? "CREATED" : "SAVED");
        h.note = submit ? "Trình duyệt kế hoạch dự giờ" : "Lưu bản nháp";
        h.actor = actor;
        historyRepo.save(h);

        // Bắn thông báo cho TBĐT khi submit
        if (submit) {
            List<AppUser> tbdtList = userRepo.findByCampusIdAndActiveTrue(actor.campus.id).stream()
                    .filter(u -> "TBDT".equals(u.role)).toList();
            for (AppUser tbdt : tbdtList) {
                Notification n = new Notification();
                n.user = tbdt;
                n.message = "Bộ môn " + faculty.name + " vừa gửi kế hoạch dự giờ: " + plan.code;
                n.link = "/plans/" + plan.id;
                notiRepo.save(n);
            }
        }
        return plan;
    }

    // --- THAO TÁC DUYỆT CỦA TBĐT (UC-14) ---
    @Transactional
    public Inspection approvePlan(Long planId, AppUser actor) {
        if (!"TBDT".equals(actor.role) && !actor.headOffice) {
            throw new SecurityException("Chỉ Trưởng ban Đào tạo mới có quyền phê duyệt kế hoạch");
        }
        Inspection plan = inspectionRepo.findById(planId).orElseThrow();
        if (!"SUBMITTED".equals(plan.status)) {
            throw new IllegalArgumentException("Chỉ có thể phê duyệt kế hoạch đang ở trạng thái Chờ duyệt (SUBMITTED)");
        }
        plan.status = "APPROVED";
        plan.lastNote = null;
        inspectionRepo.save(plan);

        InspectionHistory h = new InspectionHistory();
        h.inspection = plan;
        h.action = "APPROVED";
        h.note = "TBĐT phê duyệt kế hoạch";
        h.actor = actor;
        historyRepo.save(h);

        Notification n = new Notification();
        n.user = plan.creator;
        n.message = "Kế hoạch dự giờ " + plan.code + " đã được TBĐT phê duyệt.";
        n.link = "/plans/" + plan.id;
        notiRepo.save(n);
        return plan;
    }

    @Transactional
    public Inspection requestEditPlan(Long planId, String note, AppUser actor) {
        if (!"TBDT".equals(actor.role) && !actor.headOffice) {
            throw new SecurityException("Chỉ Trưởng ban Đào tạo mới có quyền yêu cầu sửa");
        }
        if (note == null || note.isBlank()) {
            throw new IllegalArgumentException("Yêu cầu sửa bắt buộc phải nhập ghi chú phản hồi");
        }
        Inspection plan = inspectionRepo.findById(planId).orElseThrow();
        if (!"SUBMITTED".equals(plan.status)) {
            throw new IllegalArgumentException("Chỉ có thể yêu cầu sửa kế hoạch đang ở trạng thái Chờ duyệt (SUBMITTED)");
        }
        plan.status = "REJECTED_NEED_EDIT";
        plan.lastNote = note;
        inspectionRepo.save(plan);

        InspectionHistory h = new InspectionHistory();
        h.inspection = plan;
        h.action = "REJECTED_NEED_EDIT";
        h.note = note;
        h.actor = actor;
        historyRepo.save(h);

        Notification n = new Notification();
        n.user = plan.creator;
        n.message = "TBĐT yêu cầu sửa kế hoạch " + plan.code + ": " + note;
        n.link = "/plans/" + plan.id;
        notiRepo.save(n);
        return plan;
    }

    @Transactional
    public Inspection rejectPlan(Long planId, String reason, AppUser actor) {
        if (!"TBDT".equals(actor.role) && !actor.headOffice) {
            throw new SecurityException("Chỉ Trưởng ban Đào tạo mới có quyền từ chối");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Từ chối kế hoạch bắt buộc phải nhập lý do");
        }
        Inspection plan = inspectionRepo.findById(planId).orElseThrow();
        if (!"SUBMITTED".equals(plan.status)) {
            throw new IllegalArgumentException("Chỉ có thể từ chối kế hoạch đang ở trạng thái Chờ duyệt (SUBMITTED)");
        }
        plan.status = "CANCELLED";
        plan.lastNote = reason;
        inspectionRepo.save(plan);

        InspectionHistory h = new InspectionHistory();
        h.inspection = plan;
        h.action = "CANCELLED";
        h.note = reason;
        h.actor = actor;
        historyRepo.save(h);

        Notification n = new Notification();
        n.user = plan.creator;
        n.message = "Kế hoạch " + plan.code + " đã bị từ chối: " + reason;
        n.link = "/plans/" + plan.id;
        notiRepo.save(n);
        return plan;
    }

    /** Xóa kế hoạch: Kế hoạch APPROVED cần Super Admin (Head Office) đồng ý */
    @Transactional
    public void deletePlan(Long planId, AppUser actor) {
        Inspection plan = inspectionRepo.findById(planId).orElseThrow();
        if ("APPROVED".equals(plan.status) && !actor.headOffice) {
            throw new SecurityException("Kế hoạch đã duyệt chỉ được xóa khi có Super Admin (Head Office) đồng ý");
        }
        List<InspectionDetail> details = detailRepo.findByInspectionId(planId);
        detailRepo.deleteAll(details);
        List<InspectionHistory> histories = historyRepo.findByInspectionIdOrderByCreatedAtAsc(planId);
        historyRepo.deleteAll(histories);
        inspectionRepo.delete(plan);
    }
}
