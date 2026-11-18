package com.solutio.api.domain.login.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "login.rate-limit")
public class LoginRateLimitProperties {

    private final Tier pair = new Tier(5, Duration.ofMinutes(5));
    private final Tier id = new Tier(30, Duration.ofMinutes(15));
    private final Tier ip = new Tier(30, Duration.ofMinutes(15));

    private String clientIpHeader = "CF-Connecting-IP";

    @Getter
    @Setter
    public static class Tier {

        private int maxFailures;
        private Duration blockDuration;

        public Tier() {
        }

        public Tier(int maxFailures, Duration blockDuration) {
            this.maxFailures = maxFailures;
            this.blockDuration = blockDuration;
        }
    }
}
