package com.studyanchor.tutor.infrastructure;

import com.studyanchor.tutor.domain.Material;
import com.studyanchor.tutor.domain.MaterialRepository;
import com.studyanchor.tutor.domain.MaterialSearch;
import com.studyanchor.tutor.domain.Message;
import com.studyanchor.tutor.domain.TopicReference;
import com.studyanchor.tutor.domain.TutorAgent;
import com.studyanchor.tutor.domain.TutorUnavailableException;
import com.studyanchor.tutor.domain.TutoringSession;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.ChatMemory;
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
import java.util.List;

/**
 * Implements FR-07. Adapter for the {@link TutorAgent} port with LangChain4j and Google Gemini. Each turn it
 * rebuilds the conversation from the saved transcript and gives the LLM tools bound to this one session.
 * Without {@code LLM_API_KEY} the service still starts; tutoring then answers 503 (AC12).
 */
@Component
class GeminiTutorAgent implements TutorAgent {

    private static final Logger log = LoggerFactory.getLogger(GeminiTutorAgent.class);
    private static final int MEMORY_MESSAGES = 60;

    /** LangChain4j turns this interface into the agent: it sends the message, runs tool calls, returns the text. */
    interface Tutor {
        String chat(String message);
    }

    private final ChatModel model;
    private final TutoringTools.Solver solver;
    private final MaterialRepository materials;
    private final Clock clock;

    GeminiTutorAgent(@Value("${llm.api-key}") String apiKey, @Value("${llm.model}") String modelName,
                     MaterialRepository materials, Clock clock) {
        // Gemini 3 signs each tool call ("thought signature") and rejects the next request unless it is sent back
        this.model = apiKey.isBlank() ? null : GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey).modelName(modelName).timeout(Duration.ofSeconds(60))
                .returnThinking(true).sendThinking(true).build();
        this.solver = model == null ? null : AiServices.create(TutoringTools.Solver.class, model);
        this.materials = materials;
        this.clock = clock;
        log.info("Tutor agent: model {} ({})", modelName, model == null ? "no LLM_API_KEY, tutoring disabled" : "ready");
    }

    @Override
    public String answer(TutoringSession session, TopicReference topic) {
        if (model == null) {
            throw new TutorUnavailableException("The AI tutor is not configured: LLM_API_KEY is not set", null);
        }
        List<Message> transcript = session.messages();
        List<Material> topicMaterials = materials.findByTopicId(session.topicId());
        ChatMemory memory = MessageWindowChatMemory.withMaxMessages(MEMORY_MESSAGES);
        memory.add(SystemMessage.from(instructions(session, topic, topicMaterials)));
        transcript.subList(0, transcript.size() - 1).forEach(m ->
                memory.add(m.fromStudent() ? UserMessage.from(m.text()) : AiMessage.from(m.text())));
        Tutor tutor = AiServices.builder(Tutor.class)
                .chatModel(model)
                .chatMemory(memory)
                .tools(new TutoringTools(session, topicMaterials, solver, clock))
                .toolArgumentsErrorHandler(ToolArgumentsErrorHandler.sendExceptionMessageToLlm())
                .toolExecutionErrorHandler(ToolExecutionErrorHandler.failInvocation())
                .build();
        String reply;
        try {
            reply = tutor.chat(transcript.getLast().text());
        } catch (RuntimeException e) {
            log.warn("Tutor LLM call failed for session {}", session.id(), e);
            throw new TutorUnavailableException("The AI tutor is unavailable right now: " + e.getMessage(), e);
        }
        if (reply == null || reply.isBlank()) {
            throw new TutorUnavailableException("The AI tutor returned an empty reply; please resend", null);
        }
        return reply.strip();
    }

    /**
     * The prompt side of AC2–AC10; the same rules are enforced again in code by the tools. AC4: the material
     * paragraphs closest to the question are always included, whether or not the LLM calls searchMaterial.
     */
    private static String instructions(TutoringSession s, TopicReference topic, List<Material> topicMaterials) {
        String grounding = topicMaterials.isEmpty()
                ? "This topic has no study material yet; help from general knowledge."
                : "The student's material most related to the question (call searchMaterial for more):\n"
                + TutoringTools.quote(MaterialSearch.search(topicMaterials, s.question()));
        String stage = switch (s.status()) {
            case ACTIVE -> """
                    Rules:
                    - Never write the full solution or the final answer yourself. Guide with one question or one hint.
                    - Call searchMaterial before you give a hint, and say which material helped when it did.
                    - When the student's newest message is a real try at answering, call recordAttempt once.
                    - If the student asks for the answer, call revealSolution. If it is REFUSED, encourage them and
                      give another hint. If it returns a solution, share it and walk through it step by step.
                    Attempts recorded so far: %d.""".formatted(s.attempts());
            case CHECKING -> s.checkQuestionDue()
                    ? "The student has ended the session. Ask exactly one short check-understanding question about"
                    + " their original question. Do not grade yet and do not give the answer."
                    : "The student has answered your check-understanding question. Grade it by calling"
                    + " recordUnderstanding with a level from 1 to 5, then tell them the level and one thing to review.";
            case ENDED -> "The session has ended.";
        };
        return """
                You are a Socratic tutor for a university student. Many students using this app have ADHD, so keep
                every reply short (under 120 words), warm and focused, and ask one thing at a time.
                Topic: %s (%s). The student's question: "%s"
                %s
                %s""".formatted(topic.topicName(), topic.courseCode(), s.question(), grounding, stage);
    }
}
