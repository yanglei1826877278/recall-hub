package com.recallhub.share;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class JournalShareDtos {
    private JournalShareDtos() {}

    public record PasswordBody(
            @NotNull
            @Pattern(regexp = "\\d{4}", message = "查看密码必须是 4 位数字")
            String password) {}

    public record UnlockBody(
            @NotNull
            @Pattern(regexp = "(?:\\d{4}|\\d{6})", message = "查看密码格式不正确")
            String password) {}

    public record ShareStatus(
            boolean active,
            String sharePath,
            LocalDateTime sharedAt,
            LocalDateTime snapshotUpdatedAt,
            boolean contentOutdated) {
        public static ShareStatus inactive() {
            return new ShareStatus(false, null, null, null, false);
        }
    }

    public record PublicShareState(boolean unlocked, int passwordDigits, LocalDate date, String content) {
        public static PublicShareState locked(int passwordDigits) {
            return new PublicShareState(false, passwordDigits, null, null);
        }
    }
}
