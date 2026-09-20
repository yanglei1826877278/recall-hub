package com.recallhub.theme;

import com.recallhub.common.ApiResponse;
import com.recallhub.theme.ThemeService.ThemeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/themes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class ThemeController {
    private final ThemeService service;
    @GetMapping public ApiResponse<?> list() { return ApiResponse.ok(service.list()); }
    @GetMapping("/current") public ApiResponse<?> current() { return ApiResponse.ok(service.current()); }
    @GetMapping("/{id}") public ApiResponse<?> get(@PathVariable long id) { return ApiResponse.ok(service.get(id)); }
    @PostMapping public ApiResponse<?> create(@Valid @RequestBody ThemeRequest body) { return ApiResponse.ok(service.create(body)); }
    @PutMapping("/{id}") public ApiResponse<?> update(@PathVariable long id, @Valid @RequestBody ThemeRequest body) {
        return ApiResponse.ok(service.update(id, body));
    }
    @DeleteMapping("/{id}") public ApiResponse<Void> delete(@PathVariable long id) { service.delete(id); return ApiResponse.ok(); }
    @PostMapping("/{id}/activate") public ApiResponse<?> activate(@PathVariable long id) { return ApiResponse.ok(service.activate(id)); }
    @GetMapping(value = "/{id}/export", produces = "text/css")
    public ResponseEntity<String> export(@PathVariable long id) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=globals.css")
                .contentType(MediaType.parseMediaType("text/css;charset=UTF-8")).body(service.export(id));
    }
}

