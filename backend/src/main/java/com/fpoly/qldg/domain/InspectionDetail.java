package com.fpoly.qldg.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

/** Một buổi dự giờ: lớp được dự + ngày/ca dự kiến + GV1, GV2 (bắt buộc), GV3 (tùy chọn). */
@Entity
@Table(name = "inspection_details")
public class InspectionDetail {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional = false) public Inspection inspection;
    @ManyToOne(optional = false) public Teaching teaching;
    public LocalDate plannedDate;
    public Integer shift;
    @ManyToOne public AppUser inspector1;
    @ManyToOne public AppUser inspector2;
    @ManyToOne public AppUser inspector3;
    /** Lý do (bắt buộc với kế hoạch đột xuất) */
    @Column(length = 500) public String reason;
}