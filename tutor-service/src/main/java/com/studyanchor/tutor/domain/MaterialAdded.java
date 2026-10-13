package com.studyanchor.tutor.domain;

import java.time.Instant;
import java.util.UUID;

/** Implements FR-02 AC3. Published to {@code tutor.materials} when material is added; Planner counts it. */
public record MaterialAdded(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String userId,
        UUID materialId,
        UUID topicId,
        String title) implements DomainEvent {

    static MaterialAdded of(Material m) {
        return new MaterialAdded(UUID.randomUUID(), "MaterialAdded", m.addedAt(), m.userId(), m.id(), m.topicId(),
                m.title());
    }
}
