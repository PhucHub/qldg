package com.fpoly.qldg.domain;

import jakarta.persistence.*;

/** Cơ sở đào tạo (14 cơ sở). */
@Entity
@Table(name = "campuses")
public class Campus {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(unique = true, nullable = false, length = 20) public String code;
    @Column(nullable = false) public String name;
    public String address;
}