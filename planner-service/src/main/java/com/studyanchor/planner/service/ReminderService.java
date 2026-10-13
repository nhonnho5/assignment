package com.studyanchor.planner.service;

import com.studyanchor.planner.domain.Goal;
import com.studyanchor.planner.domain.GoalRepository;
import com.studyanchor.planner.domain.NotFoundException;
import com.studyanchor.planner.domain.Reminder;
import com.studyanchor.planner.domain.ReminderRepository;
import com.studyanchor.planner.domain.ValueStatement;
import com.studyanchor.planner.domain.ValueStatementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

/** Implements FR-12 AC2, AC6, AC9, AC10. Application service for reminders that quote the student's own "why". */
@Service
public class ReminderService {

    private final ReminderRepository reminders;
    private final GoalRepository goals;
    private final ValueStatementRepository values;
    private final Clock clock;

    public ReminderService(ReminderRepository reminders, GoalRepository goals, ValueStatementRepository values,
                           Clock clock) {
        this.reminders = reminders;
        this.goals = goals;
        this.values = values;
        this.clock = clock;
    }

    /** AC2, AC9: one reminder per spike window, quoting a not-yet-achieved goal or a value statement. */
    @Transactional
    public void onDistractionSpike(String userId, int distractionCount, Instant windowStart) {
        if (reminders.existsByUserIdAndTriggerKey(userId, Reminder.spikeKey(windowStart))) {
            return;
        }
        List<String> quotes = Stream.concat(
                goals.findByUserId(userId).stream().filter(g -> g.achievedAt() == null).map(Goal::quote),
                values.findByUserId(userId).stream().map(ValueStatement::quote)).toList();
        reminders.save(Reminder.forDistractionSpike(userId, distractionCount, windowStart, quotes,
                ThreadLocalRandom.current(), clock.instant()));
    }

    @Transactional(readOnly = true)
    public List<Reminder> unread(String userId) {
        return reminders.findUnreadByUserId(userId);
    }

    /** AC10: another student's reminder → 404; already read → 409 (from the aggregate). */
    @Transactional
    public Reminder markRead(String userId, UUID reminderId) {
        Reminder reminder = reminders.findByIdAndUserId(reminderId, userId)
                .orElseThrow(() -> new NotFoundException("Reminder " + reminderId + " not found"));
        reminder.markRead(clock.instant());
        return reminders.save(reminder);
    }
}
