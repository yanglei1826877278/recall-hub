package com.recallhub.auth;

import com.recallhub.common.ApiResponse;
import com.recallhub.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.web.csrf.CsrfToken;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserMapper users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @GetMapping("/status")
    public ApiResponse<?> status(Authentication authentication, CsrfToken csrfToken) {
        boolean initialized = users.selectCount(null) > 0;
        boolean authenticated = authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
        return ApiResponse.ok(Map.of("initialized", initialized, "authenticated", authenticated,
                "username", authenticated ? authentication.getName() : "", "csrfHeader", csrfToken.getHeaderName()));
    }

    @PostMapping("/setup")
    @Transactional
    public ApiResponse<?> setup(@Valid @RequestBody Credentials body, HttpServletRequest request) {
        if (users.selectCount(null) > 0)
            throw new BusinessException("ALREADY_INITIALIZED", "系统已经完成初始化", HttpStatus.CONFLICT);
        UserEntity user = new UserEntity();
        user.setUsername(body.username());
        user.setPasswordHash(passwordEncoder.encode(body.password()));
        users.insert(user);
        return login(body, request);
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@Valid @RequestBody Credentials body, HttpServletRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(body.username(), body.password()));
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        request.getSession(true).setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return ApiResponse.ok(Map.of("username", authentication.getName()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return ApiResponse.ok();
    }

    public record Credentials(@NotBlank @Size(max = 100) String username,
                              @NotBlank @Size(min = 8, max = 128) String password) {}
}

