package com.studyanchor.focus.service;

import com.studyanchor.focus.domain.ConflictException;
import com.studyanchor.focus.domain.Distraction;
import com.studyanchor.focus.domain.EventPublisher;
import com.studyanchor.focus.domain.FocusSession;
import com.studyanchor.focus.domain.FocusSessionRepository;
import com.studyanchor.focus.domain.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Collection;
import java.util.UUID;

/**
 * Implements FR-05, FR-06. Application service for focus sessions: loads or creates the aggregate,
 * calls it, saves it, then publishes the events it registered.
 */
@Service
public class FocusSessionService {

    private final FocusSessionRepository repository;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    public FocusSessionService(FocusSessionRepository repository, EventPublisher eventPublisher, Clock clock) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /** FR-05 AC1, AC2, AC4: starts a session unless the user already has an active one. */
    @Transactional
    public FocusSession start(String userId, UUID timeBlockId, UUID topicId, Integer plannedMinutes,
                              Collection<String> allowedSites) {
        if (repository.hasActiveSession(userId)) {
            throw new ConflictException("User " + userId + " already has an active focus session");
        }
        FocusSession session = FocusSession.start(userId, timeBlockId, topicId, plannedMinutes, allowedSites,
                clock.instant());
        return saveAndPublish(session);
    }

    /** FR-05 AC3, AC4, AC6: ends the user's own session. */
    @Transactional
    public FocusSession end(String userId, UUID sessionId) {
        FocusSession session = get(userId, sessionId);
        session.end(clock.instant());
        return saveAndPublish(session);
    }

    /** FR-06 AC1–AC4: logs a distraction in the user's own active session (ended → 409, other user's → 404). */
    @Transactional
    public Distraction logDistraction(String userId, UUID sessionId, String type, String site, String note) {
        FocusSession session = get(userId, sessionId);
        Distraction distraction = session.logDistraction(type, site, note, clock.instant());
        saveAndPublish(session);
        return distraction;
    }

    @Transactional(readOnly = true)
    public FocusSession get(String userId, UUID sessionId) {
        return repository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new NotFoundException("Focus session " + sessionId + " not found"));
    }

    private FocusSession saveAndPublish(FocusSession session) {
        FocusSession saved = repository.save(session);
        session.pullDomainEvents().forEach(eventPublisher::publish);
        return saved;
    }
}
