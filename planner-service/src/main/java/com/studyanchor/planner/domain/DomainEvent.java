package com.studyanchor.planner.domain;

import java.time.Instant;
import java.util.UUID;

/** Envelope fields every event carries (docs/05-events.md). The message key is {@link #userId()}. */
public interface DomainEvent {

    UUID eventId();

    String eventType();

    Instant occurredAt();

    String userId();
}
