package com.studyanchor.planner.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Implements FR-11 AC1, AC3, AC4. Aggregate root: a personal objective with a "why" and an optional target date. */
@Entity
@Table(name = "goal")
public class Goal {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 500)
    private String why;

    private LocalDate targetDate;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant achievedAt;

    /** For JPA only. */
    protected Goal() {
    }

    /** FR-11 AC3: title 1–100 and why 1–500 characters; the target date may not be before today in Sydney. */
    public static Goal create(String userId, String title, String why, LocalDate targetDate, Instant now) {
        if (targetDate != null && targetDate.isBefore(StudyCalendar.dayOf(now))) {
            throw new DomainException("targetDate cannot be before today (Australia/Sydney)");
        }
        Goal goal = new Goal();
        goal.id = UUID.randomUUID();
        goal.userId = Text.userId(userId);
        goal.title = Text.of(title, "title", 100);
        goal.why = Text.of(why, "why", 500);
        goal.targetDate = targetDate;
        goal.createdAt = now;
        return goal;
    }

    /** FR-11 AC4: records when; achieving twice is a conflict and there is no "un-achieve". */
    public void achieve(Instant now) {
        if (achievedAt != null) {
            throw new ConflictException("Goal " + id + " is already achieved");
        }
        achievedAt = now;
    }

    /** FR-12 AC2: the goal in the student's own words, for a reminder. */
    public String quote() {
        return "your goal \"" + title + "\": \"" + why + "\"";
    }

    public UUID id() {
        return id;
    }

    public String userId() {
        return userId;
    }

    public String title() {
        return title;
    }

    public String why() {
        return why;
    }

    public LocalDate targetDate() {
        return targetDate;
    }

    public Instant achievedAt() {
        return achievedAt;
    }
}
