package com.studyanchor.focus.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Implements FR-05. Aggregate root: one timed focus session of one student.
 * It can only be changed through {@link #start} and {@link #end}; both register a domain event
 * that the application service publishes after saving.
 */
@Entity
@Table(name = "focus_session")
public class FocusSession {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String userId;

    private UUID timeBlockId;

    private UUID topicId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "planned_minutes", nullable = false))
    private PlannedMinutes plannedMinutes;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "focus_session_allowed_site", joinColumns = @JoinColumn(name = "session_id"))
    @Column(name = "site")
    private Set<String> allowedSites = new LinkedHashSet<>();

    @Column(nullable = false)
    private Instant startedAt;

    private Instant endedAt;

    /** Exact duration in whole seconds (FR-05 AC5); null while the session is active. */
    private Long actualSeconds;

    @Transient
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    /** For JPA only. */
    protected FocusSession() {
    }

    /**
     * Starts a new session (FR-05 AC1). Time block and topic IDs are stored as given (AC8).
     * Allowed sites are trimmed and lower-cased so FR-06 can compare them; blank entries are dropped.
     */
    public static FocusSession start(String userId, UUID timeBlockId, UUID topicId, Integer plannedMinutes,
                                     Collection<String> allowedSites, Instant now) {
        if (userId == null || userId.isBlank()) {
            throw new DomainException("userId is required");
        }
        FocusSession session = new FocusSession();
        session.id = UUID.randomUUID();
        session.userId = userId;
        session.timeBlockId = timeBlockId;
        session.topicId = topicId;
        session.plannedMinutes = PlannedMinutes.of(plannedMinutes);
        if (allowedSites != null) {
            for (String site : allowedSites) {
                if (site != null && !site.isBlank()) {
                    session.allowedSites.add(site.trim().toLowerCase(Locale.ROOT));
                }
            }
        }
        session.startedAt = now;
        session.domainEvents.add(FocusSessionStarted.of(session));
        return session;
    }

    /** Ends the session and records the exact duration, dropping fractions of a second (FR-05 AC3, AC5, AC6). */
    public void end(Instant now) {
        if (!isActive()) {
            throw new ConflictException("Focus session " + id + " has already ended");
        }
        if (now.isBefore(startedAt)) {
            throw new DomainException("A session cannot end before it started");
        }
        endedAt = now;
        actualSeconds = Duration.between(startedAt, now).truncatedTo(ChronoUnit.SECONDS).toSeconds();
        domainEvents.add(FocusSessionEnded.of(this));
    }

    public boolean isActive() {
        return endedAt == null;
    }

    /** Returns the events registered since the last call and forgets them, so each is published once. */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    public UUID id() {
        return id;
    }

    public String userId() {
        return userId;
    }

    public UUID timeBlockId() {
        return timeBlockId;
    }

    public UUID topicId() {
        return topicId;
    }

    public PlannedMinutes plannedMinutes() {
        return plannedMinutes;
    }

    public Set<String> allowedSites() {
        return Set.copyOf(allowedSites);
    }

    public Instant startedAt() {
        return startedAt;
    }

    public Instant endedAt() {
        return endedAt;
    }

    /** The exact duration, or null while the session is still active. */
    public Duration actualDuration() {
        return actualSeconds == null ? null : Duration.ofSeconds(actualSeconds);
    }
}
