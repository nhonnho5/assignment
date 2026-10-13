package com.studyanchor.tutor.service;

import com.studyanchor.tutor.domain.MaterialRepository;
import com.studyanchor.tutor.domain.TopicReference;
import com.studyanchor.tutor.domain.TopicReferenceRepository;
import com.studyanchor.tutor.domain.TutoringSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implements FR-01 AC10, FR-02 AC7, FR-07 AC13. Keeps Tutor's copy of Planner topics in step with
 * {@code planner.topics}.
 * Idempotent, because the whole topic is replayed from the start every time Tutor starts.
 */
@Service
public class TopicCatalogService {

    private final TopicReferenceRepository repository;
    private final MaterialRepository materials;
    private final TutoringSessionRepository sessions;

    public TopicCatalogService(TopicReferenceRepository repository, MaterialRepository materials,
                               TutoringSessionRepository sessions) {
        this.repository = repository;
        this.materials = materials;
        this.sessions = sessions;
    }

    /** A repeated TopicCreated is ignored, so the topic is stored once. */
    @Transactional
    public void onTopicCreated(TopicReference topic) {
        if (!repository.existsById(topic.topicId())) {
            repository.save(topic);
        }
    }

    /**
     * A TopicDeleted for an unknown topic is ignored. The topic's materials (FR-02 AC7) and tutoring sessions
     * (FR-07 AC13) go with it.
     */
    @Transactional
    public void onTopicDeleted(UUID topicId) {
        materials.deleteByTopicId(topicId);
        sessions.deleteByTopicId(topicId);
        repository.deleteById(topicId);
    }

    @Transactional(readOnly = true)
    public List<TopicReference> topicsOf(String userId) {
        return repository.findByUserId(userId);
    }
}
