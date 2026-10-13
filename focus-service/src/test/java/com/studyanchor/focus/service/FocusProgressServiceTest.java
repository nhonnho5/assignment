package com.studyanchor.focus.service;

import com.studyanchor.focus.domain.FocusProgress;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Application-service rules for FR-10, using a fake history (no Kafka). */
class FocusProgressServiceTest {

    @Test
    void FR10_progressUsesTodayFromClock() {
        // 14:00 UTC on 13 Oct is already 14 Oct in Sydney (AEDT, UTC+11)
        Clock clock = Clock.fixed(Instant.parse("2026-10-13T14:00:00Z"), ZoneOffset.UTC);
        LocalDate sydneyToday = LocalDate.of(2026, 10, 14);
        LocalDate[] askedUntil = new LocalDate[1];

        FocusProgressService service = new FocusProgressService((userId, until) -> {
            askedUntil[0] = until;
            return Map.of(sydneyToday, 600L);
        }, clock);

        FocusProgress progress = service.progressOf("alice");

        assertThat(askedUntil[0]).isEqualTo(sydneyToday);
        assertThat(progress.today().minutes()).isEqualTo(10);
    }
}
