package com.studyanchor.focus.domain;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Domain rules for FR-05 (start and end a focus session). Plain Java, no Spring. */
class FocusSessionTest {

    private static final Instant START = Instant.parse("2026-10-14T03:00:00Z");

    private FocusSession startSession(Integer plannedMinutes) {
        return FocusSession.start("alice", null, null, plannedMinutes, List.of(), START);
    }

    @Test
    void FR05_plannedMinutesMustBeBetween10And180() {
        assertThatThrownBy(() -> startSession(9)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> startSession(181)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> startSession(null)).isInstanceOf(DomainException.class);

        assertThat(startSession(10).plannedMinutes().value()).isEqualTo(10);
        assertThat(startSession(180).plannedMinutes().value()).isEqualTo(180);
    }

    @Test
    void FR05_startRegistersFocusSessionStarted() {
        UUID topicId = UUID.randomUUID();
        FocusSession session = FocusSession.start("alice", null, topicId, 25, List.of("Canvas.uow.edu.au "), START);

        assertThat(session.isActive()).isTrue();
        assertThat(session.allowedSites()).containsExactly("canvas.uow.edu.au");

        List<DomainEvent> events = session.pullDomainEvents();
        assertThat(events).singleElement().isInstanceOfSatisfying(FocusSessionStarted.class, e -> {
            assertThat(e.eventType()).isEqualTo("FocusSessionStarted");
            assertThat(e.userId()).isEqualTo("alice");
            assertThat(e.sessionId()).isEqualTo(session.id());
            assertThat(e.topicId()).isEqualTo(topicId);
            assertThat(e.timeBlockId()).isNull();
            assertThat(e.plannedMinutes()).isEqualTo(25);
            assertThat(e.startedAt()).isEqualTo(START);
            assertThat(e.occurredAt()).isEqualTo(START);
        });
        assertThat(session.pullDomainEvents()).as("events are handed out only once").isEmpty();
    }

    @Test
    void FR05_endRecordsExactDurationInMinutesAndSeconds() {
        FocusSession session = startSession(25);

        session.end(START.plus(Duration.ofMinutes(25).plusSeconds(59).plusMillis(700)));

        assertThat(session.isActive()).isFalse();
        assertThat(session.actualDuration()).isEqualTo(Duration.ofMinutes(25).plusSeconds(59));
        assertThat(session.actualDuration().toSeconds()).isEqualTo(1559);
    }

    @Test
    void FR05_endRegistersFocusSessionEnded() {
        FocusSession session = startSession(25);
        session.pullDomainEvents();
        Instant end = START.plusSeconds(1559);

        session.end(end);

        assertThat(session.pullDomainEvents()).singleElement().isInstanceOfSatisfying(FocusSessionEnded.class, e -> {
            assertThat(e.eventType()).isEqualTo("FocusSessionEnded");
            assertThat(e.userId()).isEqualTo("alice");
            assertThat(e.sessionId()).isEqualTo(session.id());
            assertThat(e.actualSeconds()).isEqualTo(1559);
            assertThat(e.endedAt()).isEqualTo(end);
        });
    }

    @Test
    void FR10_endedEventCarriesStartedAt() {
        FocusSession session = startSession(25);
        session.pullDomainEvents();
        session.end(START.plusSeconds(1500));

        FocusSessionEnded ended = (FocusSessionEnded) session.pullDomainEvents().get(0);
        assertThat(ended.startedAt()).isEqualTo(START);
    }

    @Test
    void FR05_cannotEndTwice() {
        FocusSession session = startSession(25);
        session.end(START.plusSeconds(60));

        assertThatThrownBy(() -> session.end(START.plusSeconds(120))).isInstanceOf(ConflictException.class);
    }
}
