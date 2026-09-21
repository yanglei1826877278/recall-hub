package com.recallhub.notification;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recallhub.common.ApiResponse;
import com.recallhub.common.BusinessException;
import com.recallhub.openclaw.OpenClawClient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notification-targets")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class NotificationTargetController {
    private final NotificationTargetMapper mapper;
    private final OpenClawClient openClaw;

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

    @PostMapping("/{id}/test")
    public ApiResponse<?> test(@PathVariable long id) {
        NotificationTargetEntity target = mapper.selectById(id);
        if (target == null)
            throw new BusinessException("TARGET_NOT_FOUND", "通知渠道不存在", HttpStatus.NOT_FOUND);

        String message = "✅ RecallHub 渠道测试成功\n\n如果你收到这条消息，说明提醒渠道配置正常。";
        String idempotencyKey = "recallhub:notification-target:test:" + id + ":" + UUID.randomUUID();
        OpenClawClient.HookResult result = openClaw.deliver(target, message, idempotencyKey);
        if (!result.success()) {
            HttpStatus status = "HOOK_NOT_CONFIGURED".equals(result.errorCode())
                    ? HttpStatus.BAD_REQUEST : HttpStatus.BAD_GATEWAY;
            throw new BusinessException(
                    result.errorCode() == null ? "CHANNEL_TEST_FAILED" : result.errorCode(),
                    result.errorMessage() == null ? "测试消息发送失败" : result.errorMessage(),
                    status);
        }
        return ApiResponse.ok(new TestResult(result.runId(), result.delivered()));
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

    public record TestResult(String runId, boolean delivered) {}
}
