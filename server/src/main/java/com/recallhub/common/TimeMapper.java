package com.recallhub.common;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.*;

@Component
@RequiredArgsConstructor
public class TimeMapper {
    private final ZoneId appZoneId;

    public LocalDateTime toUtc(OffsetDateTime value) {
        return value == null ? null : value.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    public OffsetDateTime fromUtc(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC).atZoneSameInstant(appZoneId).toOffsetDateTime();
    }

    public LocalDateTime startOfDayUtc(LocalDate date) {
        return date.atStartOfDay(appZoneId).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    public LocalDateTime endExclusiveUtc(LocalDate date) {
        return date.plusDays(1).atStartOfDay(appZoneId).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}

