package com.fpoly.qldg.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Một buổi học cụ thể của phân công (bảng calendars).
 * Danh tính ổn định = (phân công, số thứ tự buổi), KHÔNG phải ngày.
 * Ràng buộc duy nhất ở tầng DB chặn trùng lặp kể cả khi code có lỗi.
 */
@Entity
@Table(name = "calendars", uniqueConstraints = @UniqueConstraint(
        columnNames = {"teaching_id", "teacher_id", "date", "shift", "room"}))
public class CalendarSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @ManyToOne(optional = false) public Teaching teaching;
    @ManyToOne(optional = false) public AppUser teacher;
    /** Kiểu DATE, không có giờ - giờ học chính là ca */
    @Column(nullable = false) public LocalDate date;
    /** Ca 1-6 là ca học thật; ca 7+ là tự học online */
    public int shift;
    @Column(nullable = false, length = 30) public String room;
    public Integer sessionNo;
}