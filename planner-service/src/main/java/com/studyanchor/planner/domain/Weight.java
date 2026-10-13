package com.studyanchor.planner.domain;

import jakarta.persistence.Embeddable;

/** Implements FR-01 AC2, AC7. Value object: how much a deadline counts toward the course, 1–100 %. */
@Embeddable
public record Weight(int value) {

    public Weight {
        if (value < 1 || value > 100) {
            throw new DomainException("weight must be a whole number from 1 to 100, was " + value);
        }
    }
}
