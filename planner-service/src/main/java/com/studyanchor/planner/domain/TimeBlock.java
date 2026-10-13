package com.studyanchor.planner.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Implements FR-04 AC3, AC5, AC14 and FR-12 AC12. Entity inside the {@link StudyPlan} aggregate: a scheduled period
 * to study one topic. It keeps the topic's name and course code as they were when planned.
 */
@Entity
@Table(name = "time_block")
public class TimeBlock {

    public enum Status { PLANNED, STARTED, COMPLETED, MISSED }

    /** AC3: the least time between two blocks. */
    static final Duration GAP = Duration.ofMinutes(10);

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID topicId;

    @Column(nullable = false)
    private String topicName;

    @Column(nullable = false)
    private String courseCode;

    /** Stored in UTC; shown in Sydney time. */
    @Column(nullable = false)
    private Instant startsAt;

    @Column(nullable = false)
    private Instant endsAt;

    @Column(nullable = false, length = 200)
    private String rationale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    /** For JPA only. */
    protected TimeBlock() {
    }

    /** Only {@link StudyPlan} creates blocks; it checks the rules that need the week and the other blocks. */
    TimeBlock(Course course, Topic topic, Instant startsAt, Instant endsAt, String rationale) {
        this.id = UUID.randomUUID();
        this.topicId = topic.id();
        this.topicName = topic.name();
        this.courseCode = course.code().value();
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.rationale = Text.of(rationale, "rationale", 200);
        this.status = Status.PLANNED;
    }

    /** AC3: the two blocks overlap or are less than 10 minutes apart. */
    boolean tooCloseTo(TimeBlock other) {
        return startsAt.isBefore(other.endsAt.plus(GAP)) && other.startsAt.isBefore(endsAt.plus(GAP));
    }

    /** FR-12 AC12: a session was started for this block, so it will not count as missed. */
    void markStarted() {
        if (status == Status.PLANNED) {
            status = Status.STARTED;
        }
    }

    /** FR-12 AC12: a session for this block ended; late is fine, so even a missed block becomes completed. */
    void markCompleted() {
        status = Status.COMPLETED;
    }

    /** FR-12 AC12: true only the first time, when the block has ended and no session was started for it. */
    boolean markMissed(Instant now) {
        if (status != Status.PLANNED || endsAt.isAfter(now)) {
            return false;
        }
        status = Status.MISSED;
        return true;
    }

    public LocalDate date() {
        return StudyCalendar.dayOf(startsAt);
    }

    public LocalTime start() {
        return StudyCalendar.toSydney(startsAt).toLocalTime();
    }

    public LocalTime end() {
        return StudyCalendar.toSydney(endsAt).toLocalTime();
    }

    public long minutes() {
        return Duration.between(startsAt, endsAt).toMinutes();
    }

    public UUID id() {
        return id;
    }

    public UUID topicId() {
        return topicId;
    }

    public String topicName() {
        return topicName;
    }

    public String courseCode() {
        return courseCode;
    }

    public Instant startsAt() {
        return startsAt;
    }

    public String rationale() {
        return rationale;
    }

    public Status status() {
        return status;
    }
}
