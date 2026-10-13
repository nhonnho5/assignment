package com.studyanchor.focus.presentation;

import com.studyanchor.focus.domain.FocusSession;
import com.studyanchor.focus.service.FocusSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/** Implements FR-05, FR-06. REST endpoints for focus sessions and their distractions; delegates all decisions to the service. */
@RestController
@RequestMapping("/sessions")
class FocusSessionController {

    private final FocusSessionService service;

    FocusSessionController(FocusSessionService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<SessionResponse> start(@RequestHeader("X-User-Id") String userId,
                                          @RequestBody StartSessionRequest request) {
        FocusSession session = service.start(userId, request.timeBlockId(), request.topicId(),
                request.plannedMinutes(), request.allowedSites());
        return ResponseEntity.created(URI.create("/sessions/" + session.id())).body(SessionResponse.from(session));
    }

    @PostMapping("/{sessionId}/end")
    SessionResponse end(@RequestHeader("X-User-Id") String userId, @PathVariable UUID sessionId) {
        return SessionResponse.from(service.end(userId, sessionId));
    }

    /** FR-06: log a distraction in an active session. */
    @PostMapping("/{sessionId}/distractions")
    @ResponseStatus(HttpStatus.CREATED)
    DistractionResponse logDistraction(@RequestHeader("X-User-Id") String userId, @PathVariable UUID sessionId,
                                       @RequestBody LogDistractionRequest request) {
        return DistractionResponse.from(
                service.logDistraction(userId, sessionId, request.type(), request.site(), request.note()));
    }

    @GetMapping("/{sessionId}")
    SessionResponse get(@RequestHeader("X-User-Id") String userId, @PathVariable UUID sessionId) {
        return SessionResponse.from(service.get(userId, sessionId));
    }
}
