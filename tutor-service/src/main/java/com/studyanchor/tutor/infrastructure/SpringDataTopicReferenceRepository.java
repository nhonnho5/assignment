package com.studyanchor.tutor.infrastructure;

import com.studyanchor.tutor.domain.TopicReference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Spring Data generates the SQL for these methods from their names. */
interface SpringDataTopicReferenceRepository extends JpaRepository<TopicReference, UUID> {

    boolean existsByTopicIdAndUserId(UUID topicId, String userId);

    List<TopicReference> findByUserIdOrderByCourseCodeAscTopicNameAsc(String userId);
}
