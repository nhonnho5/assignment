package com.studyanchor.tutor.infrastructure;

import com.studyanchor.tutor.domain.TopicReference;
import com.studyanchor.tutor.domain.TopicReferenceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/** Implements FR-01 AC10. Adapter: fulfils the domain repository interface using Spring Data JPA and H2. */
@Repository
class JpaTopicReferenceRepositoryAdapter implements TopicReferenceRepository {

    private final SpringDataTopicReferenceRepository jpa;

    JpaTopicReferenceRepositoryAdapter(SpringDataTopicReferenceRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public TopicReference save(TopicReference topic) {
        return jpa.save(topic);
    }

    @Override
    public boolean existsById(UUID topicId) {
        return jpa.existsById(topicId);
    }

    @Override
    public List<TopicReference> findByUserId(String userId) {
        return jpa.findByUserIdOrderByCourseCodeAscTopicNameAsc(userId);
    }

    /** Spring Data 3 ignores an unknown ID here, which is what FR-01 AC10 needs. */
    @Override
    public void deleteById(UUID topicId) {
        jpa.deleteById(topicId);
    }
}
