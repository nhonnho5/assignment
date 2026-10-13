package com.studyanchor.tutor.infrastructure;

import com.studyanchor.tutor.domain.TopicReference;

import java.time.Instant;
import java.util.UUID;

/**
 * Implements FR-01 AC10. Tutor's own copy of the {@code planner.topics} contract (docs/05-events.md). One record
 * holds the fields of both TopicCreated and TopicDeleted; fields that don't apply are null.
 */
record PlannerTopicEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String userId,
        UUID topicId,
        UUID courseId,
        String courseCode,
        String topicName) {

    TopicReference toTopicReference() {
        return new TopicReference(topicId, userId, courseId, courseCode, topicName);
    }
}
