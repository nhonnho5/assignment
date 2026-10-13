package com.studyanchor.planner.infrastructure;

import com.studyanchor.planner.domain.ValueStatement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data generates the SQL for these methods from their names. */
interface SpringDataValueStatementRepository extends JpaRepository<ValueStatement, UUID> {

    Optional<ValueStatement> findByIdAndUserId(UUID id, String userId);

    List<ValueStatement> findByUserIdOrderByCreatedAt(String userId);
}
