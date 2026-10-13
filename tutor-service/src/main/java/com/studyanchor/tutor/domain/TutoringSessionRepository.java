package com.studyanchor.tutor.domain;

import java.util.Optional;
import java.util.UUID;

/** Implements FR-07. Repository for the {@link TutoringSession} aggregate; implemented in infrastructure. */
public interface TutoringSessionRepository {

    TutoringSession save(TutoringSession session);

    /** AC11: finds a session only if it belongs to the given user, so other users' sessions look like "not found". */
    Optional<TutoringSession> findByIdAndUserId(UUID id, String userId);

    /** AC13. */
    void deleteByTopicId(UUID topicId);
}
