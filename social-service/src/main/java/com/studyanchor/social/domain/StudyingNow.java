package com.studyanchor.social.domain;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Implements FR-09 AC1, AC2, AC5, AC6. Domain service: the rules of the "studying now" board, kept out of
 * the presence store so they can be tested without Kafka.
 */
public final class StudyingNow {

    private StudyingNow() {
    }

    /**
     * FR-09 AC1, AC5: friends who are studying right now and share their presence, longest-studying first.
     * A friend with sharing off never appears, whatever the store says.
     */
    public static List<Presence> friendsStudying(Collection<String> friendIds, Map<String, Presence> presences,
                                                 Set<String> sharingOn) {
        return friendIds.stream()
                .filter(sharingOn::contains)
                .map(presences::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Presence::startedAt))
                .toList();
    }

    /** FR-09 AC2, AC6: how many room members are studying; an anonymous count, so sharing does not matter. */
    public static int countStudying(Collection<String> members, Map<String, Presence> presences) {
        return (int) members.stream().filter(presences::containsKey).count();
    }
}
