package com.fpoly.qldg.controller;

import com.fpoly.qldg.domain.*;
import com.fpoly.qldg.repo.Repos.*;
import com.fpoly.qldg.service.SecurityScopeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    private final UserRepo userRepo;
    private final CampusRepo campusRepo;
    private final FacultyRepo facultyRepo;
    private final SecurityScopeService scopeService;

    @Value("${app.dev.fake-token:true}")
    private boolean devFakeTokenEnabled;

    public AuthController(
            UserRepo userRepo, CampusRepo campusRepo, FacultyRepo facultyRepo,
            SecurityScopeService scopeService) {
        this.userRepo = userRepo;
        this.campusRepo = campusRepo;
        this.facultyRepo = facultyRepo;
        this.scopeService = scopeService;
    }

    /** Lấy thông tin user hiện tại qua token */
    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        AppUser u = scopeService.resolveUser(authHeader);
        if (u == null) return ResponseEntity.status(401).body(Map.of("message", "Chưa đăng nhập"));
        List<Faculty> faculties = scopeService.myFaculties(u);

        Map<String, Object> res = new HashMap<>();
        res.put("id", u.id);
        res.put("account", u.account);
        res.put("fullName", u.fullName);
        res.put("email", u.email);
        res.put("role", u.role);
        res.put("staffCode", u.staffCode);
        res.put("headOffice", u.headOffice);
        res.put("campus", u.campus);
        res.put("faculty", u.faculty);
        res.put("faculties", faculties); // Phạm vi my_faculties
        return ResponseEntity.ok(res);
    }

    /** Danh sách tài khoản mẫu để chọn nhanh trên thanh Dev Fake Token */
    @GetMapping("/dev/users")
    public ResponseEntity<?> devUsers() {
        if (!devFakeTokenEnabled) return ResponseEntity.status(403).body("Dev fake token disabled");
        List<AppUser> list = userRepo.findAll();
        List<Map<String, Object>> res = list.stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.id);
            m.put("account", u.account);
            m.put("fullName", u.fullName);
            m.put("role", u.role);
            m.put("campusCode", u.campus.code);
            m.put("campusName", u.campus.name);
            m.put("facultyCode", u.faculty != null ? u.faculty.code : "");
            m.put("label", String.format("%s - %s (%s, %s)", u.account, u.fullName, u.role, u.campus.code.toUpperCase()));
            return m;
        }).toList();
        return ResponseEntity.ok(res);
    }

    /** Sinh dev token giả lập */
    @PostMapping("/dev/token")
    public ResponseEntity<?> createDevToken(@RequestBody Map<String, Long> body) {
        if (!devFakeTokenEnabled) return ResponseEntity.status(403).body("Dev fake token disabled");
        Long userId = body.get("userId");
        if (userId == null) return ResponseEntity.badRequest().body("Thiếu userId");
        String token = scopeService.generateDevToken(userId);
        return ResponseEntity.ok(Map.of("token", token));
    }

    @GetMapping("/campuses")
    public List<Campus> getCampuses() {
        return campusRepo.findAll();
    }
}
