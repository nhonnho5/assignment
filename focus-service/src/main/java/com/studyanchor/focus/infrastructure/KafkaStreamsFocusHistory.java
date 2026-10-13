package com.studyanchor.focus.infrastructure;

import com.studyanchor.focus.domain.FocusHistory;
import com.studyanchor.focus.domain.FocusHistoryUnavailableException;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Implements FR-10 AC1–AC3. Adapter: answers {@link FocusHistory} with an interactive query on the
 * stream's window store, so progress is read straight from the stream's state (no database).
 */
@Component
class KafkaStreamsFocusHistory implements FocusHistory {

    private final InteractiveQueryService queryService;

    KafkaStreamsFocusHistory(InteractiveQueryService queryService) {
        this.queryService = queryService;
    }

    @Override
    public Map<LocalDate, Long> secondsPerDay(String userId, LocalDate until) {
        Map<LocalDate, Long> result = new HashMap<>();
        Instant to = SydneyDayTimestampExtractor.windowStartOf(until);
        // While the stream is starting or rebalancing, both getting the store and reading it can fail
        try {
            ReadOnlyWindowStore<String, Long> store =
                    queryService.getQueryableStore(FocusProgressTopology.STORE, QueryableStoreTypes.windowStore());
            // From the epoch: read every day the store still retains (no fixed streak cap, FR-10 AC7)
            try (var windows = store.fetch(userId, Instant.EPOCH, to)) {
                windows.forEachRemaining(kv -> result.put(SydneyDayTimestampExtractor.sydneyDayOf(kv.key), kv.value));
            }
        } catch (IllegalStateException | InvalidStateStoreException e) {
            throw new FocusHistoryUnavailableException("Focus progress is not available yet, try again shortly", e);
        }
        return result;
    }
}
