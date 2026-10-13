package com.studyanchor.planner.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static com.studyanchor.planner.domain.DomainException.required;
import static java.time.temporal.ChronoUnit.MINUTES;

/**
 * Implements FR-04 and FR-12 AC12. Aggregate root: one student's {@link TimeBlock}s for one week (Monday–Sunday,
 * Sydney time). Every block rule (AC3, AC10) is checked here, so the planning agent cannot break one.
 */
@Entity
@Table(name = "study_plan", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "week_start"}))
public class StudyPlan {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private LocalDate weekStart;

    @Column(length = 4000)
    private String summary;

    @Column(length = 500)
    private String shortfall;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "plan_id", nullable = false)
    private Set<TimeBlock> blocks = new HashSet<>();

    /** For JPA only. */
    protected StudyPlan() {
    }

    /** AC1, AC9: the week starts on a Monday and has not ended yet in Sydney. */
    public static StudyPlan create(String userId, LocalDate weekStart, Instant now) {
        required(weekStart, "weekStart");
        if (weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new DomainException("weekStart must be a Monday, was a " + weekStart.getDayOfWeek());
        }
        if (weekStart.plusDays(6).isBefore(StudyCalendar.dayOf(now))) {
            throw new DomainException("The week of " + weekStart + " is already over");
        }
        StudyPlan plan = new StudyPlan();
        plan.id = UUID.randomUUID();
        plan.userId = Text.userId(userId);
        plan.weekStart = weekStart;
        return plan;
    }

    /**
     * AC3, AC10: a block in this week, whole minutes, 25–90 minutes, inside one availability slot, starting after
     * {@code now}, and at least 10 minutes from every other block.
     */
    public TimeBlock addBlock(Course course, Topic topic, LocalDate date, LocalTime start, LocalTime end,
                              String rationale, WeeklyAvailability availability, Instant now) {
        required(date, "date");
        required(start, "start");
        required(end, "end");
        if (date.isBefore(weekStart) || date.isAfter(weekStart.plusDays(6))) {
            throw new DomainException(date + " is not in the week of " + weekStart);
        }
        if (!start.equals(start.truncatedTo(MINUTES)) || !end.equals(end.truncatedTo(MINUTES))) {
            throw new DomainException("Times must be whole minutes (HH:mm)");
        }
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes < 25 || minutes > 90) {
            throw new DomainException("A block must be 25–90 minutes long, was " + minutes);
        }
        if (!availability.covers(date.getDayOfWeek(), start, end)) {
            throw new DomainException(date.getDayOfWeek() + " " + start + "–" + end
                    + " is not inside one availability slot");
        }
        TimeBlock block = new TimeBlock(course, topic, StudyCalendar.toInstant(date.atTime(start)),
                StudyCalendar.toInstant(date.atTime(end)), rationale);
        if (!block.startsAt().isAfter(now)) {
            throw new DomainException("A block cannot start in the past (" + date + " " + start + ")");
        }
        blocks.stream().filter(block::tooCloseTo).findFirst().ifPresent(other -> {
            throw new DomainException("A block needs at least 10 minutes from the block " + other.date() + " "
                    + other.start() + "–" + other.end());
        });
        blocks.add(block);
        return block;
    }

    /**
     * AC6, AC15: the agent explains that the available time is not enough. Refused while the plan is empty and a
     * slot later this week is still free, so the agent cannot give up instead of using the time there is.
     */
    public void reportShortfall(String reason, WeeklyAvailability availability, Instant now) {
        String text = Text.of(reason, "reason", 500);
        if (blocks.isEmpty() && hasSlotStartingAfter(availability, now)) {
            throw new DomainException("Use the free time first: add blocks for the highest-ranked topics, then"
                    + " report the shortfall");
        }
        shortfall = text;
    }

    private boolean hasSlotStartingAfter(WeeklyAvailability availability, Instant now) {
        return IntStream.range(0, 7).mapToObj(weekStart::plusDays).anyMatch(date -> availability.slots().stream()
                .anyMatch(s -> s.day() == date.getDayOfWeek()
                        && StudyCalendar.toInstant(date.atTime(s.start())).isAfter(now)));
    }

    /** AC11: the agent's closing message to the student. */
    public void summarise(String text) {
        String stripped = text.strip();
        summary = stripped.length() > 4000 ? stripped.substring(0, 4000) : stripped;
    }

    /** AC8: replacing the plan keeps its identity but drops the old blocks, shortfall and summary. */
    public void startOver() {
        blocks.clear();
        shortfall = null;
        summary = null;
    }

    /** FR-12 AC12. */
    public void sessionStarted(UUID blockId) {
        block(blockId).markStarted();
    }

    /** FR-12 AC12. */
    public void sessionEnded(UUID blockId) {
        block(blockId).markCompleted();
    }

    /** FR-12 AC4, AC12: marks every block that ended without a session; returns only the newly missed ones. */
    public List<TimeBlock> markMissed(Instant now) {
        List<TimeBlock> missed = new ArrayList<>();
        for (TimeBlock block : blocks) {
            if (block.markMissed(now)) {
                missed.add(block);
            }
        }
        return missed;
    }

    private TimeBlock block(UUID blockId) {
        return blocks.stream().filter(b -> b.id().equals(blockId)).findFirst()
                .orElseThrow(() -> new NotFoundException("Time block " + blockId + " not found"));
    }

    public UUID id() {
        return id;
    }

    public String userId() {
        return userId;
    }

    public LocalDate weekStart() {
        return weekStart;
    }

    public String summary() {
        return summary;
    }

    public String shortfall() {
        return shortfall;
    }

    /** In time order. */
    public List<TimeBlock> blocks() {
        return blocks.stream().sorted(Comparator.comparing(TimeBlock::startsAt)).toList();
    }
}
