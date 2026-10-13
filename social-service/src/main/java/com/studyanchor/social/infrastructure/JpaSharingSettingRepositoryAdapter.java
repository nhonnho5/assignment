package com.studyanchor.social.infrastructure;

import com.studyanchor.social.domain.SharingSetting;
import com.studyanchor.social.domain.SharingSettingRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Implements FR-08 AC3, FR-09 AC5. Adapter: fulfils the domain repository interface using Spring Data JPA. */
@Repository
class JpaSharingSettingRepositoryAdapter implements SharingSettingRepository {

    private final SpringDataSharingSettingRepository jpa;

    JpaSharingSettingRepositoryAdapter(SpringDataSharingSettingRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public SharingSetting save(SharingSetting setting) {
        return jpa.save(setting);
    }

    @Override
    public Optional<SharingSetting> findByUserId(String userId) {
        return jpa.findById(userId);
    }

    @Override
    public Set<String> sharingAmong(Collection<String> userIds) {
        if (userIds.isEmpty()) {
            return Set.of();
        }
        return jpa.findByUserIdInAndSharingTrue(userIds).stream()
                .map(SharingSetting::userId)
                .collect(Collectors.toSet());
    }
}
