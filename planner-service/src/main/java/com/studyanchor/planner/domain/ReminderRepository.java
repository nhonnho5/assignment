package com.studyanchor.planner.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Implements FR-12. Repository for the {@link Reminder} aggregate; implemented in infrastructure. */
public interface ReminderRepository {

    Reminder save(Reminder reminder);

    /** Finds a reminder only if it belongs to the given user (otherwise "not found"). */
    Optional<Reminder> findByIdAndUserId(UUID id, String userId);

    /** Newest first. */
    List<Reminder> findUnreadByUserId(String userId);

    boolean existsByUserIdAndTriggerKey(String userId, String triggerKey);
}
