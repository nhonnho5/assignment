package com.studyanchor.focus.domain;

import java.time.LocalDate;

/** Implements FR-10. Value object: total focus time of one student on one Sydney day. */
public record DailyFocus(LocalDate day, long seconds) {

    /** A day counts toward the streak with at least 25 focus minutes (glossary: Streak). */
    public static final long STREAK_THRESHOLD_SECONDS = 25 * 60;

    public DailyFocus {
        if (day == null) {
            throw new DomainException("day is required");
        }
        if (seconds < 0) {
            throw new DomainException("focus seconds cannot be negative");
        }
    }

    /** Whole minutes, rounded down. */
    public long minutes() {
        return seconds / 60;
    }

    public boolean meetsStreakThreshold() {
        return seconds >= STREAK_THRESHOLD_SECONDS;
    }
}
