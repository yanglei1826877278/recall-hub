package com.recallhub.share;

import com.recallhub.common.ApiResponse;
import com.recallhub.share.JournalShareDtos.UnlockBody;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/journal-shares")
@RequiredArgsConstructor
public class PublicJournalShareController {
    private final JournalShareService service;

    @GetMapping("/{token}")
    public ResponseEntity<?> state(@PathVariable String token, HttpServletRequest request) {
        return response(ApiResponse.ok(service.publicState(token, request.getSession(true))));
    }

    @PostMapping("/{token}/unlock")
    public ResponseEntity<?> unlock(@PathVariable String token, @Valid @RequestBody UnlockBody body,
                                    HttpServletRequest request) {
        return response(ApiResponse.ok(service.unlock(token, body.password(), request.getRemoteAddr(),
                request.getSession(true))));
    }

    private ResponseEntity<?> response(ApiResponse<?> body) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("X-Robots-Tag", "noindex, nofollow, noarchive")
                .header("Referrer-Policy", "no-referrer")
                .body(body);
    }
}
