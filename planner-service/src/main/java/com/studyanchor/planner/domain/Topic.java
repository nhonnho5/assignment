package com.studyanchor.planner.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/** Implements FR-01. Entity inside the {@link Course} aggregate: a unit of content that can be studied. */
@Entity
@Table(name = "topic")
public class Topic {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    /** For JPA only. */
    protected Topic() {
    }

    /** Only {@link Course} creates topics (FR-01 AC6: name 1–100 characters). */
    Topic(String name) {
        this.id = UUID.randomUUID();
        this.name = Text.of(name, "Topic name", 100);
    }

    public UUID id() {
        return id;
    }

    public String name() {
        return name;
    }
}
