package com.fpoly.qldg.domain;

import jakarta.persistence.*;

/**
 * Tài khoản. Định danh = (email + cơ sở): một người dạy 2 cơ sở có 2 dòng - đúng thiết kế.
 * Quyền Head Office gắn theo email (cờ headOffice bật trên mọi dòng cùng email).
 */
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = {"email", "campus_id"}))
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(nullable = false, length = 50) public String account;
    @Column(nullable = false) public String email;
    public String fullName;
    @Column(length = 20) public String staffCode;
    /** HEAD_OFFICE | TBDT | TCQLDT | CNBM | TM | GV */
    @Column(nullable = false, length = 20) public String role;
    @ManyToOne(optional = false) public Campus campus;
    @ManyToOne public Faculty faculty;
    public boolean active = true;
    public boolean headOffice;
}