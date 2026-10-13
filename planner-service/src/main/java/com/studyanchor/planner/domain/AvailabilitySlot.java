package com.studyanchor.planner.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.UUID;

import static com.studyanchor.planner.domain.DomainException.required;
import static java.time.temporal.ChronoUnit.MINUTES;

/** Implements FR-03 AC1, AC3, AC5. Entity inside {@link WeeklyAvailability}: one weekly time slot in Sydney time. */
@Entity
@Table(name = "availability_slot")
public class AvailabilitySlot {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false)
    private DayOfWeek day;

    @Column(name = "start_time", nullable = false)
    private LocalTime start;

    @Column(name = "end_time", nullable = false)
    private LocalTime end;

    /** For JPA only. */
    protected AvailabilitySlot() {
    }

    /** Whole minutes, end after start on the same day, at least 25 minutes long (exactly 25 is allowed). */
    AvailabilitySlot(DayOfWeek day, LocalTime start, LocalTime end) {
        this.id = UUID.randomUUID();
        this.day = required(day, "day");
        this.start = required(start, "start");
        this.end = required(end, "end");
        if (!start.equals(start.truncatedTo(MINUTES)) || !end.equals(end.truncatedTo(MINUTES))) {
            throw new DomainException("Times must be whole minutes (HH:mm)");
        }
        if (!end.isAfter(start)) {
            throw new DomainException("end must be after start on the same day");
        }
        if (Duration.between(start, end).toMinutes() < 25) {
            throw new DomainException("A slot must be at least 25 minutes long");
        }
    }

    /** FR-03 AC2, AC5: same day and the times cross; touching slots (10:00 end, 10:00 start) do not overlap. */
    boolean overlaps(AvailabilitySlot other) {
        return day == other.day && start.isBefore(other.end) && other.start.isBefore(end);
    }

    /** FR-04 AC10: the time from {@code from} to {@code to} on {@code day} lies inside this slot. */
    boolean contains(DayOfWeek day, LocalTime from, LocalTime to) {
        return this.day == day && !from.isBefore(start) && !to.isAfter(end);
    }

    public UUID id() {
        return id;
    }

    public DayOfWeek day() {
        return day;
    }

    public LocalTime start() {
        return start;
    }

    public LocalTime end() {
        return end;
    }
}
