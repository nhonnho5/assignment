package com.studyanchor.planner.infrastructure;

import com.studyanchor.planner.domain.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data generates the SQL for these methods from their names. */
interface SpringDataReminderRepository extends JpaRepository<Reminder, UUID> {

    Optional<Reminder> findByIdAndUserId(UUID id, String userId);

    List<Reminder> findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(String userId);

    boolean existsByUserIdAndTriggerKey(String userId, String triggerKey);
}
