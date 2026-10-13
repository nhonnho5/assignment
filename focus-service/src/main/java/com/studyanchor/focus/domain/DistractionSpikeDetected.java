package com.studyanchor.focus.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Implements FR-12 AC1, AC8. Published to {@code focus.alerts} by the spike stream when one student's fixed
 * 30-minute window reaches its 3rd distraction. Also holds the spike rule, so the stream and the tests share it.
 */
public record DistractionSpikeDetected(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String userId,
        int distractionCount,
        Instant windowStart,
        Instant windowEnd) implements DomainEvent {

    public static final String TYPE = "DistractionSpikeDetected";
    public static final Duration WINDOW = Duration.ofMinutes(30);
    public static final int THRESHOLD = 3;

    /** True only when a window's count reaches exactly the threshold, so each window alerts once. */
    public static boolean firesAt(long countInWindow) {
        return countInWindow == THRESHOLD;
    }

    public static DistractionSpikeDetected of(String userId, Instant windowStart, Instant windowEnd, Instant now) {
        return new DistractionSpikeDetected(UUID.randomUUID(), TYPE, now, userId, THRESHOLD, windowStart, windowEnd);
    }
}
