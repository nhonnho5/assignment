package com.studyanchor.planner.domain;

import jakarta.persistence.Embeddable;

import java.util.Locale;

/** Implements FR-01 AC6. Value object: a course code, 1–20 characters, upper-cased so csci318 = CSCI318. */
@Embeddable
public record CourseCode(String value) {

    public CourseCode {
        value = Text.of(value, "Course code", 20).toUpperCase(Locale.ROOT);
    }
}
