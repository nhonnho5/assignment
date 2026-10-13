package com.studyanchor.planner.service;

import com.studyanchor.planner.domain.AvailabilitySlot;
import com.studyanchor.planner.domain.WeeklyAvailability;
import com.studyanchor.planner.domain.WeeklyAvailabilityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

/** Implements FR-03. Application service for weekly availability: load (or start empty), call the aggregate, save. */
@Service
public class AvailabilityService {

    private final WeeklyAvailabilityRepository repository;

    public AvailabilityService(WeeklyAvailabilityRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AvailabilitySlot addSlot(String userId, DayOfWeek day, LocalTime start, LocalTime end) {
        WeeklyAvailability availability = get(userId);
        AvailabilitySlot slot = availability.add(day, start, end);
        repository.save(availability);
        return slot;
    }

    /** A student who never added a slot has an empty week. */
    @Transactional(readOnly = true)
    public WeeklyAvailability get(String userId) {
        return repository.findByUserId(userId).orElseGet(() -> WeeklyAvailability.emptyFor(userId));
    }

    @Transactional
    public void removeSlot(String userId, UUID slotId) {
        WeeklyAvailability availability = get(userId);
        availability.remove(slotId);
        repository.save(availability);
    }
}
