package com.fpoly.qldg.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

/** Kỳ học + mốc 2 block. Block 1 bắt đầu = đầu kỳ; Block 2 kết thúc = cuối kỳ. */
@Entity
@Table(name = "semesters")
public class Semester {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    /** SPRING | SUMMER | FALL */
    @Column(nullable = false, length = 10) public String term;
    public int year;
    /**
     * Số thứ tự kỳ = year*3 + (Spring 0, Summer 1, Fall 2).
     * Khoảng cách "nghỉ dạy N kỳ" đếm theo số này, không theo tháng.
     */
    public int ordinal;
    public LocalDate startDate;
    public LocalDate block1End;
    public LocalDate block2Start;
    public LocalDate endDate;

    public String getName() {
        return term.charAt(0) + term.substring(1).toLowerCase() + " " + year;
    }

    public String name() {
        return getName();
    }

    public String getShortCode() { // FA26, SP26, SU26
        return term.substring(0, 2) + String.valueOf(year).substring(2);
    }

    public String shortCode() {
        return getShortCode();
    }

    public LocalDate blockStart(int block) { return block == 1 ? startDate : block2Start; }

    public LocalDate blockEnd(int block) { return block == 1 ? block1End : endDate; }

    public static int ordinalOf(String term, int year) {
        return year * 3 + switch (term) { case "SPRING" -> 0; case "SUMMER" -> 1; default -> 2; };
    }
}