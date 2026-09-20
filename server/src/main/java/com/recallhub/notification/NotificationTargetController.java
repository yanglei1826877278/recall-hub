package com.recallhub.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recallhub.common.ApiResponse;
import com.recallhub.common.BusinessException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notification-targets")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class NotificationTargetController {
    private final NotificationTargetMapper mapper;

    @GetMapping public ApiResponse<?> list() {
        return ApiResponse.ok(mapper.selectList(new LambdaQueryWrapper<NotificationTargetEntity>()
                .orderByDesc(NotificationTargetEntity::getIsDefault).orderByAsc(NotificationTargetEntity::getId)));
    }

    @PostMapping @Transactional
    public ApiResponse<?> create(@Valid @RequestBody TargetRequest body) {
        NotificationTargetEntity entity = copy(new NotificationTargetEntity(), body);
        if (Boolean.TRUE.equals(entity.getIsDefault())) clearDefault();
        mapper.insert(entity);
        return ApiResponse.ok(mapper.selectById(entity.getId()));
    }

    @PutMapping("/{id}") @Transactional
    public ApiResponse<?> update(@PathVariable long id, @Valid @RequestBody TargetRequest body) {
        NotificationTargetEntity entity = mapper.selectById(id);
        if (entity == null) throw new BusinessException("TARGET_NOT_FOUND", "通知渠道不存在", HttpStatus.NOT_FOUND);
        copy(entity, body);
        if (Boolean.TRUE.equals(entity.getIsDefault())) clearDefault();
        mapper.updateById(entity);
        return ApiResponse.ok(mapper.selectById(id));
    }

    @DeleteMapping("/{id}") public ApiResponse<Void> delete(@PathVariable long id) {
        mapper.deleteById(id); return ApiResponse.ok();
    }

    private NotificationTargetEntity copy(NotificationTargetEntity entity, TargetRequest body) {
        entity.setName(body.name()); entity.setChannel(body.channel()); entity.setTarget(body.target());
        entity.setAccountId(body.accountId()); entity.setAgentId(body.agentId() == null ? "main" : body.agentId());
        entity.setEnabled(body.enabled() == null || body.enabled()); entity.setIsDefault(Boolean.TRUE.equals(body.isDefault()));
        return entity;
    }

    private void clearDefault() {
        mapper.selectList(new LambdaQueryWrapper<NotificationTargetEntity>().eq(NotificationTargetEntity::getIsDefault, true))
                .forEach(t -> { t.setIsDefault(false); mapper.updateById(t); });
    }

    public record TargetRequest(@NotBlank String name, @NotBlank String channel, @NotBlank String target,
                                String accountId, String agentId, Boolean enabled, Boolean isDefault) {}
}

