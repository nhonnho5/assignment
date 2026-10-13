package com.studyanchor.focus.service;

import com.studyanchor.focus.domain.FocusSession;
import com.studyanchor.focus.domain.FocusSessionRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Test fake: keeps sessions in a map instead of a database. */
class InMemoryFocusSessionRepository implements FocusSessionRepository {

    private final Map<UUID, FocusSession> sessions = new HashMap<>();

    @Override
    public FocusSession save(FocusSession session) {
        sessions.put(session.id(), session);
        return session;
    }

    @Override
    public Optional<FocusSession> findByIdAndUserId(UUID id, String userId) {
        return Optional.ofNullable(sessions.get(id)).filter(s -> s.userId().equals(userId));
    }

    @Override
    public boolean hasActiveSession(String userId) {
        return sessions.values().stream().anyMatch(s -> s.userId().equals(userId) && s.isActive());
    }

    boolean contains(UUID id) {
        return sessions.containsKey(id);
    }
}
