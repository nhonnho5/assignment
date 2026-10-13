package com.studyanchor.planner.infrastructure;

import com.studyanchor.planner.domain.Goal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data generates the SQL for these methods from their names. */
interface SpringDataGoalRepository extends JpaRepository<Goal, UUID> {

    Optional<Goal> findByIdAndUserId(UUID id, String userId);

    List<Goal> findByUserIdOrderByCreatedAt(String userId);
}
