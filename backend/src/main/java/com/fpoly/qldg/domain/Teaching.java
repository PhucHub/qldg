package com.fpoly.qldg.domain;

import jakarta.persistence.*;

/**
 * Phân công giảng dạy (bảng subject_user, trong nghiệp vụ gọi là Teaching).
 * 1 dòng = 1 lớp + 1 môn + 1 block + 1 kỳ + 1 giảng viên.
 */
@Entity
@Table(name = "subject_user", uniqueConstraints = @UniqueConstraint(
        columnNames = {"class_code", "subject_id", "semester_id", "block", "teacher_id"}))
public class Teaching {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(name = "class_code", nullable = false, length = 30) public String classCode;
    @ManyToOne(optional = false) public Subject subject;
    @ManyToOne(optional = false) public Semester semester;
    public int block;
    @ManyToOne(optional = false) public AppUser teacher;
    @ManyToOne(optional = false) public Campus campus;
}