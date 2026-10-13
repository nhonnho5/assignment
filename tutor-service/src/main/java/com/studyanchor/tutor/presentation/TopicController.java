package com.studyanchor.tutor.presentation;

import com.studyanchor.tutor.domain.TopicReference;
import com.studyanchor.tutor.service.TopicCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Implements FR-01 AC10. Lists the topics Tutor knows for the student in {@code X-User-Id}. */
@RestController
class TopicController {

    record TopicResponse(UUID topicId, UUID courseId, String courseCode, String topicName) {

        static TopicResponse from(TopicReference t) {
            return new TopicResponse(t.topicId(), t.courseId(), t.courseCode(), t.topicName());
        }
    }

    private final TopicCatalogService catalog;

    TopicController(TopicCatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/topics")
    List<TopicResponse> topics(@RequestHeader("X-User-Id") String userId) {
        return catalog.topicsOf(userId).stream().map(TopicResponse::from).toList();
    }
}
