package com.fpoly.qldg.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Dòng thời gian lịch sử của kế hoạch (tạo, gửi, yêu cầu sửa, duyệt...). */
@Entity
@Table(name = "inspection_histories")
public class InspectionHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional = false) public Inspection inspection;
    /** CREATED | SAVED | SUBMITTED | APPROVED | REJECTED_NEED_EDIT | CANCELLED */
    @Column(length = 25) public String action;
    @Column(length = 1000) public String note;
    @ManyToOne(optional = false) public AppUser actor;
    public LocalDateTime createdAt = LocalDateTime.now();
}