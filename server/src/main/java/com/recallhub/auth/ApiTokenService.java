package com.recallhub.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recallhub.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ApiTokenService {
    public static final Set<String> ALLOWED_SCOPES = Set.of(
            "ENTRY_READ", "ENTRY_WRITE", "REMINDER_READ", "REMINDER_WRITE",
            "SEARCH", "JOURNAL_READ", "JOURNAL_WRITE");
    private final ApiTokenMapper mapper;
    private final ObjectMapper objectMapper;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public CreatedToken create(String name, List<String> scopes) {
        if (scopes == null || scopes.isEmpty() || !ALLOWED_SCOPES.containsAll(scopes))
            throw new BusinessException("INVALID_SCOPE", "Token 权限范围无效");
        byte[] secret = new byte[32];
        random.nextBytes(secret);
        String plain = "rh_" + HexFormat.of().formatHex(secret);
        ApiTokenEntity entity = new ApiTokenEntity();
        entity.setName(name);
        entity.setTokenPrefix(plain.substring(0, 11));
        entity.setTokenHash(hash(plain));
        try { entity.setScopes(objectMapper.writeValueAsString(scopes)); }
        catch (Exception e) { throw new IllegalStateException(e); }
        entity.setEnabled(true);
        mapper.insert(entity);
        return new CreatedToken(entity.getId(), name, entity.getTokenPrefix(), scopes, plain, entity.getCreatedAt());
    }

    public TokenPrincipal authenticate(String plain) {
        if (plain == null || !plain.startsWith("rh_")) return null;
        ApiTokenEntity entity = mapper.selectOne(new LambdaQueryWrapper<ApiTokenEntity>()
                .eq(ApiTokenEntity::getTokenHash, hash(plain)).eq(ApiTokenEntity::getEnabled, true));
        if (entity == null) return null;
        try {
            List<String> scopes = objectMapper.readValue(entity.getScopes(), new TypeReference<>() {});
            entity.setLastUsedAt(LocalDateTime.now());
            mapper.updateById(entity);
            return new TokenPrincipal(entity.getId(), entity.getName(), scopes.stream()
                    .map(s -> new SimpleGrantedAuthority("SCOPE_" + s)).toList());
        } catch (Exception e) {
            throw new BusinessException("TOKEN_INVALID", "Token 配置损坏", HttpStatus.UNAUTHORIZED);
        }
    }

    public List<TokenView> list() {
        return mapper.selectList(null).stream().map(e -> {
            try {
                return new TokenView(e.getId(), e.getName(), e.getTokenPrefix(),
                        objectMapper.readValue(e.getScopes(), new TypeReference<List<String>>() {}),
                        e.getEnabled(), e.getCreatedAt(), e.getLastUsedAt());
            } catch (Exception ex) { throw new IllegalStateException(ex); }
        }).toList();
    }

    public void revoke(long id) {
        ApiTokenEntity token = mapper.selectById(id);
        if (token == null) throw new BusinessException("TOKEN_NOT_FOUND", "Token 不存在", HttpStatus.NOT_FOUND);
        token.setEnabled(false);
        mapper.updateById(token);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    public record TokenPrincipal(Long id, String name, List<SimpleGrantedAuthority> authorities) {}
    public record CreatedToken(Long id, String name, String prefix, List<String> scopes,
                               String token, LocalDateTime createdAt) {}
    public record TokenView(Long id, String name, String prefix, List<String> scopes,
                            boolean enabled, LocalDateTime createdAt, LocalDateTime lastUsedAt) {}
}

