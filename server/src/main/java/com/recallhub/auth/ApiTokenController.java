package com.recallhub.auth;

import com.recallhub.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/tokens")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class ApiTokenController {
    private final ApiTokenService service;

    @GetMapping public ApiResponse<?> list() { return ApiResponse.ok(service.list()); }
    @PostMapping public ApiResponse<?> create(@Valid @RequestBody CreateToken body) {
        return ApiResponse.ok(service.create(body.name(), body.scopes()));
    }
    @DeleteMapping("/{id}") public ApiResponse<Void> revoke(@PathVariable long id) {
        service.revoke(id); return ApiResponse.ok();
    }

    public record CreateToken(@NotBlank String name, @NotEmpty List<String> scopes) {}
}

