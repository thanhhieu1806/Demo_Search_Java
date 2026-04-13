package com.example.demo.controller;

import com.example.demo.dto.UserInfoDTO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/api/home")
    public String home(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "Ban chua dang nhap";
        }
        String username = authentication.getName();
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(granted -> granted.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return "hello world";
        }
        return "xin chào " + username;
    }

    @GetMapping("/api/admin")
    public String adminHome() {
        return "hello world - Admin Dashboard";
    }

    @GetMapping("/api/me")
    public UserInfoDTO getCurrentUser(Authentication authentication) {
        return UserInfoDTO.fromAuthentication(authentication);
    }

    @GetMapping("/api/check-role")
    public Map<String, Boolean> checkRole(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(granted -> granted.getAuthority().equals("ROLE_ADMIN"));
        boolean isUser = authentication.getAuthorities().stream()
                .anyMatch(granted -> granted.getAuthority().equals("ROLE_USER"));
        return Map.of(
                "admin", isAdmin,
                "user", isUser,
                "authenticated", authentication.isAuthenticated()
        );
    }

    @GetMapping("/api/user-detail")
    public Map<String, Object> getUserDetails(Authentication authentication) {
        Map<String, Object> details = new HashMap<>();
        details.put("username", authentication.getName());
        details.put("roles", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList());
        details.put("authenticated", authentication.isAuthenticated());
        return details;
    }
}
