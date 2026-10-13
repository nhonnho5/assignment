package com.studyanchor.social.infrastructure;

import com.studyanchor.social.domain.Presence;
import com.studyanchor.social.domain.PresenceBoard;
import com.studyanchor.social.domain.PresenceUnavailableException;
import org.apache.kafka.streams.errors.InvalidStateStoreException;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.cloud.stream.binder.kafka.streams.InteractiveQueryService;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Implements FR-09 AC4. Adapter: answers {@link PresenceBoard} with an interactive query on the presence
 * store, so "who is studying now" is read straight from the stream's state (no database, no call to Focus).
 */
@Component
class KafkaStreamsPresenceBoard implements PresenceBoard {

    private final InteractiveQueryService queryService;

    KafkaStreamsPresenceBoard(InteractiveQueryService queryService) {
        this.queryService = queryService;
    }

    @Override
    public Map<String, Presence> presenceOf(Collection<String> userIds) {
        Map<String, Presence> result = new HashMap<>();
        if (userIds.isEmpty()) {
            return result;
        }
        // While the stream is starting or rebalancing, both getting the store and reading it can fail
        try {
            ReadOnlyKeyValueStore<String, Presence> store =
                    queryService.getQueryableStore(PresenceTopology.STORE, QueryableStoreTypes.keyValueStore());
            for (String userId : userIds) {
                Presence presence = store.get(userId);
                if (presence != null) {
                    result.put(userId, presence);
                }
            }
        } catch (IllegalStateException | InvalidStateStoreException e) {
            throw new PresenceUnavailableException("Presence is not available yet, try again shortly", e);
        }
        return result;
    }
}
