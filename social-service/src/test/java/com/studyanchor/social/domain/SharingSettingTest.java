package com.studyanchor.social.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Domain rules for FR-08 AC3 sharing settings. Plain Java, no Spring. */
class SharingSettingTest {

    @Test
    void FR08_sharingIsOffByDefault() {
        assertThat(SharingSetting.defaultFor("alice").isSharing()).isFalse();
    }

    @Test
    void FR08_sharingCanBeTurnedOnAndOff() {
        SharingSetting setting = SharingSetting.defaultFor("alice");

        setting.change(true);
        assertThat(setting.isSharing()).isTrue();

        setting.change(false);
        assertThat(setting.isSharing()).isFalse();
    }

    @Test
    void FR08_sharingValueIsRequired() {
        SharingSetting setting = SharingSetting.defaultFor("alice");

        assertThatThrownBy(() -> setting.change(null)).isInstanceOf(DomainException.class);
    }
}
