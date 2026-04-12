package com.bizcord.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.bizcord.backend", "com.bizcord.rtc"})
@EnableJpaAuditing
@EnableScheduling
@EntityScan(basePackages = {"com.bizcord.backend.entity", "com.bizcord.rtc.model"})
@EnableJpaRepositories(basePackages = {"com.bizcord.backend.repository", "com.bizcord.rtc.repository"})
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

}
