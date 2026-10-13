package com.studyanchor.social.infrastructure;

import com.studyanchor.social.domain.Presence;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.util.function.Consumer;

/**
 * Implements FR-09 AC3, AC4. Kafka Streams topology over {@code focus.sessions} (key = userId): Started becomes
 * a {@link Presence}, Ended becomes a tombstone (null), and the result is a table holding only the users who
 * are in a session right now, materialised as the queryable key-value store {@value #STORE}.
 * Every user's presence is stored; the sharing filter is applied at query time (FR-09 AC5).
 */
@Configuration
class PresenceTopology {

    static final String STORE = "presence-store";

    private static final Logger log = LoggerFactory.getLogger(PresenceTopology.class);

    @Bean
    Consumer<KStream<String, FocusSessionEvent>> presence() {
        return events -> events
                .filter((userId, event) -> event != null && (event.isStarted() || event.isEnded()))
                .peek((userId, event) -> log.info("Presence: {} for user {} (session {}, topic {})",
                        event.eventType(), userId, event.sessionId(), event.topicId()))
                // A null value is a tombstone: toTable deletes the key, so the user is no longer "studying now"
                .mapValues(event -> event.isStarted() ? event.toPresence() : null)
                .toTable(Materialized.<String, Presence, KeyValueStore<Bytes, byte[]>>as(STORE)
                        .withKeySerde(Serdes.String())
                        .withValueSerde(presenceSerde()));
    }

    /** JSON without Spring's type headers, so the store's changelog does not depend on Java class names. */
    static JsonSerde<Presence> presenceSerde() {
        return new JsonSerde<>(Presence.class).noTypeInfo().ignoreTypeHeaders();
    }
}
