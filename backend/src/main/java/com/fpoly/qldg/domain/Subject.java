package com.fpoly.qldg.domain;

import jakarta.persistence.*;

/** Môn học. Mỗi môn thuộc đúng 1 bộ môn - quyết định lớp vào kế hoạch của bộ môn nào. */
@Entity
@Table(name = "subjects")
public class Subject {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(unique = true, nullable = false, length = 20) public String code;
    @Column(nullable = false) public String name;
    @ManyToOne(optional = false) public Faculty faculty;
    /** true = do import tự tạo, chờ TC&QLĐT đặt tên chính thức */
    public boolean autoCreated;
}