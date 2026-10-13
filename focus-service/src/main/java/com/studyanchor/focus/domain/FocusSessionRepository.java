package com.studyanchor.focus.domain;

import java.util.Optional;
import java.util.UUID;

/** Implements FR-05. Repository for the {@link FocusSession} aggregate; implemented in infrastructure. */
public interface FocusSessionRepository {

    FocusSession save(FocusSession session);

    /** Finds a session only if it belongs to the given user, so other users' sessions look like "not found". */
    Optional<FocusSession> findByIdAndUserId(UUID id, String userId);

    boolean hasActiveSession(String userId);
}
