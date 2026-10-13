package com.studyanchor.planner.infrastructure;

import com.studyanchor.planner.service.CourseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

/** Implements FR-02 AC8. Reads {@code tutor.materials}, logs each event and lets the course count the material. */
@Configuration
class TutorMaterialsConsumer {

    private static final Logger log = LoggerFactory.getLogger(TutorMaterialsConsumer.class);

    @Bean
    Consumer<MaterialAddedEvent> tutorMaterials(CourseService courses) {
        return event -> {
            log.info("Received {} for user {}: material={} topic={} title={}", event.eventType(), event.userId(),
                    event.materialId(), event.topicId(), event.title());
            courses.onMaterialAdded(event.topicId(), event.materialId());
        };
    }
}
