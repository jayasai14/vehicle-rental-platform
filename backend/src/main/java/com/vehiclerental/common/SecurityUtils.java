package com.vehiclerental.common;

import com.vehiclerental.config.JwtAuthenticationFilter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public class SecurityUtils {

    public static Optional<Long> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof JwtAuthenticationFilter.UserPrincipal principal) {
            return Optional.of(principal.getId());
        }
        return Optional.empty();
    }

    public static Long getRequiredCurrentUserId() {
        return getCurrentUserId().orElseThrow(() -> new CustomExceptions.UnauthorizedException("User is not authenticated"));
    }

    public static boolean hasRole(String roleName) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        String targetRole = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(targetRole));
    }
}
