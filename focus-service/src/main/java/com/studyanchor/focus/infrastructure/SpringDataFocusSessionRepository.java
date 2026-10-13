package com.studyanchor.focus.infrastructure;

import com.studyanchor.focus.domain.FocusSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/** Spring Data generates the SQL for these methods from their names. */
interface SpringDataFocusSessionRepository extends JpaRepository<FocusSession, UUID> {

    Optional<FocusSession> findByIdAndUserId(UUID id, String userId);

    boolean existsByUserIdAndEndedAtIsNull(String userId);
}
