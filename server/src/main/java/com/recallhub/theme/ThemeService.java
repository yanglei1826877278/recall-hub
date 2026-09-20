package com.recallhub.theme;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recallhub.common.BusinessException;
import com.recallhub.setting.AppSettingEntity;
import com.recallhub.setting.AppSettingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ThemeService {
    private static final Set<String> TOKENS = Set.of(
            "--background","--foreground","--card","--card-foreground","--popover","--popover-foreground",
            "--primary","--primary-foreground","--secondary","--secondary-foreground","--muted","--muted-foreground",
            "--accent","--accent-foreground","--destructive","--border","--input","--ring",
            "--chart-1","--chart-2","--chart-3","--chart-4","--chart-5","--radius",
            "--sidebar","--sidebar-foreground","--sidebar-primary","--sidebar-primary-foreground",
            "--sidebar-accent","--sidebar-accent-foreground","--sidebar-border","--sidebar-ring",
            "--todo","--todo-foreground","--diary","--diary-foreground","--idea","--idea-foreground",
            "--note","--note-foreground","--reminder","--reminder-foreground","--success","--success-foreground",
            "--overdue","--overdue-foreground");
    private static final Pattern COLOR = Pattern.compile("(?i)^(oklch|hsl|hsla|rgb|rgba)\\([^;{}]*\\)$|^#[0-9a-f]{3,8}$|^(transparent|currentColor)$");
    private static final Pattern RADIUS = Pattern.compile("^0$|^[0-9]+(?:\\.[0-9]+)?(?:px|rem|em)$");
    private final ThemeMapper mapper;
    private final AppSettingMapper settings;
    private final ObjectMapper objectMapper;

    public List<ThemeView> list() { return mapper.selectList(new LambdaQueryWrapper<ThemeEntity>()
            .orderByDesc(ThemeEntity::getIsBuiltin).orderByAsc(ThemeEntity::getName)).stream().map(this::view).toList(); }
    public ThemeView get(long id) { return view(require(id)); }

    public ThemeView current() {
        AppSettingEntity value = settings.selectById("theme_id");
        long id = value == null ? 1 : Long.parseLong(value.getSettingValue());
        ThemeEntity theme = mapper.selectById(id);
        return view(theme == null ? require(1) : theme);
    }

    @Transactional
    public ThemeView create(ThemeRequest request) {
        validate(request.lightVariables()); validate(request.darkVariables());
        ThemeEntity entity = new ThemeEntity();
        entity.setName(request.name().trim()); entity.setSlug(uniqueSlug(request.slug(), request.name(), null));
        writeVariables(entity, request); entity.setIsBuiltin(false); mapper.insert(entity); return view(mapper.selectById(entity.getId()));
    }

    @Transactional
    public ThemeView update(long id, ThemeRequest request) {
        ThemeEntity entity = require(id);
        if (Boolean.TRUE.equals(entity.getIsBuiltin())) throw new BusinessException("BUILTIN_THEME", "内置主题不能修改");
        validate(request.lightVariables()); validate(request.darkVariables());
        entity.setName(request.name().trim()); entity.setSlug(uniqueSlug(request.slug(), request.name(), id));
        writeVariables(entity, request); mapper.updateById(entity); return view(mapper.selectById(id));
    }

    @Transactional public void delete(long id) {
        ThemeEntity entity = require(id);
        if (Boolean.TRUE.equals(entity.getIsBuiltin())) throw new BusinessException("BUILTIN_THEME", "内置主题不能删除");
        AppSettingEntity current = settings.selectById("theme_id");
        if (current != null && String.valueOf(id).equals(current.getSettingValue())) settings.upsert("theme_id", "1");
        mapper.deleteById(id);
    }

    public ThemeView activate(long id) { require(id); settings.upsert("theme_id", String.valueOf(id)); return get(id); }

    public String export(long id) {
        ThemeView theme = get(id);
        return block(":root", theme.lightVariables()) + "\n\n" + block(".dark", theme.darkVariables()) + "\n";
    }

    private String block(String selector, Map<String,String> values) {
        StringBuilder css = new StringBuilder(selector).append(" {\n");
        values.forEach((k,v) -> css.append("  ").append(k).append(": ").append(v).append(";\n"));
        return css.append("}").toString();
    }

    private void validate(Map<String,String> values) {
        if (values == null) throw new BusinessException("THEME_INVALID", "主题变量不能为空");
        for (var entry : values.entrySet()) {
            if (!TOKENS.contains(entry.getKey())) throw new BusinessException("THEME_UNKNOWN_TOKEN", "不支持的主题变量：" + entry.getKey());
            String value = entry.getValue() == null ? "" : entry.getValue().trim();
            if (value.toLowerCase().contains("url(") || value.toLowerCase().contains("expression(") || value.contains(";"))
                throw new BusinessException("THEME_UNSAFE_VALUE", "主题变量包含不安全的值");
            if ("--radius".equals(entry.getKey()) ? !RADIUS.matcher(value).matches() : !COLOR.matcher(value).matches())
                throw new BusinessException("THEME_INVALID_VALUE", entry.getKey() + " 的值无效");
        }
    }

    private String uniqueSlug(String requested, String name, Long id) {
        String base = requested == null || requested.isBlank() ? name : requested;
        base = Normalizer.normalize(base, Normalizer.Form.NFKD).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (base.isBlank()) base = "theme";
        String slug = base; int n = 2;
        while (mapper.selectCount(new LambdaQueryWrapper<ThemeEntity>().eq(ThemeEntity::getSlug, slug)
                .ne(id != null, ThemeEntity::getId, id)) > 0) slug = base + "-" + n++;
        return slug;
    }

    private void writeVariables(ThemeEntity entity, ThemeRequest request) {
        try {
            entity.setLightVariables(objectMapper.writeValueAsString(request.lightVariables()));
            entity.setDarkVariables(objectMapper.writeValueAsString(request.darkVariables()));
            entity.setSourceCss(request.sourceCss());
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    private ThemeView view(ThemeEntity e) {
        try {
            return new ThemeView(e.getId(), e.getName(), e.getSlug(),
                    objectMapper.readValue(e.getLightVariables(), new TypeReference<LinkedHashMap<String,String>>() {}),
                    objectMapper.readValue(e.getDarkVariables(), new TypeReference<LinkedHashMap<String,String>>() {}),
                    e.getSourceCss(), Boolean.TRUE.equals(e.getIsBuiltin()), e.getCreatedAt(), e.getUpdatedAt());
        } catch (Exception ex) { throw new IllegalStateException(ex); }
    }

    private ThemeEntity require(long id) {
        ThemeEntity entity = mapper.selectById(id);
        if (entity == null) throw new BusinessException("THEME_NOT_FOUND", "主题不存在", HttpStatus.NOT_FOUND);
        return entity;
    }

    public record ThemeRequest(String name, String slug, Map<String,String> lightVariables,
                               Map<String,String> darkVariables, String sourceCss) {}
    public record ThemeView(Long id, String name, String slug, Map<String,String> lightVariables,
                            Map<String,String> darkVariables, String sourceCss, boolean builtin,
                            java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt) {}
}

