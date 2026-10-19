package com.fpoly.qldg.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = @Index(columnList = "user_id,is_read"))
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional = false) public AppUser user;
    @Column(length = 500) public String message;
    public String link;
    @Column(name = "is_read") public boolean read;
    public LocalDateTime createdAt = LocalDateTime.now();
}