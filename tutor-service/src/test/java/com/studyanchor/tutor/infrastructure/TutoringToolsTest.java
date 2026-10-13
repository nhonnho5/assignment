package com.studyanchor.tutor.infrastructure;

import com.studyanchor.tutor.domain.Material;
import com.studyanchor.tutor.domain.SessionStatus;
import com.studyanchor.tutor.domain.TutoringSession;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** FR-07 agent tool rules, called directly as the LLM would call them — without the LLM (CLAUDE.md). */
class TutoringToolsTest {

    private static final Instant NOW = Instant.parse("2026-10-14T03:00:00Z");

    private final UUID topicId = UUID.randomUUID();
    private final TutoringSession session = TutoringSession.start("alice", topicId, "What is a Kafka partition?", NOW);
    private final List<String> solverCalls = new ArrayList<>();
    private final TutoringTools.Solver solver = (question, material) -> {
        solverCalls.add(question + " | " + material);
        return "A partition is an ordered, append-only log.";
    };
    private final Material notes = Material.create("alice", topicId, "notes.md", null,
            "A topic is split into partitions.\n\nEach partition is an ordered log.", NOW);

    private TutoringTools tools(List<Material> materials) {
        return new TutoringTools(session, materials, solver, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private void studentTries(TutoringTools tools, String text) {
        session.addTutorReply("Hint", NOW);
        session.addStudentMessage(text, NOW);
        assertThat(tools.recordAttempt()).startsWith("Attempt recorded");
    }

    @Test
    void FR07_revealSolutionNeverCallsSolverBeforeTwoAttempts() {
        TutoringTools tools = tools(List.of(notes));

        assertThat(tools.revealSolution()).startsWith("REFUSED").contains("0 attempt");
        studentTries(tools, "A log?");
        assertThat(tools.revealSolution()).startsWith("REFUSED").contains("1 attempt");

        assertThat(solverCalls).isEmpty();
        assertThat(session.solutionRevealed()).isFalse();
    }

    @Test
    void FR07_revealSolutionCallsSolverAfterTwoAttempts() {
        TutoringTools tools = tools(List.of(notes));
        studentTries(tools, "A log?");
        studentTries(tools, "An ordered log inside a topic?");

        assertThat(tools.revealSolution()).contains("A partition is an ordered, append-only log.");

        assertThat(solverCalls).singleElement().satisfies(call -> assertThat(call)
                .startsWith("What is a Kafka partition?").contains("Each partition is an ordered log."));
        assertThat(session.solutionRevealed()).isTrue();
    }

    @Test
    void FR07_attemptForOpeningQuestionIsRefused() {
        assertThat(tools(List.of()).recordAttempt()).startsWith("REFUSED");
        assertThat(session.attempts()).isZero();
    }

    @Test
    void FR07_searchMaterialSaysWhenTopicHasNoMaterial() {
        assertThat(tools(List.of()).searchMaterial("partition")).contains("no study material");
        assertThat(tools(List.of(notes)).searchMaterial("photosynthesis")).contains("Nothing in the material");
        assertThat(tools(List.of(notes)).searchMaterial("partitions")).contains("From \"notes\":")
                .contains("Each partition is an ordered log.");
    }

    @Test
    void FR07_outOfRangeLevelIsRefused() {
        TutoringTools tools = tools(List.of());
        assertThat(tools.recordUnderstanding(3)).as("not checking yet").startsWith("REFUSED");
        session.end(NOW);
        session.addTutorReply("Check: why does the key matter?", NOW);
        session.addStudentMessage("It picks the partition", NOW);

        assertThat(tools.recordUnderstanding(7)).startsWith("REFUSED").contains("1–5");
        assertThat(tools.recordUnderstanding(4)).startsWith("Level 4 recorded");
        assertThat(session.status()).isEqualTo(SessionStatus.ENDED);
    }
}
