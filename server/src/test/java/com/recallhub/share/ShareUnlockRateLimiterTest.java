package com.recallhub.share;

import com.recallhub.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShareUnlockRateLimiterTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-21T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void blocksAfterFiveWrongAttemptsAndSuccessClearsTheWindow() {
        ShareUnlockRateLimiter limiter = new ShareUnlockRateLimiter(clock);
        String key = "12:127.0.0.1";

        for (int i = 0; i < 5; i++) {
            limiter.check(key);
            limiter.failure(key);
        }

        assertThatThrownBy(() -> limiter.check(key))
                .isInstanceOfSatisfying(BusinessException.class, error -> {
                    assertThat(error.getCode()).isEqualTo("SHARE_UNLOCK_RATE_LIMITED");
                    assertThat(error.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
                });

        limiter.success(key);
        limiter.check(key);
    }
}
