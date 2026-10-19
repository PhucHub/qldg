package com.fpoly.qldg.domain;

import jakarta.persistence.*;

/**
 * Cơ sở nào có bộ môn nào (bật/tắt) và gộp nhóm bằng group_key.
 * Các bộ môn cùng group_key tại CÙNG một cơ sở = một nhóm quản lý, do 1 CNBM phụ trách.
 * (Trang cấu hình bảng này thuộc nhóm khác - ở đây chỉ đọc để tính phạm vi.)
 */
@Entity
@Table(name = "campus_faculty", uniqueConstraints = @UniqueConstraint(columnNames = {"campus_id", "faculty_id"}))
public class CampusFaculty {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional = false) public Campus campus;
    @ManyToOne(optional = false) public Faculty faculty;
    @Column(name = "is_active") public boolean active;
    @Column(name = "group_key", length = 30) public String groupKey;
    @ManyToOne public AppUser cnbm;
    @ManyToOne public AppUser tm;
}