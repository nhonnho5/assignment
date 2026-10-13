package com.studyanchor.tutor.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implements FR-07. Aggregate root: one conversation with the AI tutor about one question on one topic.
 * Every rule the agent's tools rely on lives here (AC3, AC7, AC8, AC10), so the LLM can be refused but never
 * bypass them. It registers {@link UnderstandingRecorded} for the service to publish after saving.
 */
@Entity
@Table(name = "tutoring_session")
public class TutoringSession {

    static final int MAX_MESSAGE = 2_000;
    static final int ATTEMPTS_FOR_SOLUTION = 2;
    public static final String END_REQUEST = "I'd like to end the session now.";

    @Id
    private UUID id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private UUID topicId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status;

    private boolean solutionRevealed;

    private Integer level;

    /** The transcript in order; the first message is the student's question. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tutoring_message", joinColumns = @JoinColumn(name = "session_id"))
    @OrderColumn(name = "position")
    private List<Message> messages = new ArrayList<>();

    @Transient
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    /** For JPA only. */
    protected TutoringSession() {
    }

    /** AC1, AC11: the question is the first student message. */
    public static TutoringSession start(String userId, UUID topicId, String question, Instant now) {
        TutoringSession s = new TutoringSession();
        s.id = UUID.randomUUID();
        s.userId = userId;
        s.topicId = topicId;
        s.status = SessionStatus.ACTIVE;
        s.addStudentMessage(question, now);
        return s;
    }

    /** AC10, AC11: 1–2,000 characters (trimmed); an ended session takes no more messages → 409. */
    public void addStudentMessage(String text, Instant now) {
        if (status == SessionStatus.ENDED) {
            throw new ConflictException("Tutoring session " + id + " has ended");
        }
        messages.add(new Message(true, Text.of(text, "Message", MAX_MESSAGE), false, now));
    }

    public void addTutorReply(String text, Instant now) {
        messages.add(new Message(false, text, false, now));
    }

    /** AC7: marks the student's latest message as an attempt — once per message, never the opening question. */
    public void recordAttempt() {
        requireStatus(SessionStatus.ACTIVE, "record an attempt");
        int last = messages.size() - 1;
        Message latest = messages.get(last);
        if (last == 0 || !latest.fromStudent() || latest.attempt()) {
            throw new DomainException("Only the student's newest reply after the question can count as an attempt,"
                    + " and only once");
        }
        messages.set(last, latest.asAttempt());
    }

    /** AC3, AC8: the solution needs at least 2 recorded attempts. */
    public void revealSolution() {
        requireStatus(SessionStatus.ACTIVE, "reveal the solution");
        if (attempts() < ATTEMPTS_FOR_SOLUTION) {
            throw new DomainException("The student has made " + attempts() + " attempt(s); "
                    + ATTEMPTS_FOR_SOLUTION + " are needed before the solution");
        }
        solutionRevealed = true;
    }

    /**
     * AC10: the student asks to finish; the tutor will ask a check-understanding question. Twice → 409.
     * The request is kept as a student message, so the transcript stays honest and keeps alternating.
     */
    public void end(Instant now) {
        if (status != SessionStatus.ACTIVE) {
            throw new ConflictException("Tutoring session " + id + " is already " + status);
        }
        status = SessionStatus.CHECKING;
        messages.add(new Message(true, END_REQUEST, false, now));
    }

    /** AC10: true right after {@link #end}, until the tutor has asked its check-understanding question. */
    public boolean checkQuestionDue() {
        return status == SessionStatus.CHECKING && messages.getLast().fromStudent()
                && messages.getLast().text().equals(END_REQUEST);
    }

    /**
     * AC5, AC10: level 1–5, only once, and only when the newest message is the student's answer to the
     * check-understanding question (never straight after the end request); ends the session.
     */
    public void recordUnderstanding(int level, Instant now) {
        requireStatus(SessionStatus.CHECKING, "record understanding");
        if (checkQuestionDue() || !messages.getLast().fromStudent()) {
            throw new DomainException("Ask one check-understanding question and wait for the student's answer"
                    + " before grading");
        }
        if (level < 1 || level > 5) {
            throw new DomainException("Understanding level must be 1–5, was " + level);
        }
        this.level = level;
        status = SessionStatus.ENDED;
        domainEvents.add(UnderstandingRecorded.of(this, now));
    }

    private void requireStatus(SessionStatus required, String action) {
        if (status != required) {
            throw new DomainException("Cannot " + action + " while the session is " + status);
        }
    }

    /** Returns the events registered since the last call and forgets them, so each is published once. */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    public int attempts() {
        return (int) messages.stream().filter(Message::attempt).count();
    }

    public String question() {
        return messages.getFirst().text();
    }

    public UUID id() {
        return id;
    }

    public String userId() {
        return userId;
    }

    public UUID topicId() {
        return topicId;
    }

    public SessionStatus status() {
        return status;
    }

    public boolean solutionRevealed() {
        return solutionRevealed;
    }

    public Integer level() {
        return level;
    }

    public List<Message> messages() {
        return List.copyOf(messages);
    }
}
