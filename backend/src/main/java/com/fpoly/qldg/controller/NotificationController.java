package com.fpoly.qldg.controller;

import com.fpoly.qldg.domain.*;
import com.fpoly.qldg.repo.Repos.*;
import com.fpoly.qldg.service.SecurityScopeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {
    private final NotificationRepo notiRepo;
    private final SecurityScopeService scopeService;

    public NotificationController(NotificationRepo notiRepo, SecurityScopeService scopeService) {
        this.notiRepo = notiRepo;
        this.scopeService = scopeService;
    }

    @GetMapping
    public ResponseEntity<List<Notification>> getMyNotifications(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        AppUser u = scopeService.resolveUser(authHeader);
        if (u == null) return ResponseEntity.ok(List.of());
        return ResponseEntity.ok(notiRepo.findByUserIdOrderByCreatedAtDesc(u.id));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        notiRepo.findById(id).ifPresent(n -> {
            n.read = true;
            notiRepo.save(n);
        });
        return ResponseEntity.ok().build();
    }
}
