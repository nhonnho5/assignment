package com.studyanchor.planner.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
 * Implements FR-01, FR-02 AC8, FR-07 AC14. Entity inside the {@link Course} aggregate: a unit of content that can
 * be studied.
 * It remembers the IDs of the materials Tutor reported for it, so a repeated MaterialAdded is counted once, and the
 * latest understanding level Tutor recorded for it.
 */
@Entity
@Table(name = "topic")
public class Topic {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "topic_material", joinColumns = @JoinColumn(name = "topic_id"))
    @Column(name = "material_id")
    private Set<UUID> materialIds = new HashSet<>();

    private Integer understandingLevel;

    private Instant understandingRecordedAt;

    /** For JPA only. */
    protected Topic() {
    }

    /** Only {@link Course} creates topics (FR-01 AC6: name 1–100 characters). */
    Topic(String name) {
        this.id = UUID.randomUUID();
        this.name = Text.of(name, "Topic name", 100);
    }

    void recordMaterial(UUID materialId) {
        materialIds.add(materialId);
    }

    /** FR-07 AC14: the event with the latest occurredAt wins, so an older one arriving late changes nothing. */
    void recordUnderstanding(int level, Instant recordedAt) {
        if (understandingRecordedAt == null || recordedAt.isAfter(understandingRecordedAt)) {
            understandingLevel = level;
            understandingRecordedAt = recordedAt;
        }
    }

    /** FR-07 AC14: null until Tutor recorded a level. */
    public Integer understandingLevel() {
        return understandingLevel;
    }

    public int materialCount() {
        return materialIds.size();
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }
}
