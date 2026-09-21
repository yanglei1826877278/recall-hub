package com.recallhub.share;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recallhub.common.BusinessException;
import com.recallhub.journal.DailyJournalEntity;
import com.recallhub.journal.DailyJournalMapper;
import com.recallhub.share.JournalShareDtos.PublicShareState;
import com.recallhub.share.JournalShareDtos.ShareStatus;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class JournalShareService {
    private static final String SESSION_GRANTS = "recallhub.share.grants";
    private static final long GRANT_HOURS = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JournalShareMapper shares;
    private final DailyJournalMapper journals;
    private final PasswordEncoder passwordEncoder;
    private final ShareUnlockRateLimiter rateLimiter;
    private final Clock clock;

    public ShareStatus status(LocalDate date) {
        DailyJournalEntity journal = findJournal(date);
        if (journal == null) return ShareStatus.inactive();
        JournalShareEntity share = shares.findByJournalId(journal.getId());
        if (!active(share)) return ShareStatus.inactive();
        return view(share, journal);
    }

    @Transactional
    public ShareStatus create(LocalDate date, String password) {
        DailyJournalEntity journal = requireShareableJournal(date);
        JournalShareEntity share = shares.findByJournalId(journal.getId());
        if (active(share)) {
            throw new BusinessException("JOURNAL_SHARE_ALREADY_ACTIVE", "这一天已经在分享中", HttpStatus.CONFLICT);
        }

        LocalDateTime now = utcNow();
        if (share == null) {
            share = new JournalShareEntity();
            share.setJournalId(journal.getId());
            share.setPasswordVersion(1);
        } else {
            share.setPasswordVersion(share.getPasswordVersion() == null ? 1 : share.getPasswordVersion() + 1);
        }
        share.setShareToken(newToken());
        share.setPasswordHash(passwordEncoder.encode(password));
        share.setPasswordDigits(4);
        share.setContentSnapshot(effectiveContent(journal));
        share.setSnapshotUpdatedAt(now);
        share.setSharedAt(now);
        share.setRevokedAt(null);
        if (share.getId() == null) shares.insert(share); else shares.updateById(share);
        return view(shares.findByJournalId(journal.getId()), journal);
    }

    @Transactional
    public ShareStatus updatePassword(LocalDate date, String password) {
        DailyJournalEntity journal = requireJournal(date);
        JournalShareEntity share = requireActive(shares.findByJournalId(journal.getId()));
        share.setPasswordHash(passwordEncoder.encode(password));
        share.setPasswordDigits(4);
        share.setPasswordVersion(share.getPasswordVersion() + 1);
        shares.updateById(share);
        return view(shares.selectById(share.getId()), journal);
    }

    @Transactional
    public ShareStatus updateContent(LocalDate date) {
        DailyJournalEntity journal = requireShareableJournal(date);
        JournalShareEntity share = requireActive(shares.findByJournalId(journal.getId()));
        share.setContentSnapshot(effectiveContent(journal));
        share.setSnapshotUpdatedAt(utcNow());
        shares.updateById(share);
        return view(shares.selectById(share.getId()), journal);
    }

    @Transactional
    public void revoke(LocalDate date) {
        DailyJournalEntity journal = requireJournal(date);
        JournalShareEntity share = requireActive(shares.findByJournalId(journal.getId()));
        share.setRevokedAt(utcNow());
        shares.updateById(share);
    }

    public PublicShareState publicState(String token, HttpSession session) {
        JournalShareEntity share = requirePublicShare(token);
        UnlockGrant grant = grants(session).get(share.getId());
        if (grant == null || grant.passwordVersion() != share.getPasswordVersion()
                || grant.unlockedAt().plus(GRANT_HOURS, ChronoUnit.HOURS).isBefore(clock.instant())) {
            if (grant != null) removeGrant(session, share.getId());
            return PublicShareState.locked(passwordDigits(share));
        }
        DailyJournalEntity journal = journals.selectById(share.getJournalId());
        if (journal == null) throw publicNotFound();
        return new PublicShareState(true, passwordDigits(share), journal.getJournalDate(), share.getContentSnapshot());
    }

    public PublicShareState unlock(String token, String password, String clientAddress, HttpSession session) {
        JournalShareEntity share = requirePublicShare(token);
        String key = share.getId() + ":" + clientAddress;
        rateLimiter.check(key);
        if (!passwordEncoder.matches(password, share.getPasswordHash())) {
            rateLimiter.failure(key);
            throw new BusinessException("SHARE_PASSWORD_INVALID", "查看密码错误", HttpStatus.UNAUTHORIZED);
        }
        rateLimiter.success(key);
        putGrant(session, share.getId(), new UnlockGrant(share.getPasswordVersion(), clock.instant()));
        return publicState(token, session);
    }

    private ShareStatus view(JournalShareEntity share, DailyJournalEntity journal) {
        return new ShareStatus(true, "/s/" + share.getShareToken(), share.getSharedAt(),
                share.getSnapshotUpdatedAt(), !Objects.equals(share.getContentSnapshot(), effectiveContent(journal)));
    }

    private DailyJournalEntity findJournal(LocalDate date) {
        return journals.selectOne(new LambdaQueryWrapper<DailyJournalEntity>()
                .eq(DailyJournalEntity::getJournalDate, date));
    }

    private DailyJournalEntity requireJournal(LocalDate date) {
        DailyJournalEntity journal = findJournal(date);
        if (journal == null) throw new BusinessException("JOURNAL_NOT_FOUND", "这一天还没有整理后的日记", HttpStatus.NOT_FOUND);
        return journal;
    }

    private DailyJournalEntity requireShareableJournal(LocalDate date) {
        DailyJournalEntity journal = requireJournal(date);
        if (effectiveContent(journal).isBlank()) {
            throw new BusinessException("JOURNAL_SHARE_EMPTY", "请先保存整理后的日记再分享");
        }
        return journal;
    }

    private String effectiveContent(DailyJournalEntity journal) {
        if (journal.getContent() != null && !journal.getContent().isBlank()) return journal.getContent();
        return journal.getGeneratedContent() == null ? "" : journal.getGeneratedContent();
    }

    private JournalShareEntity requireActive(JournalShareEntity share) {
        if (!active(share)) throw new BusinessException("JOURNAL_SHARE_NOT_ACTIVE", "这一天还没有启用分享", HttpStatus.NOT_FOUND);
        return share;
    }

    private JournalShareEntity requirePublicShare(String token) {
        JournalShareEntity share = token == null || token.length() > 64 ? null : shares.findByToken(token);
        if (!active(share)) throw publicNotFound();
        return share;
    }

    private boolean active(JournalShareEntity share) {
        return share != null && share.getRevokedAt() == null;
    }

    private int passwordDigits(JournalShareEntity share) {
        return Objects.equals(share.getPasswordDigits(), 4) ? 4 : 6;
    }

    private BusinessException publicNotFound() {
        return new BusinessException("JOURNAL_SHARE_NOT_FOUND", "分享不存在或已取消", HttpStatus.NOT_FOUND);
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private LocalDateTime utcNow() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    @SuppressWarnings("unchecked")
    private Map<Long, UnlockGrant> grants(HttpSession session) {
        Object current = session.getAttribute(SESSION_GRANTS);
        return current instanceof Map<?, ?> ? (Map<Long, UnlockGrant>) current : Map.of();
    }

    private void putGrant(HttpSession session, Long shareId, UnlockGrant grant) {
        synchronized (session) {
            Map<Long, UnlockGrant> updated = new HashMap<>(grants(session));
            updated.put(shareId, grant);
            session.setAttribute(SESSION_GRANTS, updated);
        }
    }

    private void removeGrant(HttpSession session, Long shareId) {
        synchronized (session) {
            Map<Long, UnlockGrant> updated = new HashMap<>(grants(session));
            updated.remove(shareId);
            session.setAttribute(SESSION_GRANTS, updated);
        }
    }

    private record UnlockGrant(int passwordVersion, Instant unlockedAt) implements java.io.Serializable {}
}
