package com.fpoly.qldg.domain;

import jakarta.persistence.*;

/** Kết quả đối soát của từng dòng file import. */
@Entity
@Table(name = "import_rows", indexes = @Index(columnList = "job_id,result"))
public class ImportRow {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional = false) public ImportJob job;
    public int rowNo;
    public String classCode, subjectCode, teacherAccount;
    public Integer sessionNo;
    /** INSERTED | SKIPPED | UPDATED_ROOM | UPDATED_TIME | SWAPPED | ERROR */
    @Column(length = 20) public String result;
    public String oldValue, newValue;
    @Column(length = 500) public String message;
}