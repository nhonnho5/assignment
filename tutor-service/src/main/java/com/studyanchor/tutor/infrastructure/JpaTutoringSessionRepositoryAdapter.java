package com.studyanchor.tutor.infrastructure;

import com.studyanchor.tutor.domain.TutoringSession;
import com.studyanchor.tutor.domain.TutoringSessionRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Implements FR-07. Adapter: fulfils the domain repository interface using Spring Data JPA and H2. */
@Repository
class JpaTutoringSessionRepositoryAdapter implements TutoringSessionRepository {

    private final SpringDataTutoringSessionRepository jpa;

    JpaTutoringSessionRepositoryAdapter(SpringDataTutoringSessionRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public TutoringSession save(TutoringSession session) {
        return jpa.save(session);
    }

    @Override
    public Optional<TutoringSession> findByIdAndUserId(UUID id, String userId) {
        return jpa.findByIdAndUserId(id, userId);
    }

    @Override
    public void deleteByTopicId(UUID topicId) {
        jpa.deleteByTopicId(topicId);
    }
}
