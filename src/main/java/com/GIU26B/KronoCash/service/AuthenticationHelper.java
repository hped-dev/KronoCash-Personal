package com.GIU26B.KronoCash.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

/** Distingue un usuario real del token anónimo de Spring Security. */
public final class AuthenticationHelper {

    private AuthenticationHelper() {
    }

    public static boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserDetails;
    }
}
