package com.studyanchor.social.service;

import com.studyanchor.social.domain.SharingSetting;
import com.studyanchor.social.domain.SharingSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implements FR-08 AC3. Application service for a student's presence-sharing setting. */
@Service
public class SharingService {

    private final SharingSettingRepository repository;

    public SharingService(SharingSettingRepository repository) {
        this.repository = repository;
    }

    /** A student who never changed the setting gets the default (off), without saving anything. */
    @Transactional(readOnly = true)
    public SharingSetting get(String userId) {
        return repository.findByUserId(userId).orElseGet(() -> SharingSetting.defaultFor(userId));
    }

    @Transactional
    public SharingSetting set(String userId, Boolean sharing) {
        SharingSetting setting = get(userId);
        setting.change(sharing);
        return repository.save(setting);
    }
}
