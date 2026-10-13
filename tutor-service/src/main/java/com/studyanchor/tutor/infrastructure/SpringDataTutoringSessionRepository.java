package com.studyanchor.tutor.infrastructure;

import com.studyanchor.tutor.domain.TutoringSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Spring Data generates the SQL for these methods from their names. */
interface SpringDataTutoringSessionRepository extends JpaRepository<TutoringSession, UUID> {

    Optional<TutoringSession> findByIdAndUserId(UUID id, String userId);

    void deleteByTopicId(UUID topicId);
}
