package com.studyanchor.planner.service;

import com.studyanchor.planner.domain.Goal;
import com.studyanchor.planner.domain.GoalRepository;
import com.studyanchor.planner.domain.NotFoundException;
import com.studyanchor.planner.domain.Reminder;
import com.studyanchor.planner.domain.ReminderRepository;
import com.studyanchor.planner.domain.ValueStatement;
import com.studyanchor.planner.domain.ValueStatementRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Application-service rules for FR-12 reminders, using in-memory fakes (no Spring, no Kafka). */
class ReminderServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-14T03:10:00Z");
    private static final Instant WINDOW_START = Instant.parse("2026-10-14T03:00:00Z");

    /** Keeps reminders in a list instead of a database. */
    static class InMemoryReminders implements ReminderRepository {

        final List<Reminder> reminders = new ArrayList<>();

        public Reminder save(Reminder r) {
            reminders.remove(r);
            reminders.add(r);
            return r;
        }

        public Optional<Reminder> findByIdAndUserId(UUID id, String userId) {
            return reminders.stream().filter(r -> r.id().equals(id) && r.userId().equals(userId)).findFirst();
        }

        public List<Reminder> findUnreadByUserId(String userId) {
            return reminders.stream().filter(r -> r.userId().equals(userId) && !r.isRead()).toList();
        }

        public boolean existsByUserIdAndTriggerKey(String userId, String triggerKey) {
            return reminders.stream().anyMatch(r -> r.userId().equals(userId) && r.triggerKey().equals(triggerKey));
        }
    }

    private final List<Goal> goals = new ArrayList<>();
    private final List<ValueStatement> values = new ArrayList<>();

    private final ReminderService service = new ReminderService(new InMemoryReminders(), fakeGoals(goals),
            fakeValues(values), Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void FR12_repeatedSpikeCreatesOneReminder() {
        service.onDistractionSpike("alice", 3, WINDOW_START);
        service.onDistractionSpike("alice", 3, WINDOW_START);
        service.onDistractionSpike("alice", 3, WINDOW_START.plusSeconds(1800));

        assertThat(service.unread("alice")).hasSize(2);
    }

    @Test
    void FR12_quotesOnlyUnachievedGoalsAndValues() {
        Goal achieved = Goal.create("alice", "Old goal", "Done already", null, NOW);
        achieved.achieve(NOW);
        goals.add(achieved);
        values.add(ValueStatement.create("alice", "Small steps", NOW));
        values.add(ValueStatement.create("bob", "Bob's words", NOW));

        service.onDistractionSpike("alice", 3, WINDOW_START);

        assertThat(service.unread("alice")).singleElement().satisfies(r ->
                assertThat(r.message()).contains("Small steps").doesNotContain("Old goal", "Bob's words"));
    }

    @Test
    void FR12_cannotReadAnotherStudentsReminder() {
        service.onDistractionSpike("alice", 3, WINDOW_START);
        UUID id = service.unread("alice").getFirst().id();

        assertThatThrownBy(() -> service.markRead("bob", id)).isInstanceOf(NotFoundException.class);
        assertThat(service.markRead("alice", id).readAt()).isEqualTo(NOW);
        assertThat(service.unread("alice")).isEmpty();
    }

    static GoalRepository fakeGoals(List<Goal> goals) {
        return new GoalRepository() {
            public Goal save(Goal g) {
                return g;
            }

            public Optional<Goal> findByIdAndUserId(UUID id, String userId) {
                return Optional.empty();
            }

            public List<Goal> findByUserId(String userId) {
                return goals.stream().filter(g -> g.userId().equals(userId)).toList();
            }

            public void delete(Goal g) {
            }
        };
    }

    static ValueStatementRepository fakeValues(List<ValueStatement> values) {
        return new ValueStatementRepository() {
            public ValueStatement save(ValueStatement v) {
                return v;
            }

            public Optional<ValueStatement> findByIdAndUserId(UUID id, String userId) {
                return Optional.empty();
            }

            public List<ValueStatement> findByUserId(String userId) {
                return values.stream().filter(v -> v.userId().equals(userId)).toList();
            }

            public void delete(ValueStatement v) {
            }
        };
    }
}
