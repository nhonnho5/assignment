package com.studyanchor.social;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point of the Social service (port 8084). See docs/03-bounded-contexts.md. */
@SpringBootApplication
public class SocialServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SocialServiceApplication.class, args);
    }
}
