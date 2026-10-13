package com.studyanchor.tutor.service;

import com.studyanchor.tutor.domain.ConflictException;
import com.studyanchor.tutor.domain.DomainEvent;
import com.studyanchor.tutor.domain.NotFoundException;
import com.studyanchor.tutor.domain.SessionStatus;
import com.studyanchor.tutor.domain.TopicReference;
import com.studyanchor.tutor.domain.TutorAgent;
import com.studyanchor.tutor.domain.TutorUnavailableException;
import com.studyanchor.tutor.domain.TutoringSession;
import com.studyanchor.tutor.domain.TutoringSessionRepository;
import com.studyanchor.tutor.domain.UnderstandingRecorded;
import com.studyanchor.tutor.service.TopicCatalogServiceTest.InMemoryTopics;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Application-service rules for FR-07, with in-memory fakes and a scripted agent (no Spring, no Kafka, no LLM). */
class TutoringServiceTest {

    /** Keeps sessions in a map instead of a database and counts saves. */
    static class InMemorySessions implements TutoringSessionRepository {

        final Map<UUID, TutoringSession> byId = new HashMap<>();
        int saves;

        @Override
        public TutoringSession save(TutoringSession session) {
            saves++;
            byId.put(session.id(), session);
            return session;
        }

        @Override
        public Optional<TutoringSession> findByIdAndUserId(UUID id, String userId) {
            return Optional.ofNullable(byId.get(id)).filter(s -> s.userId().equals(userId));
        }

        @Override
        public void deleteByTopicId(UUID topicId) {
            byId.values().removeIf(s -> s.topicId().equals(topicId));
        }
    }

    /** Stands in for the LLM: runs what the test tells its "tools" to do, then replies; or fails like a dead LLM. */
    static class ScriptedAgent implements TutorAgent {

        Consumer<TutoringSession> tools = s -> { };
        boolean down;

        @Override
        public String answer(TutoringSession session, TopicReference topic) {
            if (down) {
                throw new TutorUnavailableException("quota exceeded", null);
            }
            if (session.checkQuestionDue()) {
                return "Check: why does the key matter?";
            }
            tools.accept(session);
            return "What do you think a partition is?";
        }
    }

    private final InMemoryTopics topics = new InMemoryTopics();
    private final InMemorySessions sessions = new InMemorySessions();
    private final ScriptedAgent agent = new ScriptedAgent();
    private final List<DomainEvent> published = new ArrayList<>();
    private final TutoringService service = new TutoringService(sessions, topics, agent,
            event -> {
                assertThat(sessions.saves).as("saved before publishing").isPositive();
                published.add(event);
            },
            Clock.fixed(Instant.parse("2026-10-14T03:00:00Z"), ZoneOffset.UTC));

    private final UUID topicId = topics.save(
            new TopicReference(UUID.randomUUID(), "alice", UUID.randomUUID(), "CSCI318", "Kafka")).topicId();

    @Test
    void FR07_unknownTopicIs404() {
        assertThatThrownBy(() -> service.start("alice", UUID.randomUUID(), "What is a partition?"))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.start("bob", topicId, "What is a partition?"))
                .isInstanceOf(NotFoundException.class);
        assertThat(sessions.byId).isEmpty();
    }

    @Test
    void FR07_otherStudentsSessionIs404() {
        TutoringSession s = service.start("alice", topicId, "What is a partition?");

        assertThatThrownBy(() -> service.get("bob", s.id())).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.send("bob", s.id(), "A log?")).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.end("bob", s.id())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void FR07_tutorReplyIsAddedToTranscript() {
        TutoringSession s = service.start("alice", topicId, "What is a partition?");

        assertThat(s.messages()).extracting(m -> m.fromStudent() + ":" + m.text())
                .containsExactly("true:What is a partition?", "false:What do you think a partition is?");
        assertThat(service.get("alice", s.id())).isSameAs(s);
    }

    @Test
    void FR07_llmFailureSavesNothing() {
        agent.down = true;

        assertThatThrownBy(() -> service.start("alice", topicId, "What is a partition?"))
                .isInstanceOf(TutorUnavailableException.class);
        assertThat(sessions.saves).isZero();
        assertThat(published).isEmpty();
    }

    @Test
    void FR07_levelIsPublishedAfterSave() {
        TutoringSession s = service.start("alice", topicId, "What is a partition?");
        service.end("alice", s.id());
        assertThat(s.messages().getLast().text()).isEqualTo("Check: why does the key matter?");
        assertThatThrownBy(() -> service.end("alice", s.id())).isInstanceOf(ConflictException.class);

        agent.tools = session -> session.recordUnderstanding(4, Instant.parse("2026-10-14T03:00:00Z"));
        service.send("alice", s.id(), "It picks the partition, so order is kept per key");

        assertThat(s.status()).isEqualTo(SessionStatus.ENDED);
        assertThat(published).singleElement().isInstanceOfSatisfying(UnderstandingRecorded.class,
                e -> assertThat(e.level()).isEqualTo(4));
    }
}
