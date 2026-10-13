package com.studyanchor.planner.domain;

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

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Implements FR-01 AC2. Entity inside the {@link Course} aggregate: a dated assessment item linked to topics.
 * The rules that need the whole course (topics exist, total weight) are checked by {@link Course#addDeadline}.
 */
@Entity
@Table(name = "deadline")
public class Deadline {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String title;

    /** Stored in UTC (FR-01 AC8). */
    @Column(nullable = false)
    private Instant dueAt;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "weight", nullable = false))
    private Weight weight;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "deadline_topic", joinColumns = @JoinColumn(name = "deadline_id"))
    @Column(name = "topic_id")
    private Set<UUID> topicIds = new HashSet<>();

    /** For JPA only. */
    protected Deadline() {
    }

    Deadline(String title, Instant dueAt, Weight weight, Set<UUID> topicIds) {
        this.id = UUID.randomUUID();
        this.title = Text.of(title, "Deadline title", 100);
        this.dueAt = dueAt;
        this.weight = weight;
        this.topicIds.addAll(topicIds);
    }

    public UUID id() {
        return id;
    }

    public String title() {
        return title;
    }

    public Instant dueAt() {
        return dueAt;
    }

    public Weight weight() {
        return weight;
    }

    public Set<UUID> topicIds() {
        return Set.copyOf(topicIds);
    }
}
