package com.fpoly.qldg.domain;

import jakarta.persistence.*;

/** Bộ môn - bảng TOÀN CỤC, dùng chung cho mọi cơ sở (không có cột cơ sở). */
@Entity
@Table(name = "faculties")
public class Faculty {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(unique = true, nullable = false, length = 20) public String code;
    @Column(nullable = false) public String name;
}