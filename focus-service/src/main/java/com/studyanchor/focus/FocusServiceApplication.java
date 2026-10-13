package com.studyanchor.focus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point of the Focus service (port 8083). See docs/03-bounded-contexts.md. */
@SpringBootApplication
public class FocusServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FocusServiceApplication.class, args);
    }
}
