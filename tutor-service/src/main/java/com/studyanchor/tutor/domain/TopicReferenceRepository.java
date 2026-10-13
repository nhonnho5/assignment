package com.studyanchor.tutor.domain;

import java.util.List;
import java.util.UUID;

/** Implements FR-01 AC10. Repository for the {@link TopicReference} read model; implemented in infrastructure. */
public interface TopicReferenceRepository {

    TopicReference save(TopicReference topic);

    boolean existsById(UUID topicId);

    /** Sorted by course code, then topic name. */
    List<TopicReference> findByUserId(String userId);

    /** Does nothing if the topic is unknown. */
    void deleteById(UUID topicId);
}
