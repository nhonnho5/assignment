package com.studyanchor.tutor.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneOffset;

/** Provides the clock; tests replace it with a fixed one. Instants are UTC (docs/09-conventions.md). */
@Configuration
class TimeConfig {

    /** Ticks in whole milliseconds so the instants in events match what H2 stores (it keeps microseconds). */
    @Bean
    Clock clock() {
        return Clock.tickMillis(ZoneOffset.UTC);
    }
}
