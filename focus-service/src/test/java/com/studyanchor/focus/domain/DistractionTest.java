package com.studyanchor.focus.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Domain rules for FR-06 (log a distraction) and the FR-12 spike rule. Plain Java, no Spring. */
class DistractionTest {

    private static final Instant START = Instant.parse("2026-10-14T03:00:00Z");
    private static final Instant LATER = START.plusSeconds(600);

    private FocusSession session(String... allowedSites) {
        return FocusSession.start("alice", null, null, 25, List.of(allowedSites), START);
    }

    private static Distraction log(FocusSession session, String type, String site) {
        return session.logDistraction(type, site, null, LATER);
    }

    @Test
    void FR06_canLogOnlyDuringActiveSession() {
        FocusSession session = session();
        session.end(LATER);

        assertThatThrownBy(() -> log(session, "PHONE", null)).isInstanceOf(ConflictException.class);
    }

    @Test
    void FR06_typeIsRequiredAndCaseInsensitive() {
        FocusSession session = session();

        assertThatThrownBy(() -> log(session, null, null)).isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> log(session, "TIKTOK", null)).isInstanceOf(DomainException.class);
        assertThat(log(session, " social_media ", null).type()).isEqualTo(DistractionType.SOCIAL_MEDIA);
    }

    @Test
    void FR06_siteIsCleanedToHost() {
        FocusSession session = session(" HTTPS://www.Google.com/search?q=x ");

        assertThat(session.allowedSites()).containsExactly("google.com");
        assertThat(log(session, "OTHER_SITE", "http://www.YouTube.com:443/watch?v=1").site()).isEqualTo("youtube.com");
        assertThatThrownBy(() -> log(session, "OTHER_SITE", "https://")).isInstanceOf(DomainException.class);
    }

    @Test
    void FR06_siteOutsideAllowedSitesIsOffPolicy() {
        assertThat(log(session("google.com"), "OTHER_SITE", "youtube.com").offPolicy()).isTrue();
        assertThat(log(session(), "OTHER_SITE", "youtube.com").offPolicy()).as("empty list allows nothing").isTrue();
        assertThat(log(session("google.com"), "OTHER_SITE", "google.com").offPolicy()).isFalse();
    }

    @Test
    void FR06_subdomainOfAllowedSiteIsAllowed() {
        FocusSession session = session("google.com");

        assertThat(log(session, "OTHER_SITE", "docs.google.com").offPolicy()).isFalse();
        assertThat(log(session, "OTHER_SITE", "notgoogle.com").offPolicy()).as("not a subdomain").isTrue();
    }

    @Test
    void FR06_noSiteIsNeverOffPolicy() {
        Distraction distraction = log(session(), "PHONE", "  ");

        assertThat(distraction.site()).isNull();
        assertThat(distraction.offPolicy()).isFalse();
    }

    @Test
    void FR06_siteAndNoteLimits() {
        FocusSession session = session();

        assertThatThrownBy(() -> log(session, "OTHER_SITE", "a".repeat(197) + ".com"))
                .isInstanceOf(DomainException.class);
        assertThatThrownBy(() -> session.logDistraction("THOUGHT", null, "x".repeat(501), LATER))
                .isInstanceOf(DomainException.class);
        assertThat(session.logDistraction("THOUGHT", null, " " + "x".repeat(500) + " ", LATER).note())
                .hasSize(500);
        assertThat(session.logDistraction("THOUGHT", null, " ", LATER).note()).isNull();
    }

    @Test
    void FR06_loggingRegistersDistractionLogged() {
        FocusSession session = session("google.com");
        session.pullDomainEvents();

        Distraction distraction = session.logDistraction("other_site", "youtube.com", "rabbit hole", LATER);

        assertThat(session.distractions()).containsExactly(distraction);
        assertThat(distraction.loggedAt()).isEqualTo(LATER);
        assertThat(session.pullDomainEvents()).singleElement().isInstanceOfSatisfying(DistractionLogged.class, e -> {
            assertThat(e.eventType()).isEqualTo("DistractionLogged");
            assertThat(e.userId()).isEqualTo("alice");
            assertThat(e.occurredAt()).isEqualTo(LATER);
            assertThat(e.sessionId()).isEqualTo(session.id());
            assertThat(e.distractionType()).isEqualTo(DistractionType.OTHER_SITE);
            assertThat(e.site()).isEqualTo("youtube.com");
            assertThat(e.offPolicy()).isTrue();
        });
    }

    @Test
    void FR12_spikeFiresOnceAtThirdDistraction() {
        assertThat(DistractionSpikeDetected.firesAt(2)).isFalse();
        assertThat(DistractionSpikeDetected.firesAt(3)).isTrue();
        assertThat(DistractionSpikeDetected.firesAt(4)).as("only once per window").isFalse();
    }
}
