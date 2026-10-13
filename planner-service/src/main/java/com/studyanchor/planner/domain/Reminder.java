package com.studyanchor.planner.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.random.RandomGenerator;

/**
 * Implements FR-12 AC2, AC4, AC6, AC9, AC10. Aggregate root: a message to the student that quotes their own goals or
 * values. {@code triggerKey} names what caused it (one spike window, one missed block), so the same cause never
 * creates two reminders.
 */
@Entity
@Table(name = "reminder", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "trigger_key"}))
public class Reminder {

    public enum Kind { DISTRACTION_SPIKE, MISSED_BLOCK }

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);

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

    /** AC2, AC9: one reminder per spike window. */
    public static Reminder forDistractionSpike(String userId, int distractionCount, Instant windowStart,
                                               List<String> quotes, RandomGenerator random, Instant now) {
        return create(userId, Kind.DISTRACTION_SPIKE, spikeKey(windowStart), "You logged " + distractionCount
                + " distractions in half an hour. Take a short break, then start again. ", quotes, random, now);
    }

    /** AC4, AC12: one reminder per missed time block. */
    public static Reminder forMissedBlock(String userId, TimeBlock block, List<String> quotes, RandomGenerator random,
                                          Instant now) {
        return create(userId, Kind.MISSED_BLOCK, missedBlockKey(block.id()), "You missed your study block for "
                + block.topicName() + " (" + block.courseCode() + ") on " + DAY.format(block.date()) + ", "
                + block.start() + "–" + block.end() + ". Start a short session when you can. ", quotes, random, now);
    }

    /** One reminder per spike window (AC9). */
    public static String spikeKey(Instant windowStart) {
        return "spike:" + windowStart;
    }

    /** One reminder per missed block (AC4). */
    public static String missedBlockKey(UUID blockId) {
        return "missed:" + blockId;
    }

    /** AC9: quotes one of {@code quotes} at random; with none, a tip to add a goal. */
    private static Reminder create(String userId, Kind kind, String triggerKey, String opening, List<String> quotes,
                                   RandomGenerator random, Instant now) {
        Reminder reminder = new Reminder();
        reminder.id = UUID.randomUUID();
        reminder.userId = Text.userId(userId);
        reminder.kind = kind;
        reminder.message = quotes.isEmpty()
                ? opening + "Tip: add a goal or value statement so reminders can quote your own words."
                : opening + "Remember " + quotes.get(random.nextInt(quotes.size())) + ".";
        reminder.triggerKey = triggerKey;
        reminder.createdAt = now;
        return reminder;
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
