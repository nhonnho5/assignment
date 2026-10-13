package com.studyanchor.planner.presentation;

import com.studyanchor.planner.presentation.PlannerDtos.CreateGoalRequest;
import com.studyanchor.planner.presentation.PlannerDtos.CreateValueRequest;
import com.studyanchor.planner.presentation.PlannerDtos.GoalResponse;
import com.studyanchor.planner.presentation.PlannerDtos.ValueResponse;
import com.studyanchor.planner.service.MotivationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Implements FR-11. REST endpoints for goals ({@code /goals}) and value statements ({@code /values}). */
@RestController
class MotivationController {

    private final MotivationService service;

    MotivationController(MotivationService service) {
        this.service = service;
    }

    @PostMapping("/goals")
    @ResponseStatus(HttpStatus.CREATED)
    GoalResponse createGoal(@RequestHeader("X-User-Id") String userId, @RequestBody CreateGoalRequest request) {
        return GoalResponse.from(service.createGoal(userId, request.title(), request.why(), request.targetDate()));
    }

    @GetMapping("/goals")
    List<GoalResponse> goals(@RequestHeader("X-User-Id") String userId) {
        return service.goals(userId).stream().map(GoalResponse::from).toList();
    }

    @PostMapping("/goals/{goalId}/achieve")
    GoalResponse achieve(@RequestHeader("X-User-Id") String userId, @PathVariable UUID goalId) {
        return GoalResponse.from(service.achieve(userId, goalId));
    }

    @DeleteMapping("/goals/{goalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteGoal(@RequestHeader("X-User-Id") String userId, @PathVariable UUID goalId) {
        service.deleteGoal(userId, goalId);
    }

    @PostMapping("/values")
    @ResponseStatus(HttpStatus.CREATED)
    ValueResponse createValue(@RequestHeader("X-User-Id") String userId, @RequestBody CreateValueRequest request) {
        return ValueResponse.from(service.createValue(userId, request.text()));
    }

    @GetMapping("/values")
    List<ValueResponse> values(@RequestHeader("X-User-Id") String userId) {
        return service.values(userId).stream().map(ValueResponse::from).toList();
    }

    @DeleteMapping("/values/{valueId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteValue(@RequestHeader("X-User-Id") String userId, @PathVariable UUID valueId) {
        service.deleteValue(userId, valueId);
    }
}
