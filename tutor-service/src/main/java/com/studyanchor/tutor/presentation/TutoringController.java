package com.studyanchor.tutor.presentation;

import com.studyanchor.tutor.domain.Message;
import com.studyanchor.tutor.domain.SessionStatus;
import com.studyanchor.tutor.domain.TutoringSession;
import com.studyanchor.tutor.service.TutoringService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Implements FR-07 AC6. Talk to the Socratic tutor; delegates all decisions to the service. */
@RestController
@RequestMapping("/tutoring-sessions")
class TutoringController {

    record StartRequest(UUID topicId, String question) {
    }

    record MessageRequest(String text) {
    }

    record Line(String from, String text, boolean attempt, Instant sentAt) {

        static Line from(Message m) {
            return new Line(m.fromStudent() ? "student" : "tutor", m.text(), m.attempt(), m.sentAt());
        }
    }

    /** {@code reply}: the tutor's newest message, so a client does not have to read the whole transcript. */
    record SessionResponse(UUID sessionId, UUID topicId, SessionStatus status, int attempts, boolean solutionRevealed,
                           Integer understandingLevel, String reply, List<Line> transcript) {

        static SessionResponse from(TutoringSession s) {
            Message last = s.messages().getLast();
            return new SessionResponse(s.id(), s.topicId(), s.status(), s.attempts(), s.solutionRevealed(), s.level(),
                    last.fromStudent() ? null : last.text(), s.messages().stream().map(Line::from).toList());
        }
    }

    private final TutoringService service;

    TutoringController(TutoringService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    SessionResponse start(@RequestHeader("X-User-Id") String userId, @RequestBody StartRequest request) {
        return SessionResponse.from(service.start(userId, request.topicId(), request.question()));
    }

    @PostMapping("/{sessionId}/messages")
    SessionResponse send(@RequestHeader("X-User-Id") String userId, @PathVariable UUID sessionId,
                         @RequestBody MessageRequest request) {
        return SessionResponse.from(service.send(userId, sessionId, request.text()));
    }

    @PostMapping("/{sessionId}/end")
    SessionResponse end(@RequestHeader("X-User-Id") String userId, @PathVariable UUID sessionId) {
        return SessionResponse.from(service.end(userId, sessionId));
    }

    @GetMapping("/{sessionId}")
    SessionResponse get(@RequestHeader("X-User-Id") String userId, @PathVariable UUID sessionId) {
        return SessionResponse.from(service.get(userId, sessionId));
    }
}
