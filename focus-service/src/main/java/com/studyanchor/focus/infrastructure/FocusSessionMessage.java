package com.studyanchor.focus.infrastructure;

import java.time.Instant;

/**
 * Implements FR-10. What the progress stream reads from {@code focus.sessions}: only the fields it needs.
 * Both FocusSessionStarted and FocusSessionEnded arrive here; {@code actualSeconds} is null for Started.
 */
record FocusSessionMessage(String eventType, String userId, Long actualSeconds, Instant startedAt) {

    static final String ENDED = "FocusSessionEnded";

    boolean isEnded() {
        return ENDED.equals(eventType) && actualSeconds != null;
    }
}
