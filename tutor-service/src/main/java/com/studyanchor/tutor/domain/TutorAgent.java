package com.studyanchor.tutor.domain;

/**
 * Implements FR-07. Port: the Socratic tutor agent, implemented in infrastructure with an LLM. While it answers,
 * its tools may change the session through the aggregate's own methods (attempts, solution, understanding).
 * Throws {@link TutorUnavailableException} when the LLM cannot be reached (AC12).
 */
public interface TutorAgent {

    /**
     * Replies to the student's newest message, which is the last one in the session. Right after the student
     * ended the session, the reply is the check-understanding question (AC10).
     */
    String answer(TutoringSession session, TopicReference topic);
}
