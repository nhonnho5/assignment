package com.studyanchor.tutor.infrastructure;

import com.studyanchor.tutor.domain.Material;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data generates the SQL for these methods from their names. */
interface SpringDataMaterialRepository extends JpaRepository<Material, UUID> {

    Optional<Material> findByIdAndUserId(UUID id, String userId);

    List<Material> findByTopicIdOrderByAddedAt(UUID topicId);

    void deleteByTopicId(UUID topicId);
}
