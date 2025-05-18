package com.example.edutrack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class EduTrackApplication {

    public static void main(String[] args) {
        SpringApplication.run(EduTrackApplication.class, args);
    }

}
