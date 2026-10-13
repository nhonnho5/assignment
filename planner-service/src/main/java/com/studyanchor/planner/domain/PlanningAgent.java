package com.studyanchor.planner.domain;

import java.util.List;

/**
 * Implements FR-04. Port: the planning agent, implemented in infrastructure with an LLM. While it works, its tools
 * add blocks to {@code plan} only through {@link StudyPlan#addBlock}, which rejects any block that breaks AC3.
 * Throws {@link AgentUnavailableException} when the LLM cannot be reached (AC13).
 */
public interface PlanningAgent {

    /** Fills {@code plan} with time blocks and returns the agent's closing summary for the student. */
    String plan(StudyPlan plan, List<Course> courses, WeeklyAvailability availability);
}
