package com.studyanchor.planner.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Implements FR-11 AC2, AC5. Aggregate root: a short idea the student wants to be reminded of, 1–280 characters. */
@Entity
@Table(name = "value_statement")
public class ValueStatement {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, length = 280)
    private String text;

    @Column(nullable = false)
    private Instant createdAt;

    /** For JPA only. */
    protected ValueStatement() {
    }

    public static ValueStatement create(String userId, String text, Instant now) {
        ValueStatement value = new ValueStatement();
        value.id = UUID.randomUUID();
        value.userId = Text.userId(userId);
        value.text = Text.of(text, "text", 280);
        value.createdAt = now;
        return value;
    }

    /** FR-12 AC2: the value statement in the student's own words, for a reminder. */
    public String quote() {
        return "\"" + text + "\"";
    }

    public UUID id() {
        return id;
    }

    public String userId() {
        return userId;
    }

    public String text() {
        return text;
    }
}
