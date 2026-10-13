package com.studyanchor.planner.presentation;

import com.studyanchor.planner.presentation.PlannerDtos.CourseResponse;
import com.studyanchor.planner.presentation.PlannerDtos.CreateCourseRequest;
import com.studyanchor.planner.presentation.PlannerDtos.CreateDeadlineRequest;
import com.studyanchor.planner.presentation.PlannerDtos.CreateTopicRequest;
import com.studyanchor.planner.presentation.PlannerDtos.DeadlineResponse;
import com.studyanchor.planner.presentation.PlannerDtos.TopicResponse;
import com.studyanchor.planner.service.CourseService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Implements FR-01. REST endpoints for courses, topics and deadlines; delegates all decisions to the service. */
@RestController
@RequestMapping("/courses")
class CourseController {

    private final CourseService service;

    CourseController(CourseService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CourseResponse create(@RequestHeader("X-User-Id") String userId, @RequestBody CreateCourseRequest request) {
        return CourseResponse.from(service.create(userId, request.code(), request.name()));
    }

    @GetMapping
    List<CourseResponse> list(@RequestHeader("X-User-Id") String userId) {
        return service.list(userId).stream().map(CourseResponse::from).toList();
    }

    @GetMapping("/{courseId}")
    CourseResponse get(@RequestHeader("X-User-Id") String userId, @PathVariable UUID courseId) {
        return CourseResponse.from(service.get(userId, courseId));
    }

    @DeleteMapping("/{courseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@RequestHeader("X-User-Id") String userId, @PathVariable UUID courseId) {
        service.delete(userId, courseId);
    }

    @PostMapping("/{courseId}/topics")
    @ResponseStatus(HttpStatus.CREATED)
    TopicResponse addTopic(@RequestHeader("X-User-Id") String userId, @PathVariable UUID courseId,
                           @RequestBody CreateTopicRequest request) {
        return TopicResponse.from(service.addTopic(userId, courseId, request.name()));
    }

    @DeleteMapping("/{courseId}/topics/{topicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteTopic(@RequestHeader("X-User-Id") String userId, @PathVariable UUID courseId,
                     @PathVariable UUID topicId) {
        service.deleteTopic(userId, courseId, topicId);
    }

    @PostMapping("/{courseId}/deadlines")
    @ResponseStatus(HttpStatus.CREATED)
    DeadlineResponse addDeadline(@RequestHeader("X-User-Id") String userId, @PathVariable UUID courseId,
                                 @RequestBody CreateDeadlineRequest request) {
        return DeadlineResponse.from(service.addDeadline(userId, courseId, request.title(), request.dueAt(),
                request.weight(), request.topicIds()));
    }

    @DeleteMapping("/{courseId}/deadlines/{deadlineId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteDeadline(@RequestHeader("X-User-Id") String userId, @PathVariable UUID courseId,
                        @PathVariable UUID deadlineId) {
        service.deleteDeadline(userId, courseId, deadlineId);
    }
}
