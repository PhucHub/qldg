package com.fpoly.qldg.controller;

import com.fpoly.qldg.domain.*;
import com.fpoly.qldg.repo.Repos.*;
import com.fpoly.qldg.service.InspectionPlanService;
import com.fpoly.qldg.service.InspectionPlanService.*;
import com.fpoly.qldg.service.SecurityScopeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/plans")
@CrossOrigin(origins = "*")
public class PlanController {
    private final InspectionPlanService planService;
    private final SecurityScopeService scopeService;
    private final InspectionRepo inspectionRepo;
    private final InspectionDetailRepo detailRepo;
    private final InspectionHistoryRepo historyRepo;
    private final SemesterRepo semesterRepo;
    private final FacultyRepo facultyRepo;
    private final NotificationRepo notiRepo;

    public PlanController(
            InspectionPlanService planService, SecurityScopeService scopeService,
            InspectionRepo inspectionRepo, InspectionDetailRepo detailRepo,
            InspectionHistoryRepo historyRepo, SemesterRepo semesterRepo,
            FacultyRepo facultyRepo, NotificationRepo notiRepo) {
        this.planService = planService;
        this.scopeService = scopeService;
        this.inspectionRepo = inspectionRepo;
        this.detailRepo = detailRepo;
        this.historyRepo = historyRepo;
        this.semesterRepo = semesterRepo;
        this.facultyRepo = facultyRepo;
        this.notiRepo = notiRepo;
    }

    /** Danh sách các kỳ học */
    @GetMapping("/semesters")
    public List<Semester> getSemesters() {
        return semesterRepo.findAllByOrderByOrdinalDesc();
    }

    /** UC-09 & UC-10: Phân loại lớp và widget chỉ tiêu 15% */
    @GetMapping("/classify")
    public ResponseEntity<?> classify(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false, defaultValue = "0") int block,
            @RequestParam(required = false) Long facultyId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        AppUser u = scopeService.resolveUser(authHeader);
        if (u == null) return ResponseEntity.status(401).body("Chưa xác thực");
        List<Faculty> myF = scopeService.myFaculties(u);
        List<Long> scopeIds = myF.stream().map(f -> f.id).toList();

