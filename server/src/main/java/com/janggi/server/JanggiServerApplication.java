package com.janggi.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JanggiServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(JanggiServerApplication.class, args);
    }
}
