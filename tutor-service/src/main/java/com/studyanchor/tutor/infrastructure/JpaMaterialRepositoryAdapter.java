package com.studyanchor.tutor.infrastructure;

import com.studyanchor.tutor.domain.Material;
import com.studyanchor.tutor.domain.MaterialRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Implements FR-02. Adapter: fulfils the domain repository interface using Spring Data JPA and H2. */
@Repository
class JpaMaterialRepositoryAdapter implements MaterialRepository {

    private final SpringDataMaterialRepository jpa;

    JpaMaterialRepositoryAdapter(SpringDataMaterialRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Material save(Material material) {
        return jpa.save(material);
    }

    @Override
    public Optional<Material> findByIdAndUserId(UUID id, String userId) {
        return jpa.findByIdAndUserId(id, userId);
    }

    @Override
    public List<Material> findByTopicId(UUID topicId) {
        return jpa.findByTopicIdOrderByAddedAt(topicId);
    }

    @Override
    public void deleteByTopicId(UUID topicId) {
        jpa.deleteByTopicId(topicId);
    }
}
