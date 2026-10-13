package com.studyanchor.focus.infrastructure;

import com.studyanchor.focus.domain.FocusSession;
import com.studyanchor.focus.domain.FocusSessionRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Implements FR-05. Adapter: fulfils the domain repository interface using Spring Data JPA and H2. */
@Repository
class JpaFocusSessionRepositoryAdapter implements FocusSessionRepository {

    private final SpringDataFocusSessionRepository jpa;

    JpaFocusSessionRepositoryAdapter(SpringDataFocusSessionRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public FocusSession save(FocusSession session) {
        return jpa.save(session);
    }

    @Override
    public Optional<FocusSession> findByIdAndUserId(UUID id, String userId) {
        return jpa.findByIdAndUserId(id, userId);
    }

    @Override
    public boolean hasActiveSession(String userId) {
        return jpa.existsByUserIdAndEndedAtIsNull(userId);
    }
}
