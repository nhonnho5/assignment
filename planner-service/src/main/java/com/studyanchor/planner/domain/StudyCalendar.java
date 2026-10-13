package com.studyanchor.planner.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Implements FR-01 AC8, FR-11 AC3. The one place that says students' wall-clock times are Sydney time. */
public final class StudyCalendar {

    public static final ZoneId ZONE = ZoneId.of("Australia/Sydney");

    private StudyCalendar() {
    }

    /** A Sydney date-time as typed by the student, as the UTC instant it is stored as. */
    public static Instant toInstant(LocalDateTime sydneyTime) {
        return sydneyTime.atZone(ZONE).toInstant();
    }

    /** A stored instant as the Sydney date-time shown back to the student. */
    public static LocalDateTime toSydney(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZONE);
    }

    /** The Sydney calendar day that contains this instant. */
    public static LocalDate dayOf(Instant instant) {
        return LocalDate.ofInstant(instant, ZONE);
    }
}
