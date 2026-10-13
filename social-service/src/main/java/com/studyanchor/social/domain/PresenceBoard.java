package com.studyanchor.social.domain;

import java.util.Collection;
import java.util.Map;

/**
 * Implements FR-09 AC4. Port: who among these users is in a focus session right now. Users not studying are
 * absent from the result. Implemented in infrastructure by the Kafka Streams presence store. Sharing is
 * not applied here; it is applied at query time (FR-09 AC5).
 */
public interface PresenceBoard {

    Map<String, Presence> presenceOf(Collection<String> userIds);
}
