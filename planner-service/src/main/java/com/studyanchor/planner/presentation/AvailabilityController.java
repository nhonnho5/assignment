package com.studyanchor.planner.presentation;

import com.studyanchor.planner.presentation.PlannerDtos.AddSlotRequest;
import com.studyanchor.planner.presentation.PlannerDtos.AvailabilityResponse;
import com.studyanchor.planner.presentation.PlannerDtos.SlotResponse;
import com.studyanchor.planner.service.AvailabilityService;
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

import java.util.UUID;

/** Implements FR-03. REST endpoints for weekly availability; delegates all decisions to the service. */
@RestController
@RequestMapping("/availability")
class AvailabilityController {

    private final AvailabilityService service;

    AvailabilityController(AvailabilityService service) {
        this.service = service;
    }

    @PostMapping("/slots")
    @ResponseStatus(HttpStatus.CREATED)
    SlotResponse addSlot(@RequestHeader("X-User-Id") String userId, @RequestBody AddSlotRequest request) {
        return SlotResponse.from(service.addSlot(userId, request.day(), request.start(), request.end()));
    }

    @GetMapping
    AvailabilityResponse get(@RequestHeader("X-User-Id") String userId) {
        return AvailabilityResponse.from(userId, service.get(userId));
    }

    @DeleteMapping("/slots/{slotId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeSlot(@RequestHeader("X-User-Id") String userId, @PathVariable UUID slotId) {
        service.removeSlot(userId, slotId);
    }
}
