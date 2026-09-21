package com.recallhub.share;

import com.recallhub.common.BusinessException;
import com.recallhub.journal.DailyJournalEntity;
import com.recallhub.journal.DailyJournalMapper;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class JournalShareServiceTest {
    @Mock JournalShareMapper shares;
    @Mock DailyJournalMapper journals;
    @Mock PasswordEncoder passwordEncoder;
    @Mock ShareUnlockRateLimiter rateLimiter;

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-21T10:00:00Z"), ZoneOffset.UTC);
    private JournalShareService service;

    @BeforeEach
    void setUp() {
        service = new JournalShareService(shares, journals, passwordEncoder, rateLimiter, clock);
    }

    @Test
    void createsAnImmutableSnapshotForExactlyOneDailyJournal() {
        DailyJournalEntity journal = journal("最初分享的正文");
        when(journals.selectOne(any())).thenReturn(journal);
        when(shares.findByJournalId(7L)).thenReturn(null).thenAnswer(invocation -> {
            JournalShareEntity saved = new JournalShareEntity();
            saved.setId(11L);
            saved.setJournalId(7L);
            saved.setShareToken("saved-token");
            saved.setPasswordHash("hash");
            saved.setPasswordVersion(1);
            saved.setContentSnapshot("最初分享的正文");
            saved.setSnapshotUpdatedAt(java.time.LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
            saved.setSharedAt(java.time.LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
            return saved;
        });
        when(passwordEncoder.encode("0123")).thenReturn("hash");

        var result = service.create(LocalDate.of(2026, 9, 21), "0123");

        assertThat(result.active()).isTrue();
        assertThat(result.sharePath()).isEqualTo("/s/saved-token");
        assertThat(result.contentOutdated()).isFalse();
        verify(passwordEncoder).encode("0123");
        ArgumentCaptor<JournalShareEntity> saved = ArgumentCaptor.forClass(JournalShareEntity.class);
        verify(shares).insert(saved.capture());
        assertThat(saved.getValue().getJournalId()).isEqualTo(7L);
        assertThat(saved.getValue().getContentSnapshot()).isEqualTo("最初分享的正文");
        assertThat(saved.getValue().getShareToken()).hasSize(43);
        assertThat(saved.getValue().getPasswordDigits()).isEqualTo(4);
    }

    @Test
    void passwordVersionChangeInvalidatesAnExistingUnlockedSession() {
        DailyJournalEntity journal = journal("公开快照");
        JournalShareEntity share = activeShare();
        MockHttpSession session = new MockHttpSession();
        when(shares.findByToken("token")).thenReturn(share);
        when(journals.selectById(7L)).thenReturn(journal);
        when(passwordEncoder.matches("1234", "hash")).thenReturn(true);

        assertThat(service.unlock("token", "1234", "127.0.0.1", session).unlocked()).isTrue();

        share.setPasswordVersion(2);
        assertThat(service.publicState("token", session).unlocked()).isFalse();
    }

    @Test
    void revokedShareCannotBeReadEvenByAnExistingLink() {
        JournalShareEntity share = activeShare();
        share.setRevokedAt(java.time.LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
        when(shares.findByToken("token")).thenReturn(share);

        assertThatThrownBy(() -> service.publicState("token", new MockHttpSession()))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getCode()).isEqualTo("JOURNAL_SHARE_NOT_FOUND"));
    }

    @Test
    void passwordContractAcceptsLeadingZeroAndRejectsOtherShapes() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(new JournalShareDtos.PasswordBody("0123"))).isEmpty();
            assertThat(validator.validate(new JournalShareDtos.PasswordBody("123"))).isNotEmpty();
            assertThat(validator.validate(new JournalShareDtos.PasswordBody("12a4"))).isNotEmpty();
            assertThat(validator.validate(new JournalShareDtos.PasswordBody("12345"))).isNotEmpty();
            assertThat(validator.validate(new JournalShareDtos.UnlockBody("1234"))).isEmpty();
            assertThat(validator.validate(new JournalShareDtos.UnlockBody("123456"))).isEmpty();
        }
    }

    private DailyJournalEntity journal(String content) {
        DailyJournalEntity journal = new DailyJournalEntity();
        journal.setId(7L);
        journal.setJournalDate(LocalDate.of(2026, 9, 21));
        journal.setContent(content);
        return journal;
    }

    private JournalShareEntity activeShare() {
        JournalShareEntity share = new JournalShareEntity();
        share.setId(11L);
        share.setJournalId(7L);
        share.setShareToken("token");
        share.setPasswordHash("hash");
        share.setPasswordVersion(1);
        share.setContentSnapshot("公开快照");
        return share;
    }
}
