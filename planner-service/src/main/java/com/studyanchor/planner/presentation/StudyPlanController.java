package com.studyanchor.planner.presentation;

import com.studyanchor.planner.presentation.PlannerDtos.GeneratePlanRequest;
import com.studyanchor.planner.presentation.PlannerDtos.PlanResponse;
import com.studyanchor.planner.service.StudyPlanService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Implements FR-04 AC1, AC8. REST endpoints for AI-generated weekly study plans. */
@RestController
@RequestMapping("/plans")
class StudyPlanController {

    private final StudyPlanService service;

    StudyPlanController(StudyPlanService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PlanResponse generate(@RequestHeader("X-User-Id") String userId, @RequestBody GeneratePlanRequest request) {
        return PlanResponse.from(service.generate(userId, request.weekStart(), request.replace()));
    }

    @GetMapping("/{weekStart}")
    PlanResponse get(@RequestHeader("X-User-Id") String userId, @PathVariable LocalDate weekStart) {
        return PlanResponse.from(service.get(userId, weekStart));
    }
}
