package com.studyanchor.tutor.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Implements FR-02. Repository for the {@link Material} aggregate; implemented in infrastructure. */
public interface MaterialRepository {

    Material save(Material material);

    /** Finds a material only if it belongs to the given user, so other users' materials look like "not found". */
    Optional<Material> findByIdAndUserId(UUID id, String userId);

    /** Oldest first. */
    List<Material> findByTopicId(UUID topicId);

    void deleteByTopicId(UUID topicId);
}
