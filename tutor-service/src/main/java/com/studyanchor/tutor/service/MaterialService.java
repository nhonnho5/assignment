package com.studyanchor.tutor.service;

import com.studyanchor.tutor.domain.EventPublisher;
import com.studyanchor.tutor.domain.Material;
import com.studyanchor.tutor.domain.MaterialRepository;
import com.studyanchor.tutor.domain.MaterialText;
import com.studyanchor.tutor.domain.NotFoundException;
import com.studyanchor.tutor.domain.TopicReferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Implements FR-02. Application service for study material: checks the topic is the student's, turns the
 * uploaded file into text, saves the material, then publishes MaterialAdded.
 */
@Service
public class MaterialService {

    private final MaterialRepository materials;
    private final TopicReferenceRepository topics;
    private final MaterialText materialText;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    public MaterialService(MaterialRepository materials, TopicReferenceRepository topics, MaterialText materialText,
                           EventPublisher eventPublisher, Clock clock) {
        this.materials = materials;
        this.topics = topics;
        this.materialText = materialText;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /** FR-02 AC1–5: an unknown topic or another student's topic → 404. */
    @Transactional
    public Material add(String userId, UUID topicId, String fileName, byte[] bytes, String title) {
        requireOwnTopic(userId, topicId);
        String content = materialText.extract(fileName, bytes);
        Material material = Material.create(userId, topicId, fileName, title, content, clock.instant());
        materials.save(material);
        material.pullDomainEvents().forEach(eventPublisher::publish);
        return material;
    }

    /** FR-02 AC6. */
    @Transactional(readOnly = true)
    public List<Material> list(String userId, UUID topicId) {
        requireOwnTopic(userId, topicId);
        return materials.findByTopicId(topicId);
    }

    /** FR-02 AC6: another student's material → 404. */
    @Transactional(readOnly = true)
    public Material get(String userId, UUID materialId) {
        return materials.findByIdAndUserId(materialId, userId)
                .orElseThrow(() -> new NotFoundException("Material " + materialId + " not found"));
    }

    private void requireOwnTopic(String userId, UUID topicId) {
        if (!topics.existsByIdAndUserId(topicId, userId)) {
            throw new NotFoundException("Topic " + topicId + " not found");
        }
    }
}
