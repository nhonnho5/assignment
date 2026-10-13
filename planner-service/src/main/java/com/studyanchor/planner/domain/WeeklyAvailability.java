package com.studyanchor.planner.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Implements FR-03. Aggregate root (one per student, identified by userId): the weekly slots the student is
 * willing to study. It sees all slots at once, so it is the place that can reject overlaps.
 */
@Entity
@Table(name = "weekly_availability")
public class WeeklyAvailability {

    @Id
    private String userId;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private Set<AvailabilitySlot> slots = new HashSet<>();

    /** For JPA only. */
    protected WeeklyAvailability() {
    }

    public static WeeklyAvailability emptyFor(String userId) {
        WeeklyAvailability availability = new WeeklyAvailability();
        availability.userId = Text.userId(userId);
        return availability;
    }

    /** FR-03 AC1, AC2, AC4: adds one slot; overlapping another slot on the same day is a conflict (409). */
    public AvailabilitySlot add(DayOfWeek day, LocalTime start, LocalTime end) {
        AvailabilitySlot slot = new AvailabilitySlot(day, start, end);
        if (slots.stream().anyMatch(slot::overlaps)) {
            throw new ConflictException("The slot overlaps another slot on " + day);
        }
        slots.add(slot);
        return slot;
    }

    /** FR-03 AC4: unknown slot → 404. */
    public void remove(UUID slotId) {
        if (!slots.removeIf(s -> s.id().equals(slotId))) {
            throw new NotFoundException("Availability slot " + slotId + " not found");
        }
    }

    public List<AvailabilitySlot> slots() {
        return List.copyOf(slots);
    }
}
