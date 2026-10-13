package com.studyanchor.tutor.infrastructure;

import com.studyanchor.tutor.domain.DomainException;
import com.studyanchor.tutor.domain.Material;
import com.studyanchor.tutor.domain.MaterialSearch;
import com.studyanchor.tutor.domain.MaterialSearch.Excerpt;
import com.studyanchor.tutor.domain.TutoringSession;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Implements FR-07 AC3, AC4, AC5, AC7–10. The tutor agent's tools for one session and one turn. Each tool only
 * calls the {@link TutoringSession} aggregate, which holds the rules; a broken rule becomes a "REFUSED: …" answer
 * the LLM reads, so the agent is told no instead of the request failing.
 */
class TutoringTools {

    private static final Logger log = LoggerFactory.getLogger(TutoringTools.class);

    /** AC8: a separate LLM call that writes the full solution; reachable only through {@link #revealSolution}. */
    interface Solver {

        @SystemMessage("You write clear, complete worked solutions for university students. Use the course"
                + " material when it is relevant. Keep it under 250 words.")
        @UserMessage("Question: {{question}}\n\nCourse material:\n{{material}}")
        String solve(@V("question") String question, @V("material") String material);
    }

    private final TutoringSession session;
    private final List<Material> materials;
    private final Solver solver;
    private final Clock clock;

    TutoringTools(TutoringSession session, List<Material> materials, Solver solver, Clock clock) {
        this.session = session;
        this.materials = materials;
        this.solver = solver;
        this.clock = clock;
    }

    @Tool("Searches the student's own study material for this topic and returns the best matching paragraphs."
            + " Use it before giving a hint, so your help matches their course.")
    String searchMaterial(@P("a few key words to look for") String query) {
        if (materials.isEmpty()) {
            return "This topic has no study material yet; help from general knowledge.";
        }
        List<Excerpt> found = MaterialSearch.search(materials, query);
        log.info("Tool searchMaterial(\"{}\") in session {} → {} paragraph(s)", query, session.id(), found.size());
        return found.isEmpty() ? "Nothing in the material matches \"" + query + "\"." : quote(found);
    }

    @Tool("Records the student's newest message as a real attempt at answering. Call it once, only when the student"
            + " actually tried to answer (not for 'I don't know' or a request for the answer).")
    String recordAttempt() {
        return refusedOr("recordAttempt", () -> {
            session.recordAttempt();
            return "Attempt recorded. The student has made " + session.attempts() + " attempt(s).";
        });
    }

    @Tool("Returns the full worked solution to the student's question. It is refused until the student has made"
            + " 2 recorded attempts; when refused, give another hint instead.")
    String revealSolution() {
        return refusedOr("revealSolution", () -> {
            session.revealSolution();
            String material = quote(MaterialSearch.search(materials, session.question()));
            return "Solution to share with the student:\n" + solver.solve(session.question(), material);
        });
    }

    @Tool("Records how well the student understands the topic after grading their answer to your"
            + " check-understanding question: 1 = not at all, 3 = partly, 5 = can explain it in their own words."
            + " Ends the session.")
    String recordUnderstanding(@P("understanding level from 1 to 5") int level) {
        return refusedOr("recordUnderstanding(" + level + ")", () -> {
            session.recordUnderstanding(level, clock.instant());
            return "Level " + level + " recorded; the session has ended. Tell the student their level and one"
                    + " thing to review.";
        });
    }

    static String quote(List<Excerpt> excerpts) {
        return excerpts.stream().map(e -> "From \"" + e.materialTitle() + "\": " + e.text())
                .collect(Collectors.joining("\n\n"));
    }

    /** Runs the tool; a broken domain rule becomes "REFUSED: …" for the LLM. Logs the outcome as demo evidence. */
    private String refusedOr(String tool, Supplier<String> action) {
        String result;
        try {
            result = action.get();
        } catch (DomainException e) {
            result = "REFUSED: " + e.getMessage();
        }
        log.info("Tool {} in session {} → {}", tool, session.id(), result.lines().findFirst().orElse(""));
        return result;
    }
}
