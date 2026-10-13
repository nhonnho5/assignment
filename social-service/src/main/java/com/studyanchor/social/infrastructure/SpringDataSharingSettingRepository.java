package com.studyanchor.social.infrastructure;

import com.studyanchor.social.domain.SharingSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/** Spring Data generates the SQL for these methods from their names. */
interface SpringDataSharingSettingRepository extends JpaRepository<SharingSetting, String> {

    List<SharingSetting> findByUserIdInAndSharingTrue(Collection<String> userIds);
}
