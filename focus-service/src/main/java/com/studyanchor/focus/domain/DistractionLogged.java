package com.studyanchor.focus.domain;

import java.time.Instant;
import java.util.UUID;

/** Implements FR-06 AC4. Published to {@code focus.distractions} when a distraction is logged; the note stays in Focus. */
public record DistractionLogged(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String userId,
        UUID sessionId,
        DistractionType distractionType,
        String site,
        boolean offPolicy) implements DomainEvent {

    public static final String TYPE = "DistractionLogged";

    static DistractionLogged of(FocusSession session, Distraction d) {
        return new DistractionLogged(UUID.randomUUID(), TYPE, d.loggedAt(), session.userId(), session.id(),
                d.type(), d.site(), d.offPolicy());
    }
}
