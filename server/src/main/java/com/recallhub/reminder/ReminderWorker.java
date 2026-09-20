package com.recallhub.reminder;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ReminderWorker {
    private static final Logger log = LoggerFactory.getLogger(ReminderWorker.class);
    private final ReminderMapper mapper;
    private final ReminderService service;

    @Scheduled(fixedDelay = 10_000, initialDelay = 10_000)
    public void poll() {
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        int recovered = mapper.recoverExpired(now);
        if (recovered > 0) log.warn("Recovered {} expired reminder leases", recovered);
        for (Long id : mapper.findDueIds(now, 20)) {
            try {
                if (service.claim(id, now)) service.deliver(id);
            } catch (Exception ex) {
                log.error("Reminder delivery failed unexpectedly, reminderId={}", id, ex);
            }
        }
    }
}

