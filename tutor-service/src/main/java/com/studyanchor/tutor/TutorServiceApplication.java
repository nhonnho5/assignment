package com.studyanchor.tutor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point of the Tutor service (port 8082). See docs/03-bounded-contexts.md. */
@SpringBootApplication
public class TutorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TutorServiceApplication.class, args);
    }
}
