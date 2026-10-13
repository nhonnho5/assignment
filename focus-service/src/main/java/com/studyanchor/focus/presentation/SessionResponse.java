package com.studyanchor.focus.presentation;

import com.studyanchor.focus.domain.FocusSession;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Implements FR-05, FR-06 AC8. What the API returns for a focus session, with its distractions in logged order. */
public record SessionResponse(
        UUID id,
        String userId,
        UUID timeBlockId,
        UUID topicId,
        int plannedMinutes,
        List<String> allowedSites,
        String status,
        Instant startedAt,
        Instant endedAt,
        String actualDuration,
        Long actualSeconds,
        List<DistractionResponse> distractions) {

    static SessionResponse from(FocusSession session) {
        Duration actual = session.actualDuration();
        return new SessionResponse(
                session.id(),
                session.userId(),
                session.timeBlockId(),
                session.topicId(),
                session.plannedMinutes().value(),
                session.allowedSites().stream().sorted().toList(),
                session.isActive() ? "ACTIVE" : "ENDED",
                session.startedAt(),
                session.endedAt(),
                actual == null ? null : actual.toMinutes() + "m " + actual.toSecondsPart() + "s",
                actual == null ? null : actual.toSeconds(),
                session.distractions().stream().map(DistractionResponse::from).toList());
    }
}
