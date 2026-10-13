package com.studyanchor.tutor.service;

import com.studyanchor.tutor.domain.DomainException;
import com.studyanchor.tutor.domain.EventPublisher;
import com.studyanchor.tutor.domain.NotFoundException;
import com.studyanchor.tutor.domain.TopicReference;
import com.studyanchor.tutor.domain.TopicReferenceRepository;
import com.studyanchor.tutor.domain.TutorAgent;
import com.studyanchor.tutor.domain.TutoringSession;
import com.studyanchor.tutor.domain.TutoringSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

/**
 * Implements FR-07. Application service for tutoring: loads the session, lets the aggregate check the request,
 * asks the agent for a reply, saves, then publishes UnderstandingRecorded. If the agent fails, the exception rolls
 * the whole request back, so nothing is saved and the student can resend (AC12).
 */
@Service
public class TutoringService {

    private final TutoringSessionRepository sessions;
    private final TopicReferenceRepository topics;
    private final TutorAgent agent;
    private final EventPublisher eventPublisher;
    private final Clock clock;

    public TutoringService(TutoringSessionRepository sessions, TopicReferenceRepository topics, TutorAgent agent,
                           EventPublisher eventPublisher, Clock clock) {
        this.sessions = sessions;
        this.topics = topics;
        this.agent = agent;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /** AC1, AC2, AC11: no topic → 400; an unknown topic or another student's topic → 404. */
    @Transactional
    public TutoringSession start(String userId, UUID topicId, String question) {
        if (topicId == null) {
            throw new DomainException("topicId is required");
        }
        TopicReference topic = ownTopic(userId, topicId);
        TutoringSession session = TutoringSession.start(userId, topicId, question, clock.instant());
        return reply(session, agent.answer(session, topic));
    }

    /** AC1, AC10: a message to an ended session → 409. */
    @Transactional
    public TutoringSession send(String userId, UUID sessionId, String text) {
        TutoringSession session = get(userId, sessionId);
        session.addStudentMessage(text, clock.instant());
        return reply(session, agent.answer(session, ownTopic(userId, session.topicId())));
    }

    /** AC10: the tutor replies with a check-understanding question; ending twice → 409. */
    @Transactional
    public TutoringSession end(String userId, UUID sessionId) {
        TutoringSession session = get(userId, sessionId);
        session.end(clock.instant());
        return reply(session, agent.answer(session, ownTopic(userId, session.topicId())));
    }

    /** AC6, AC11: another student's session → 404. */
    @Transactional(readOnly = true)
    public TutoringSession get(String userId, UUID sessionId) {
        return sessions.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new NotFoundException("Tutoring session " + sessionId + " not found"));
    }

    private TutoringSession reply(TutoringSession session, String text) {
        session.addTutorReply(text, clock.instant());
        sessions.save(session);
        session.pullDomainEvents().forEach(eventPublisher::publish);
        return session;
    }

    private TopicReference ownTopic(String userId, UUID topicId) {
        return topics.findByIdAndUserId(topicId, userId)
                .orElseThrow(() -> new NotFoundException("Topic " + topicId + " not found"));
    }
}
