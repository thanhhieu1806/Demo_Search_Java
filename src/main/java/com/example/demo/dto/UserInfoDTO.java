package com.example.demo.dto;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record UserInfoDTO(
        String username,
        String role,
        Set<String> authorities,
        boolean authenticated
) {
    private static final List<String> ROLE_PRIORITY = List.of("ADMIN", "MANAGER", "USER");

    public static UserInfoDTO fromAuthentication(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || authentication.getAuthorities().stream()
                        .anyMatch(a -> "ROLE_ANONYMOUS".equals(a.getAuthority()))) {
            return new UserInfoDTO(null, null, Set.of(), false);
        }

        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        String role = resolvePrimaryRole(authorities);

        return new UserInfoDTO(
                authentication.getName(),
                role,
                authorities,
                true
        );
    }

    private static String resolvePrimaryRole(Set<String> authorities) {
        for (String role : ROLE_PRIORITY) {
            if (authorities.contains("ROLE_" + role)) {
                return role;
            }
        }
        return authorities.stream()
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .findFirst()
                .orElse("USER");
    }
}
