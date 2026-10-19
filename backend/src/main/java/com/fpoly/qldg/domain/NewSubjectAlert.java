package com.fpoly.qldg.domain;

import jakarta.persistence.*;

/** Mã môn mới phát hiện khi import (đã tự tạo môn, chờ đặt tên). */
@Entity
@Table(name = "new_subject_alerts")
public class NewSubjectAlert {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional = false) public ImportJob job;
    @ManyToOne(optional = false) public Subject subject;
    /** UPDATE_VERSION (vd COM1081 của COM108) | NEW */
    @Column(length = 20) public String kind;
    public String baseCode;
    public int classCount;
}