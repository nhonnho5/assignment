package com.studyanchor.focus.infrastructure;

import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.state.WindowStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Implements FR-10. Kafka Streams topology over {@code focus.sessions} (key = userId):
 * keep FocusSessionEnded, sum {@code actualSeconds} per user per 1-day window (Sydney days, see
 * {@link SydneyDayTimestampExtractor}) into the queryable window store {@value #STORE}.
 */
@Configuration
class FocusProgressTopology {

    static final String STORE = "focus-progress-store";

    /** FR-10 AC6: a session still counts if it arrives up to 1 day after its start day ends. */
    static final Duration GRACE = Duration.ofDays(1);

    /** FR-10 AC7: how long daily totals are kept; this is the only limit on the streak. */
    static final Duration RETENTION = Duration.ofDays(3650);

    /**
     * The Sydney timeline runs up to 11 hours ahead of real time, but Kafka 4 brokers reject records more than
     * 1 hour in the future by default. The store's changelog topic is created allowing up to 1 day ahead.
     */
    static final Map<String, String> CHANGELOG_CONFIG =
            Map.of(TopicConfig.MESSAGE_TIMESTAMP_AFTER_MAX_MS_CONFIG, String.valueOf(Duration.ofDays(1).toMillis()));

    @Bean
    SydneyDayTimestampExtractor sydneyDayTimestampExtractor() {
        return new SydneyDayTimestampExtractor();
    }

    @Bean
    Consumer<KStream<String, FocusSessionMessage>> focusProgress() {
        return sessions -> sessions
                .filter((userId, message) -> message != null && message.isEnded())
                .mapValues(FocusSessionMessage::actualSeconds)
                .groupByKey(Grouped.with(Serdes.String(), Serdes.Long()))
                .windowedBy(TimeWindows.ofSizeAndGrace(Duration.ofDays(1), GRACE))
                .reduce(Long::sum,
                        Materialized.<String, Long, WindowStore<Bytes, byte[]>>as(STORE)
                                .withKeySerde(Serdes.String())
                                .withValueSerde(Serdes.Long())
                                .withRetention(RETENTION)
                                .withLoggingEnabled(CHANGELOG_CONFIG));
    }
}
