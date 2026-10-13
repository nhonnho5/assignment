package com.studyanchor.focus.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** Implements FR-10 AC2. The one place that says which time zone a "day" is in (docs/09-conventions.md). */
public final class StudyCalendar {

    public static final ZoneId ZONE = ZoneId.of("Australia/Sydney");

    private StudyCalendar() {
    }

    /** The Sydney calendar day that contains this instant. */
    public static LocalDate dayOf(Instant instant) {
        return instant.atZone(ZONE).toLocalDate();
    }
}
