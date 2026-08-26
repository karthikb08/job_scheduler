package com.sb.utils;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
public class ClockProvider {

    private final Clock clock;

    public ClockProvider() {
        this.clock = Clock.systemUTC();
    }

    public Instant now() {
        return Instant.now(clock);
    }
}
