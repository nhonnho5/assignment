package com.studyanchor.tutor.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Rules of the {@link TutoringSession} aggregate (FR-07). Pure Java: no Spring, no LLM. */
class TutoringSessionTest {

    private static final Instant NOW = Instant.parse("2026-10-14T03:00:00Z");

    private final TutoringSession session = TutoringSession.start("alice", UUID.randomUUID(), "  What is a Kafka partition? ", NOW);

    /** The student tries an answer and the tutor records it as an attempt. */
    private void attempt(String text) {
        session.addTutorReply("Hint", NOW);
        session.addStudentMessage(text, NOW);
        session.recordAttempt();
    }

    /** The student ends, the tutor asks its check-understanding question and the student answers it. */
    private void checkAnswered() {
        session.end(NOW);
        session.addTutorReply("Why does the key matter?", NOW);
        session.addStudentMessage("It picks the partition, so order is kept per key", NOW);
    }

    @Test
    void FR07_messageMustBe1To2000Characters() {
        assertThat(session.question()).isEqualTo("What is a Kafka partition?");
        assertThatThrownBy(() -> TutoringSession.start("alice", UUID.randomUUID(), " ", NOW))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> session.addStudentMessage("x".repeat(2001), NOW))
                .isInstanceOf(DomainException.class).hasMessageContaining("2000");
        session.addStudentMessage("x".repeat(2000), NOW);
    }

    @Test
    void FR07_noAttemptForOpeningQuestion() {
        assertThatThrownBy(session::recordAttempt).isInstanceOf(DomainException.class);
        assertThat(session.attempts()).isZero();
    }

    @Test
    void FR07_atMostOneAttemptPerStudentMessage() {
        attempt("A log split for parallelism?");

        assertThatThrownBy(session::recordAttempt).isInstanceOf(DomainException.class);
        session.addTutorReply("Close. What decides which partition a record goes to?", NOW);
        assertThatThrownBy(session::recordAttempt).as("the latest message is the tutor's").isInstanceOf(DomainException.class);
        assertThat(session.attempts()).isEqualTo(1);
    }

    @Test
    void FR07_cannotRevealSolutionBeforeTwoAttempts() {
        assertThatThrownBy(session::revealSolution).isInstanceOf(DomainException.class).hasMessageContaining("0");
        attempt("A log?");
        assertThatThrownBy(session::revealSolution).isInstanceOf(DomainException.class).hasMessageContaining("1");
        assertThat(session.solutionRevealed()).isFalse();
    }

    @Test
    void FR07_revealSolutionAfterTwoAttempts() {
        attempt("A log?");
        attempt("An ordered log inside a topic?");

        session.revealSolution();

        assertThat(session.solutionRevealed()).isTrue();
    }

    @Test
    void FR07_endMovesActiveToChecking() {
        session.end(NOW);

        assertThat(session.status()).isEqualTo(SessionStatus.CHECKING);
        assertThat(session.messages().getLast()).as("the transcript shows the student ended it")
                .isEqualTo(new Message(true, TutoringSession.END_REQUEST, false, NOW));
        assertThat(session.checkQuestionDue()).isTrue();
        session.addTutorReply("Why does the key matter?", NOW);
        assertThat(session.checkQuestionDue()).isFalse();
        assertThatThrownBy(session::recordAttempt).as("no attempts while checking").isInstanceOf(DomainException.class);
        assertThatThrownBy(session::revealSolution).as("no solution while checking").isInstanceOf(DomainException.class);
    }

    @Test
    void FR07_cannotEndTwice() {
        session.end(NOW);

        assertThatThrownBy(() -> session.end(NOW)).isInstanceOf(ConflictException.class);
    }

    @Test
    void FR07_levelMustBe1To5() {
        checkAnswered();

        assertThatThrownBy(() -> session.recordUnderstanding(0, NOW)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> session.recordUnderstanding(6, NOW)).isInstanceOf(DomainException.class);
        assertThat(session.status()).isEqualTo(SessionStatus.CHECKING);
    }

    @Test
    void FR07_understandingOnlyWhenChecking() {
        assertThatThrownBy(() -> session.recordUnderstanding(3, NOW)).as("still active").isInstanceOf(DomainException.class);
        checkAnswered();
        session.recordUnderstanding(3, NOW);
        assertThatThrownBy(() -> session.recordUnderstanding(4, NOW)).as("only once").isInstanceOf(DomainException.class);
        assertThat(session.level()).isEqualTo(3);
    }

    /** Found in the curl test: the LLM graded straight after /end, skipping the check question. */
    @Test
    void FR07_understandingOnlyAfterStudentAnsweredCheckQuestion() {
        session.end(NOW);
        assertThatThrownBy(() -> session.recordUnderstanding(5, NOW)).as("check question not asked yet")
                .isInstanceOf(DomainException.class).hasMessageContaining("check-understanding question");
        session.addTutorReply("Why does the key matter?", NOW);
        assertThatThrownBy(() -> session.recordUnderstanding(5, NOW)).as("student has not answered yet")
                .isInstanceOf(DomainException.class);

        session.addStudentMessage("It picks the partition", NOW);
        session.recordUnderstanding(3, NOW);

        assertThat(session.level()).isEqualTo(3);
    }

    @Test
    void FR07_understandingEndsSessionAndRegistersEvent() {
        checkAnswered();
        session.recordUnderstanding(4, NOW);

        assertThat(session.status()).isEqualTo(SessionStatus.ENDED);
        assertThat(session.pullDomainEvents()).singleElement().isInstanceOfSatisfying(UnderstandingRecorded.class, e -> {
            assertThat(e.tutoringSessionId()).isEqualTo(session.id());
            assertThat(e.topicId()).isEqualTo(session.topicId());
            assertThat(e.level()).isEqualTo(4);
            assertThat(e.userId()).isEqualTo("alice");
        });
        assertThat(session.pullDomainEvents()).as("published once").isEmpty();
    }

    @Test
    void FR07_cannotMessageEndedSession() {
        checkAnswered();
        session.recordUnderstanding(4, NOW);

        assertThatThrownBy(() -> session.addStudentMessage("one more thing", NOW)).isInstanceOf(ConflictException.class);
    }
}
