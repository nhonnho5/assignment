package com.studyanchor.tutor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Implements FR-01 AC10. Read model: Tutor's own copy of a topic owned by Planner, built from
 * {@code planner.topics} events. Planner decides the rules for topics; Tutor only mirrors them.
 */
@Entity
@Table(name = "topic_reference")
public class TopicReference {

    @Id
    private UUID topicId;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private UUID courseId;

    @Column(nullable = false)
    private String courseCode;

    @Column(nullable = false)
    private String topicName;

    /** For JPA only. */
    protected TopicReference() {
    }

    public TopicReference(UUID topicId, String userId, UUID courseId, String courseCode, String topicName) {
        this.topicId = topicId;
        this.userId = userId;
        this.courseId = courseId;
        this.courseCode = courseCode;
        this.topicName = topicName;
    }

    public UUID topicId() {
        return topicId;
    }

    public String userId() {
        return userId;
    }

    public UUID courseId() {
        return courseId;
    }

    public String courseCode() {
        return courseCode;
    }

    public String topicName() {
        return topicName;
    }
}
