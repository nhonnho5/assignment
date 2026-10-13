package com.studyanchor.planner.presentation;

import com.studyanchor.planner.presentation.PlannerDtos.ReminderResponse;
import com.studyanchor.planner.service.ReminderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Implements FR-12 AC6, AC10. REST endpoints for the student's reminders. */
@RestController
@RequestMapping("/reminders")
class ReminderController {

    private final ReminderService service;

    ReminderController(ReminderService service) {
        this.service = service;
    }

    /** Unread reminders, newest first. */
    @GetMapping
    List<ReminderResponse> unread(@RequestHeader("X-User-Id") String userId) {
        return service.unread(userId).stream().map(ReminderResponse::from).toList();
    }

    @PostMapping("/{reminderId}/read")
    ReminderResponse markRead(@RequestHeader("X-User-Id") String userId, @PathVariable UUID reminderId) {
        return ReminderResponse.from(service.markRead(userId, reminderId));
    }
}
