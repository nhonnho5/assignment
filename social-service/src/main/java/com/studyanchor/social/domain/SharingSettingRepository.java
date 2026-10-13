package com.studyanchor.social.domain;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

/** Implements FR-08 AC3, FR-09 AC5. Repository for {@link SharingSetting}; implemented in infrastructure. */
public interface SharingSettingRepository {

    SharingSetting save(SharingSetting setting);

    Optional<SharingSetting> findByUserId(String userId);

    /** FR-09 AC5: which of these users currently have sharing on. */
    Set<String> sharingAmong(Collection<String> userIds);
}
