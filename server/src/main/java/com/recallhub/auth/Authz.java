package com.recallhub.auth;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("authz")
public class Authz {
    public boolean has(String scope, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return false;
        return auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_USER") || a.getAuthority().equals("SCOPE_" + scope));
    }
}

