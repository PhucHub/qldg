package com.fpoly.qldg.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Kế hoạch dự giờ (cơ sở + bộ môn + kỳ + block).
 * Trạng thái: DRAFT -> SUBMITTED -> APPROVED | REJECTED_NEED_EDIT (sửa rồi gửi lại) | CANCELLED.
 * Kế hoạch đột xuất có mã đuôi -DX, đi duyệt độc lập.
 */
@Entity
@Table(name = "inspections")
public class Inspection {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(unique = true, nullable = false, length = 60) public String code;
    @ManyToOne(optional = false) public Campus campus;
    @ManyToOne(optional = false) public Faculty faculty;
    @ManyToOne(optional = false) public Semester semester;
    public int block;
    @ManyToOne(optional = false) public AppUser creator;
    @Column(length = 25) public String status = "DRAFT";
    public boolean unscheduled;
    /** Ghi chú gần nhất của TBĐT (yêu cầu sửa / từ chối) */
    @Column(length = 1000) public String lastNote;
    public LocalDateTime createdAt = LocalDateTime.now();
    public LocalDateTime submittedAt;
}