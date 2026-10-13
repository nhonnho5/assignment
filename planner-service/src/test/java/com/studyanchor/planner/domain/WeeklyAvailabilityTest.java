package com.studyanchor.planner.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.UUID;

import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.TUESDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Domain rules for FR-03 weekly availability. Plain Java, no Spring. */
class WeeklyAvailabilityTest {

    private final WeeklyAvailability availability = WeeklyAvailability.emptyFor("alice");

    private static LocalTime t(String time) {
        return LocalTime.parse(time);
    }

    @Test
    void FR03_slotMustBeAtLeast25Minutes() {
        assertThatThrownBy(() -> availability.add(MONDAY, t("09:00"), t("09:24"))).isInstanceOf(DomainException.class);
        assertThat(availability.add(MONDAY, t("09:00"), t("09:25")).end()).isEqualTo(t("09:25"));
    }

    @Test
    void FR03_endMustBeAfterStart() {
        assertThatThrownBy(() -> availability.add(MONDAY, t("10:00"), t("09:00"))).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> availability.add(MONDAY, t("23:00"), t("00:30"))).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> availability.add(null, t("09:00"), t("10:00"))).isInstanceOf(DomainException.class);
    }

    @Test
    void FR03_timesMustBeWholeMinutes() {
        assertThatThrownBy(() -> availability.add(MONDAY, t("09:00:30"), t("10:00")))
                .isInstanceOf(DomainException.class);
    }

    @Test
    void FR03_overlappingSlotIsConflict() {
        availability.add(MONDAY, t("09:00"), t("11:00"));

        assertThatThrownBy(() -> availability.add(MONDAY, t("10:30"), t("12:00"))).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> availability.add(MONDAY, t("08:00"), t("12:00"))).isInstanceOf(ConflictException.class);
    }

    @Test
    void FR03_touchingSlotsAreAllowed() {
        availability.add(MONDAY, t("09:00"), t("10:00"));
        availability.add(MONDAY, t("10:00"), t("11:00"));

        assertThat(availability.slots()).hasSize(2);
    }

    @Test
    void FR03_sameTimeOnOtherDayIsAllowed() {
        availability.add(MONDAY, t("09:00"), t("10:00"));
        availability.add(TUESDAY, t("09:00"), t("10:00"));

        assertThat(availability.slots()).hasSize(2);
    }

    @Test
    void FR03_removingUnknownSlotIsNotFound() {
        UUID id = availability.add(MONDAY, t("09:00"), t("10:00")).id();
        availability.remove(id);

        assertThat(availability.slots()).isEmpty();
        assertThatThrownBy(() -> availability.remove(id)).isInstanceOf(NotFoundException.class);
    }
}
