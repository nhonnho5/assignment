package com.studyanchor.planner.infrastructure;

import com.studyanchor.planner.domain.AgentUnavailableException;
import com.studyanchor.planner.domain.Course;
import com.studyanchor.planner.domain.PlanningAgent;
import com.studyanchor.planner.domain.StudyCalendar;
import com.studyanchor.planner.domain.StudyPlan;
import com.studyanchor.planner.domain.WeeklyAvailability;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.tool.ToolArgumentsErrorHandler;
import dev.langchain4j.service.tool.ToolExecutionErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Implements FR-04. Adapter for the {@link PlanningAgent} port with LangChain4j and Google Gemini: the LLM reads the
 * student's data and adds blocks through {@link PlanningTools}. Without {@code LLM_API_KEY} the service still
 * starts; planning then answers 503 (AC13).
 */
@Component
class GeminiPlanningAgent implements PlanningAgent {

    private static final Logger log = LoggerFactory.getLogger(GeminiPlanningAgent.class);
    private static final int MEMORY_MESSAGES = 100;

    /** LangChain4j turns this interface into the agent: it sends the message, runs tool calls, returns the text. */
    interface Planner {
        String chat(String message);
    }

    private final ChatModel model;
    private final PlanningTools.FocusStats focus;
    private final Clock clock;

    GeminiPlanningAgent(@Value("${llm.api-key}") String apiKey, @Value("${llm.model}") String modelName,
                        PlanningTools.FocusStats focus, Clock clock) {
        // Gemini 3 signs each tool call ("thought signature") and rejects the next request unless it is sent back
        this.model = apiKey.isBlank() ? null : GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey).modelName(modelName).timeout(Duration.ofSeconds(90))
                .returnThinking(true).sendThinking(true).build();
        this.focus = focus;
        this.clock = clock;
        log.info("Planning agent: model {} ({})", modelName, model == null ? "no LLM_API_KEY, planning disabled" : "ready");
    }

    @Override
    public String plan(StudyPlan plan, List<Course> courses, WeeklyAvailability availability) {
        if (model == null) {
            throw new AgentUnavailableException("The planning agent is not configured: LLM_API_KEY is not set", null);
        }
        var memory = MessageWindowChatMemory.withMaxMessages(MEMORY_MESSAGES);
        memory.add(SystemMessage.from(instructions(plan)));
        Planner planner = AiServices.builder(Planner.class)
                .chatModel(model)
                .chatMemory(memory)
                .tools(new PlanningTools(plan, courses, availability, focus, clock))
                .toolArgumentsErrorHandler(ToolArgumentsErrorHandler.sendExceptionMessageToLlm())
                .toolExecutionErrorHandler(ToolExecutionErrorHandler.failInvocation())
                .build();
        String reply;
        try {
            reply = planner.chat("Please plan my study week starting Monday " + plan.weekStart() + ".");
        } catch (RuntimeException e) {
            log.warn("Planning LLM call failed for plan {}", plan.id(), e);
            throw new AgentUnavailableException("The planning agent is unavailable right now: " + e.getMessage(), e);
        }
        if (reply == null || reply.isBlank()) {
            throw new AgentUnavailableException("The planning agent returned an empty reply; please try again", null);
        }
        return reply;
    }

    /** The prompt side of AC2–AC7; the block rules are enforced again in code by {@link StudyPlan#addBlock}. */
    private String instructions(StudyPlan plan) {
        return """
                You are a study planner for a university student. Many students using this app have ADHD, so plan
                realistic, focused blocks and keep your messages short and warm.
                Plan the week from Monday %s to Sunday %s. It is now %s. All times are Australia/Sydney.
                Steps:
                1. Call getDeadlines, getAvailability, getTopics and getFocusStats (you may call them together).
                2. Call addBlocks once with all the blocks. Code checks every block and refuses any that breaks a rule:
                   - inside ONE availability slot, starting after now;
                   - 25 to 90 minutes long;
                   - at least 10 minutes from any other block;
                   - a one-sentence rationale naming why (deadline, weight, understanding level).
                   Give the higher-ranked topics from getTopics more and earlier blocks. If recent focus is low or
                   unknown, prefer 25–45 minute blocks.
                3. If blocks are REFUSED, fix them and call addBlocks again with only the fixed ones.
                4. If the free time is not enough for the topics with deadlines, still use the time there is for the
                   highest-ranked topics, then call reportInsufficientTime with a short explanation. Never overbook.
                5. Finish with a short, encouraging summary (under 80 words) of the plan for the student."""
                .formatted(plan.weekStart(), plan.weekStart().plusDays(6),
                        StudyCalendar.toSydney(clock.instant()).truncatedTo(ChronoUnit.MINUTES));
    }
}
