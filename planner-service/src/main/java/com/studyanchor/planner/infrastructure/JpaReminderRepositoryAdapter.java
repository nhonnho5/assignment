package com.studyanchor.planner.infrastructure;

import com.studyanchor.planner.domain.Reminder;
import com.studyanchor.planner.domain.ReminderRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Implements FR-12. Adapter: fulfils the domain repository interface using Spring Data JPA and H2. */
@Repository
class JpaReminderRepositoryAdapter implements ReminderRepository {

    private final SpringDataReminderRepository jpa;

    JpaReminderRepositoryAdapter(SpringDataReminderRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Reminder save(Reminder reminder) {
        return jpa.save(reminder);
    }

    @Override
    public Optional<Reminder> findByIdAndUserId(UUID id, String userId) {
        return jpa.findByIdAndUserId(id, userId);
    }

    @Override
    public List<Reminder> findUnreadByUserId(String userId) {
        return jpa.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId);
    }

    @Override
    public boolean existsByUserIdAndTriggerKey(String userId, String triggerKey) {
        return jpa.existsByUserIdAndTriggerKey(userId, triggerKey);
    }
}
