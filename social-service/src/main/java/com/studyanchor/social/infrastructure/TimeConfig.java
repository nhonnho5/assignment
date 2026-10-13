package com.studyanchor.social.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneOffset;

/** Provides the clock; tests replace it with a fixed one. Instants are UTC (docs/09-conventions.md). */
@Configuration
class TimeConfig {

    /** Ticks in whole milliseconds so stored instants match what H2 keeps (same as Focus). */
    @Bean
    Clock clock() {
        return Clock.tickMillis(ZoneOffset.UTC);
    }
}
