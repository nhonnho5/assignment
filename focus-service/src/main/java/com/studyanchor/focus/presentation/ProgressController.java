package com.studyanchor.focus.presentation;

import com.studyanchor.focus.service.FocusProgressService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Implements FR-10. {@code GET /progress/{userId}}. The user comes from the path, not X-User-Id,
 * because the Planner service calls it on the student's behalf (FR-10 AC8).
 */
@RestController
class ProgressController {

    private final FocusProgressService service;

    ProgressController(FocusProgressService service) {
        this.service = service;
    }

    @GetMapping("/progress/{userId}")
    ProgressResponse get(@PathVariable String userId) {
        return ProgressResponse.from(userId, service.progressOf(userId));
    }
}
