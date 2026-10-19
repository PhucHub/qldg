package com.fpoly.qldg.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Một lần import lịch dạy = một task trong hàng đợi (queue_tasks). */
@Entity
@Table(name = "queue_tasks")
public class ImportJob {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    public String fileName;
    public String storedPath;
    public long fileSize;
    @ManyToOne(optional = false) public Campus campus;
    @ManyToOne(optional = false) public AppUser uploader;
    /** PENDING | PROCESSING | DONE | DONE_WITH_ERRORS | FAILED */
    @Column(length = 20) public String status = "PENDING";
    public int totalRows, inserted, updated, skipped, errors, progress;
    @Column(length = 500) public String message;
    public LocalDateTime createdAt = LocalDateTime.now();
    public LocalDateTime finishedAt;
}