        ClassificationResultDto res = planService.classifyClasses(
                u.campus.id, semesterId, block, facultyId, scopeIds);
        return ResponseEntity.ok(res);
    }

    /** UC-12: Danh sách lớp cho kế hoạch đột xuất */
    @GetMapping("/unscheduled-candidates")
    public ResponseEntity<?> getUnscheduledCandidates(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false, defaultValue = "0") int block,
            @RequestParam(required = false) Long facultyId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        AppUser u = scopeService.resolveUser(authHeader);
        if (u == null) return ResponseEntity.status(401).body("Chưa xác thực");
        List<Faculty> myF = scopeService.myFaculties(u);
        List<Long> scopeIds = myF.stream().map(f -> f.id).toList();
        var list = planService.getUnscheduledCandidates(u.campus.id, semesterId, block, facultyId, scopeIds);
        return ResponseEntity.ok(list);
    }

    /** Lấy danh sách buổi học THẬT (ca 1-6) để chọn ngày dự kiến */
    @GetMapping("/teaching-sessions/{teachingId}")
    public ResponseEntity<?> getValidSessions(@PathVariable Long teachingId) {
        return ResponseEntity.ok(planService.getValidSessionsForTeaching(teachingId));
    }

    /** Giảng viên trong bộ môn hoặc cơ sở */
    @GetMapping("/inspectors/in-faculty")
    public ResponseEntity<?> getInspectorsInFaculty(
            @RequestParam(required = false) Long facultyId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        AppUser u = scopeService.resolveUser(authHeader);
        if (u == null) return ResponseEntity.status(401).body("Chưa xác thực");
        var stream = planService.searchInspectorsGlobal("", u.campus.id).stream()
                .filter(c -> c.sameCampus());
        if (facultyId != null && facultyId > 0) {
            String fCode = facultyRepo.findById(facultyId).map(f -> f.code).orElse("");
            stream = stream.filter(c -> fCode.equalsIgnoreCase(c.facultyCode()));
        }
        return ResponseEntity.ok(stream.toList());
    }

    /** UC-11: Tìm người dự chéo toàn hệ thống (khi keyword rỗng sẽ trả về toàn bộ giảng viên) */
    @GetMapping("/inspectors/search")
    public ResponseEntity<?> searchInspectors(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        AppUser u = scopeService.resolveUser(authHeader);
        Long campusId = u != null ? u.campus.id : 1L;
        return ResponseEntity.ok(planService.searchInspectorsGlobal(keyword, campusId));
    }

    /** UC-13: Danh sách kế hoạch dự giờ gom theo bộ môn */
    @GetMapping
    public ResponseEntity<?> listPlans(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false, defaultValue = "0") int block,
            @RequestParam(required = false) Long facultyId,
            @RequestParam(required = false) String status,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        AppUser u = scopeService.resolveUser(authHeader);
        if (u == null) return ResponseEntity.status(401).body("Chưa xác thực");
        List<Long> scopeIds;
        if (facultyId != null && facultyId > 0) {
            scopeIds = List.of(facultyId);
        } else {
            scopeIds = facultyRepo.findAll().stream().map(f -> f.id).toList();
        }

        List<Inspection> list = inspectionRepo.findList(u.campus.id, semesterId, block, scopeIds);
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            list = list.stream().filter(p -> status.equalsIgnoreCase(p.status)).toList();
        }

        // Gom theo bộ môn
        Map<String, List<Map<String, Object>>> grouped = new LinkedHashMap<>();
        for (Inspection p : list) {
            String facName = p.faculty.name;
            grouped.computeIfAbsent(facName, k -> new ArrayList<>());

            List<InspectionDetail> details = detailRepo.findByInspectionId(p.id);
            Map<String, Object> m = new HashMap<>();
            m.put("id", p.id);
            m.put("code", p.code);
            m.put("facultyId", p.faculty.id);
            m.put("facultyName", p.faculty.name);
            m.put("creatorAccount", p.creator.account);
            m.put("creatorName", p.creator.fullName);
            m.put("classCount", details.size());
            m.put("status", p.status);
            m.put("unscheduled", p.unscheduled);
            m.put("createdAt", p.createdAt);
            m.put("submittedAt", p.submittedAt);
            m.put("lastNote", p.lastNote);
            grouped.get(facName).add(m);
        }
        return ResponseEntity.ok(grouped);
    }

    /** Chi tiết một bản kế hoạch (UC-13) */
    @GetMapping("/{id}")
    public ResponseEntity<?> getPlanDetail(@PathVariable Long id) {
        Inspection p = inspectionRepo.findById(id).orElse(null);
        if (p == null) return ResponseEntity.notFound().build();

        List<InspectionDetail> details = detailRepo.findByInspectionId(id);
        List<InspectionHistory> histories = historyRepo.findByInspectionIdOrderByCreatedAtAsc(id);

        List<Map<String, Object>> items = details.stream().map(d -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", d.id);
            m.put("teachingId", d.teaching.id);
            m.put("classCode", d.teaching.classCode);
            m.put("subjectCode", d.teaching.subject.code);
            m.put("subjectName", d.teaching.subject.name);
            m.put("teacherAccount", d.teaching.teacher.account);
            m.put("teacherName", d.teaching.teacher.fullName);
            m.put("plannedDate", d.plannedDate);
            m.put("shift", d.shift);
            m.put("reason", d.reason);

            m.put("inspector1", d.inspector1 != null ? mapInspector(d.inspector1) : null);
            m.put("inspector2", d.inspector2 != null ? mapInspector(d.inspector2) : null);
            m.put("inspector3", d.inspector3 != null ? mapInspector(d.inspector3) : null);
            return m;
        }).toList();

        Map<String, Object> res = new HashMap<>();
        res.put("id", p.id);
        res.put("code", p.code);
        res.put("facultyId", p.faculty.id);
        res.put("facultyName", p.faculty.name);
        res.put("semesterId", p.semester.id);
        res.put("semesterName", p.semester.name());
        res.put("block", p.block);
        res.put("creatorAccount", p.creator.account);
        res.put("creatorName", p.creator.fullName);
        res.put("status", p.status);
        res.put("unscheduled", p.unscheduled);
        res.put("lastNote", p.lastNote);
        res.put("createdAt", p.createdAt);
        res.put("submittedAt", p.submittedAt);
        res.put("items", items);
        res.put("histories", histories);
        return ResponseEntity.ok(res);
    }

    private Map<String, Object> mapInspector(AppUser u) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.id);
        m.put("account", u.account);
        m.put("fullName", u.fullName);
        m.put("facultyName", u.faculty != null ? u.faculty.name : "N/A");
        m.put("campusName", u.campus.name);
        return m;
    }

    /** Tạo / Sửa kế hoạch (UC-10, UC-12) */
    @PostMapping
    public ResponseEntity<?> savePlan(
            @RequestBody PlanSaveReq req,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            AppUser u = scopeService.resolveUser(authHeader);
            if (u == null) return ResponseEntity.status(401).body("Chưa xác thực");
            Inspection p = planService.savePlan(null, req, u);
            return ResponseEntity.ok(Map.of("id", p.id, "code", p.code, "status", p.status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePlan(
            @PathVariable Long id,
            @RequestBody PlanSaveReq req,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            AppUser u = scopeService.resolveUser(authHeader);
            if (u == null) return ResponseEntity.status(401).body("Chưa xác thực");
            Inspection p = planService.savePlan(id, req, u);
            return ResponseEntity.ok(Map.of("id", p.id, "code", p.code, "status", p.status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** TBĐT phê duyệt (UC-14) */
    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        AppUser u = scopeService.resolveUser(authHeader);
        if (u == null) return ResponseEntity.status(401).body("Chưa xác thực");
        try {
            Inspection p = planService.approvePlan(id, u);
            return ResponseEntity.ok(Map.of("id", p.id, "status", p.status));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("message", e.getMessage()));
        }
    }

    /** TBĐT yêu cầu sửa (UC-14) */
    @PostMapping("/{id}/request-edit")
    public ResponseEntity<?> requestEdit(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        AppUser u = scopeService.resolveUser(authHeader);
        if (u == null) return ResponseEntity.status(401).body("Chưa xác thực");
        try {
            Inspection p = planService.requestEditPlan(id, body.get("note"), u);
            return ResponseEntity.ok(Map.of("id", p.id, "status", p.status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("message", e.getMessage()));
        }
    }

    /** TBĐT từ chối (UC-14) */
    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        AppUser u = scopeService.resolveUser(authHeader);
        if (u == null) return ResponseEntity.status(401).body("Chưa xác thực");
        try {
            Inspection p = planService.rejectPlan(id, body.get("reason"), u);
            return ResponseEntity.ok(Map.of("id", p.id, "status", p.status));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("message", e.getMessage()));
        }
    }

    /** Xóa kế hoạch (UC-13, UC-14) */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePlan(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        AppUser u = scopeService.resolveUser(authHeader);
        if (u == null) return ResponseEntity.status(401).body("Chưa xác thực");
        try {
            planService.deletePlan(id, u);
            return ResponseEntity.ok(Map.of("message", "Đã xóa kế hoạch thành công"));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("message", e.getMessage()));
        }
    }

    /** Danh sách thông báo cho chuông ở header */
    @GetMapping("/notifications")
    public ResponseEntity<?> getNotifications(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        AppUser u = scopeService.resolveUser(authHeader);
        if (u == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(notiRepo.findByUserIdOrderByCreatedAtDesc(u.id));
    }
}
