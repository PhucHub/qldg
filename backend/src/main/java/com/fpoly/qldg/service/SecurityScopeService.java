package com.fpoly.qldg.service;

import com.fpoly.qldg.domain.*;
import com.fpoly.qldg.repo.Repos.*;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Tính phạm vi bộ môn theo vai trò (my_faculties) và giải mã Bearer Token giả lập.
 */
@Service
public class SecurityScopeService {
    private final UserRepo userRepo;
    private final CampusFacultyRepo campusFacultyRepo;
    private final FacultyRepo facultyRepo;

    public SecurityScopeService(UserRepo userRepo, CampusFacultyRepo campusFacultyRepo, FacultyRepo facultyRepo) {
        this.userRepo = userRepo;
        this.campusFacultyRepo = campusFacultyRepo;
        this.facultyRepo = facultyRepo;
    }

    /** Token đơn giản cho Dev: "userId:campusId:role" */
    public String generateDevToken(Long userId) {
        AppUser u = userRepo.findById(userId).orElseThrow();
        return u.id + ":" + u.campus.id + ":" + u.role;
    }

    public AppUser resolveUser(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Mặc định cho dev: lấy user đầu tiên (TBĐT Đà Nẵng)
            return userRepo.findAll().stream().findFirst().orElse(null);
        }
        String token = authHeader.substring(7).trim();
        try {
            String[] p = token.split(":");
            return userRepo.findById(Long.parseLong(p[0])).orElse(null);
        } catch (Exception e) {
            return userRepo.findAll().stream().findFirst().orElse(null);
        }
    }

    /**
     * Quy tắc my_faculties() theo tài liệu:
     * - TBĐT, TC&QLĐT, Super Admin: MỌI bộ môn is_active = true của cơ sở
     * - CNBM: CẢ NHÓM bộ môn có cùng group_key với bộ môn của mình
     * - TM: ĐÚNG bộ môn mình phụ trách
     * - GV: Bộ môn của mình
     */
    public List<Faculty> myFaculties(AppUser user) {
        if (user == null) return List.of();
        Long campusId = user.campus.id;
        List<CampusFaculty> activeList = campusFacultyRepo.findByCampusIdAndActiveTrue(campusId);

        if (user.headOffice || "TBDT".equals(user.role) || "TCQLDT".equals(user.role)) {
            return activeList.stream().map(cf -> cf.faculty).toList();
        }

        if ("CNBM".equals(user.role)) {
            // Tìm group_key bộ môn của CNBM
            Optional<CampusFaculty> own = activeList.stream()
                    .filter(cf -> (cf.cnbm != null && cf.cnbm.id.equals(user.id)) ||
                                  (user.faculty != null && cf.faculty.id.equals(user.faculty.id)))
                    .findFirst();
            if (own.isPresent() && own.get().groupKey != null && !own.get().groupKey.isBlank()) {
                String gk = own.get().groupKey;
                return activeList.stream()
                        .filter(cf -> gk.equalsIgnoreCase(cf.groupKey))
                        .map(cf -> cf.faculty).toList();
            }
            return own.map(cf -> List.of(cf.faculty)).orElse(List.of());
        }

        if ("TM".equals(user.role)) {
            return activeList.stream()
                    .filter(cf -> (cf.tm != null && cf.tm.id.equals(user.id)) ||
                                  (user.faculty != null && cf.faculty.id.equals(user.faculty.id)))
                    .map(cf -> cf.faculty).toList();
        }

        if (user.faculty != null) {
            return activeList.stream()
                    .filter(cf -> cf.faculty.id.equals(user.faculty.id))
                    .map(cf -> cf.faculty).toList();
        }
        return List.of();
    }
}
