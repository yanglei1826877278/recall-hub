package com.recallhub.share;

import com.recallhub.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ShareUnlockRateLimiter {
    private static final int MAX_FAILURES = 5;
    private static final Duration WINDOW = Duration.ofMinutes(10);
    private final ConcurrentHashMap<String, AttemptWindow> attempts = new ConcurrentHashMap<>();
    private final Clock clock;

    public ShareUnlockRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public void check(String key) {
        Instant now = clock.instant();
        AttemptWindow window = attempts.get(key);
        if (window == null) return;
        if (!window.startedAt().plus(WINDOW).isAfter(now)) {
            attempts.remove(key, window);
            return;
        }
        if (window.failures() >= MAX_FAILURES) {
            throw new BusinessException("SHARE_UNLOCK_RATE_LIMITED", "尝试次数过多，请稍后再试", HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    public void failure(String key) {
        Instant now = clock.instant();
        attempts.compute(key, (ignored, current) -> {
            if (current == null || !current.startedAt().plus(WINDOW).isAfter(now)) {
                return new AttemptWindow(now, 1);
            }
            return new AttemptWindow(current.startedAt(), current.failures() + 1);
        });
    }

    public void success(String key) {
        attempts.remove(key);
    }

    record AttemptWindow(Instant startedAt, int failures) {}
}
