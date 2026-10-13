package com.studyanchor.focus.domain;

import jakarta.persistence.Embeddable;

/** Implements FR-05 AC1. Value object: how long the student intends to focus, 10–180 minutes. */
@Embeddable
public record PlannedMinutes(int value) {

    public static final int MIN = 10;
    public static final int MAX = 180;

    public PlannedMinutes {
        if (value < MIN || value > MAX) {
            throw new DomainException("plannedMinutes must be between " + MIN + " and " + MAX + ", was " + value);
        }
    }

    /** Accepts a possibly missing value from a request and rejects it if absent. */
    public static PlannedMinutes of(Integer value) {
        if (value == null) {
            throw new DomainException("plannedMinutes is required");
        }
        return new PlannedMinutes(value);
    }
}
