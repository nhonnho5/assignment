package com.studyanchor.planner.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.random.RandomGenerator;

/**
 * Implements FR-12 AC2, AC6, AC9, AC10. Aggregate root: a message to the student that quotes their own goals or values.
 * {@code triggerKey} names what caused it (e.g. one spike window), so the same cause never creates two reminders.
 */
@Entity
@Table(name = "reminder", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "trigger_key"}))
public class Reminder {

    public enum Kind { DISTRACTION_SPIKE }

    @Id
    private UUID id;

    @Column(nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Kind kind;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false)
    private String triggerKey;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant readAt;

    /** For JPA only. */
    protected Reminder() {
    }

    /** AC2, AC9: quotes one of {@code quotes} at random; with none, a general message and a tip to add a goal. */
    public static Reminder forDistractionSpike(String userId, int distractionCount, Instant windowStart,
                                               List<String> quotes, RandomGenerator random, Instant now) {
        String opening = "You logged " + distractionCount + " distractions in half an hour. ";
        Reminder reminder = new Reminder();
        reminder.id = UUID.randomUUID();
        reminder.userId = Text.userId(userId);
        reminder.kind = Kind.DISTRACTION_SPIKE;
        reminder.message = quotes.isEmpty()
                ? opening + "Take a short break, then start again. Tip: add a goal or value statement so reminders"
                        + " can quote your own words."
                : opening + "Take a breath and remember " + quotes.get(random.nextInt(quotes.size())) + ".";
        reminder.triggerKey = spikeKey(windowStart);
        reminder.createdAt = now;
        return reminder;
    }

    /** One reminder per spike window (AC9). */
    public static String spikeKey(Instant windowStart) {
        return "spike:" + windowStart;
    }

    /** AC10: records when; marking it read again is a conflict. */
    public void markRead(Instant now) {
        if (isRead()) {
            throw new ConflictException("Reminder " + id + " is already read");
        }
        readAt = now;
    }

    public boolean isRead() {
        return readAt != null;
    }

    public UUID id() {
        return id;
    }

    public String userId() {
        return userId;
    }

    public Kind kind() {
        return kind;
    }

    public String message() {
        return message;
    }

    public String triggerKey() {
        return triggerKey;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant readAt() {
        return readAt;
    }
}
