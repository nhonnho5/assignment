package com.studyanchor.planner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point of the Planner service (port 8081). See docs/03-bounded-contexts.md. */
@SpringBootApplication
public class PlannerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlannerServiceApplication.class, args);
    }
}
