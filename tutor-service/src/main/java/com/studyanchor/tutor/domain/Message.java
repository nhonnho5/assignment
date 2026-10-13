package com.studyanchor.tutor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Lob;

import java.time.Instant;

/**
 * Implements FR-07. Value object inside the {@link TutoringSession} aggregate: one line of the transcript.
 * A student message the tutor judged a real try is marked as an attempt (AC7).
 */
@Embeddable
public record Message(boolean fromStudent, @Lob @Column(nullable = false) String text, boolean attempt,
                      @Column(nullable = false) Instant sentAt) {

    Message asAttempt() {
        return new Message(fromStudent, text, true, sentAt);
    }
}
