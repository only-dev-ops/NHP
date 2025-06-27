package com.nhp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NHPApplication {
    public static void main(String[] args) {
        SpringApplication.run(NHPApplication.class, args);
    }
}