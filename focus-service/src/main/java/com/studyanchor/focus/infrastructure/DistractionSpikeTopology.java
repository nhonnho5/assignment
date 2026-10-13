package com.studyanchor.focus.infrastructure;

import com.studyanchor.focus.domain.DistractionSpikeDetected;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Grouped;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.state.WindowStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;
import java.util.function.Function;

/**
 * Implements FR-12 AC1, AC8. Kafka Streams topology over {@code focus.distractions} (key = userId): count each
 * student's distractions in fixed 30-minute windows (event time = when logged, see StreamBridgeEventPublisher)
 * and send one {@link DistractionSpikeDetected} to {@code focus.alerts} when a window reaches 3.
 */
@Configuration
class DistractionSpikeTopology {

    static final String STORE = "distraction-spike-store";

    /** A distraction that reaches Kafka up to a minute after its window closed still counts. */
    static final Duration GRACE = Duration.ofMinutes(1);

    private static final Logger log = LoggerFactory.getLogger(DistractionSpikeTopology.class);

    @Bean
    Function<KStream<String, DistractionMessage>, KStream<String, DistractionSpikeDetected>> distractionSpikes(
            Clock clock) {
        return distractions -> distractions
                .filter((userId, message) -> message != null && message.isLogged())
                .groupByKey(Grouped.keySerde(Serdes.String()))
                .windowedBy(TimeWindows.ofSizeAndGrace(DistractionSpikeDetected.WINDOW, GRACE))
                // Caching off: every new count reaches the filter, so "count = 3" is never merged away
                .count(Materialized.<String, Long, WindowStore<Bytes, byte[]>>as(STORE).withCachingDisabled())
                .toStream()
                .filter((window, count) -> DistractionSpikeDetected.firesAt(count))
                .map((window, count) -> KeyValue.pair(window.key(), DistractionSpikeDetected.of(window.key(),
                        window.window().startTime(), window.window().endTime(), clock.instant())))
                .peek((userId, spike) -> log.info("Detected {} for user {}: {} distractions in {} – {}",
                        spike.eventType(), userId, spike.distractionCount(), spike.windowStart(), spike.windowEnd()));
    }
}
