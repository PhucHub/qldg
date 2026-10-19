package com.fpoly.qldg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

// considerNestedRepositories: toàn bộ repository khai báo gọn trong một file Repos.java
@SpringBootApplication
@EnableScheduling
@EnableJpaRepositories(considerNestedRepositories = true)
public class QldgApplication {
    public static void main(String[] args) {
        SpringApplication.run(QldgApplication.class, args);
    }
}
