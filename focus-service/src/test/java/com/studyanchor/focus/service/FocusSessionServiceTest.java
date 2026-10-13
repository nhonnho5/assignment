package com.studyanchor.focus.service;

import com.studyanchor.focus.domain.ConflictException;
import com.studyanchor.focus.domain.DomainEvent;
import com.studyanchor.focus.domain.FocusSession;
import com.studyanchor.focus.domain.FocusSessionEnded;
import com.studyanchor.focus.domain.FocusSessionStarted;
import com.studyanchor.focus.domain.NotFoundException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Application-service rules for FR-05, using in-memory fakes (no Spring, no Kafka). */
class FocusSessionServiceTest {

    private final InMemoryFocusSessionRepository repository = new InMemoryFocusSessionRepository();
    private final List<DomainEvent> published = new ArrayList<>();
    private final List<Boolean> savedBeforePublish = new ArrayList<>();

    private final FocusSessionService service = new FocusSessionService(
            repository,
            event -> {
                published.add(event);
                savedBeforePublish.add(repository.contains(sessionIdOf(event)));
            },
            Clock.fixed(Instant.parse("2026-10-14T03:00:00Z"), ZoneOffset.UTC));

    private FocusSession start(String userId) {
        return service.start(userId, null, null, 25, List.of());
    }

    @Test
    void FR05_cannotStartSecondActiveSession() {
        start("alice");

        assertThatThrownBy(() -> start("alice")).isInstanceOf(ConflictException.class);
        assertThat(start("bob").isActive()).as("other users are not affected").isTrue();
    }

    @Test
    void FR05_canStartAgainAfterEnding() {
        FocusSession first = start("alice");
        service.end("alice", first.id());

        assertThat(start("alice").isActive()).isTrue();
    }

    @Test
    void FR05_cannotEndAnotherUsersSession() {
        FocusSession alices = start("alice");

        assertThatThrownBy(() -> service.end("bob", alices.id())).isInstanceOf(NotFoundException.class);
        assertThat(alices.isActive()).isTrue();
    }

    @Test
    void FR05_eventsPublishedOnlyAfterSave() {
        FocusSession session = start("alice");
        service.end("alice", session.id());

        assertThat(published).hasSize(2);
        assertThat(published.get(0)).isInstanceOf(FocusSessionStarted.class);
        assertThat(published.get(1)).isInstanceOf(FocusSessionEnded.class);
        assertThat(savedBeforePublish).containsOnly(true);
    }

    private static UUID sessionIdOf(DomainEvent event) {
        return switch (event) {
            case FocusSessionStarted e -> e.sessionId();
            case FocusSessionEnded e -> e.sessionId();
            default -> null;
        };
    }
}
