package com.studyanchor.planner.infrastructure;

import java.time.Instant;
import java.util.UUID;

/** Implements FR-12 AC2. Planner's own copy of the {@code focus.alerts} contract (docs/05-events.md). */
record DistractionSpikeEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String userId,
        int distractionCount,
        Instant windowStart,
        Instant windowEnd) {
}
