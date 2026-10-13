package com.studyanchor.focus.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** FR-10 AC2: a "day" is a calendar day in Australia/Sydney. */
class StudyCalendarTest {

    @Test
    void FR10_dayIsSydneyCalendarDay() {
        // 13:30 UTC on 13 Oct = 00:30 AEDT (UTC+11) on 14 Oct
        assertThat(StudyCalendar.dayOf(Instant.parse("2026-10-13T13:30:00Z"))).isEqualTo(LocalDate.of(2026, 10, 14));
        // 12:59 UTC on 13 Oct = 23:59 AEDT on 13 Oct
        assertThat(StudyCalendar.dayOf(Instant.parse("2026-10-13T12:59:00Z"))).isEqualTo(LocalDate.of(2026, 10, 13));
        // Winter (AEST, UTC+10): 14:00 UTC on 1 Jul = 00:00 on 2 Jul
        assertThat(StudyCalendar.dayOf(Instant.parse("2026-07-01T14:00:00Z"))).isEqualTo(LocalDate.of(2026, 7, 2));
    }
}
