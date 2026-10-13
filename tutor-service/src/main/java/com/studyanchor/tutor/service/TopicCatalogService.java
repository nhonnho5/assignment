package com.studyanchor.tutor.service;

import com.studyanchor.tutor.domain.MaterialRepository;
import com.studyanchor.tutor.domain.TopicReference;
import com.studyanchor.tutor.domain.TopicReferenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implements FR-01 AC10, FR-02 AC7. Keeps Tutor's copy of Planner topics in step with {@code planner.topics}.
 * Idempotent, because the whole topic is replayed from the start every time Tutor starts.
 */
@Service
public class TopicCatalogService {

    private final TopicReferenceRepository repository;
    private final MaterialRepository materials;

    public TopicCatalogService(TopicReferenceRepository repository, MaterialRepository materials) {
        this.repository = repository;
        this.materials = materials;
    }

    /** A repeated TopicCreated is ignored, so the topic is stored once. */
    @Transactional
    public void onTopicCreated(TopicReference topic) {
        if (!repository.existsById(topic.topicId())) {
            repository.save(topic);
        }
    }

    /** A TopicDeleted for an unknown topic is ignored. The topic's materials go with it (FR-02 AC7). */
    @Transactional
    public void onTopicDeleted(UUID topicId) {
        materials.deleteByTopicId(topicId);
        repository.deleteById(topicId);
    }

    @Transactional(readOnly = true)
    public List<TopicReference> topicsOf(String userId) {
        return repository.findByUserId(userId);
    }
}
