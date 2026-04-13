package com.example.demo.controller;

import com.example.demo.entity.AppUser;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.AppUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    // Thời gian mặc định cho cookie nếu env không được set
    private static final Duration DEFAULT_COOKIE_MAX_AGE = Duration.ofDays(7);
    private static final String DEFAULT_SAME_SITE = "Lax";
    private static final List<String> ROLE_PRIORITY = List.of("ADMIN", "MANAGER", "USER");

    private final SecurityContextRepository securityContextRepository;
    private final AppUserService userService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public AuthController(
            SecurityContextRepository securityContextRepository,
            AppUserService userService,
            PasswordEncoder passwordEncoder,
            UserRepository userRepository) {
        this.securityContextRepository = securityContextRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> register(
            @RequestBody AuthRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thieu du lieu dang ky");
        }
        userService.registerUser(request.normalizedUsername(), request.password(), request.normalizedEmail());
        return completeLogin(request, httpRequest, httpResponse);
    }

    @PostMapping("/login")
    public Map<String, String> login(
            @RequestBody AuthRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        return completeLogin(request, httpRequest, httpResponse);
    }

    private Map<String, String> completeLogin(
            AuthRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        if (request == null || request.normalizedUsername() == null || request.password() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thieu username hoac password");
        }

        String username = request.normalizedUsername();
        log.info("Auth attempt for user: {}", username);

        UserDetails userDetails;
        try {
            userDetails = userService.loadUserByUsername(username);
        } catch (Exception ex) {
            log.error("User lookup failed for '{}': {}", username, ex.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sai ten dang nhap hoac mat khau");
        }

        if (!passwordEncoder.matches(request.password(), userDetails.getPassword())) {
            log.error("Password mismatch for user '{}'", username);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sai ten dang nhap hoac mat khau");
        }

        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                userDetails,
                null,
                userDetails.getAuthorities());

        httpRequest.getSession(true);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        writeInfoCookies(httpRequest, httpResponse, authentication);

        log.info("Auth success for user: {}", username);
        return Map.of(
                "username", userDetails.getUsername(),
                "role", extractRole(authentication));
    }

    private void writeInfoCookies(
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            Authentication authentication) {

        String username = authentication.getName() != null ? authentication.getName() : "";
        String role = extractRole(authentication);

        Duration maxAge = resolveCookieMaxAge(); // luôn trả về non-null
        String sameSite = resolveCookieSameSite(); // luôn trả về non-null
        boolean secure = resolveSecure(httpRequest, sameSite);

        String email = null;
        String userId = null;
        try {
            String normalized = username.isEmpty() ? null : username.toLowerCase(Locale.ROOT);
            if (normalized != null) {
                AppUser user = userRepository.findByUsername(normalized).orElse(null);
                if (user != null) {
                    email = user.getEmail();
                    userId = user.getId() == null ? null : user.getId().toString();
                }
            }
        } catch (Exception ignored) {
        }

        ResponseCookie usernameCookie = ResponseCookie.from("demo_username", username)
                .httpOnly(false)
                .secure(secure)
                .path("/")
                .maxAge(maxAge) // FIX Ln 141: maxAge từ resolveCookieMaxAge() luôn non-null
                .sameSite(sameSite) // FIX Ln 142: sameSite từ resolveCookieSameSite() luôn non-null
                .build();

        ResponseCookie roleCookie = ResponseCookie.from("demo_role", role)
                .httpOnly(false)
                .secure(secure)
                .path("/")
                .maxAge(maxAge) // FIX Ln 149
                .sameSite(sameSite)
                .build();

        httpResponse.addHeader("Set-Cookie", usernameCookie.toString());
        httpResponse.addHeader("Set-Cookie", roleCookie.toString());

        if (email != null && !email.isBlank()) {
            ResponseCookie emailCookie = ResponseCookie.from("demo_email", email)
                    .httpOnly(false)
                    .secure(secure)
                    .path("/")
                    .maxAge(maxAge) // FIX Ln 161
                    .sameSite(sameSite)
                    .build();
            httpResponse.addHeader("Set-Cookie", emailCookie.toString());
        }

        if (userId != null && !userId.isBlank()) {
            ResponseCookie idCookie = ResponseCookie.from("demo_userid", userId)
                    .httpOnly(false)
                    .secure(secure)
                    .path("/")
                    .maxAge(maxAge) // FIX Ln 171
                    .sameSite(sameSite)
                    .build();
            httpResponse.addHeader("Set-Cookie", idCookie.toString());
        }
    }


    @NonNull
    private Duration resolveCookieMaxAge() {
        String raw = System.getenv("COOKIE_MAX_AGE_DAYS");
        if (raw == null || raw.isBlank()) {
            return DEFAULT_COOKIE_MAX_AGE;
        }
        try {
            long days = Long.parseLong(raw.trim());
            return Duration.ofDays(Math.max(days, 1));
        } catch (NumberFormatException ex) {
            return DEFAULT_COOKIE_MAX_AGE;
        }
    }


    @NonNull
    private String resolveCookieSameSite() {
        String raw = System.getenv("COOKIE_SAMESITE");
        if (raw != null && !raw.isBlank()) {
            return raw.trim();
        }
        return DEFAULT_SAME_SITE;
    }

    private boolean resolveSecure(HttpServletRequest request, String sameSite) {
        String raw = System.getenv("COOKIE_SECURE");
        if (raw != null && !raw.isBlank()) {
            return Boolean.parseBoolean(raw.trim());
        }
        if ("None".equalsIgnoreCase(sameSite)) {
            return true;
        }
        return request.isSecure();
    }

    @NonNull
    private String extractRole(Authentication authentication) {
        Set<String> authoritySet = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        for (String role : ROLE_PRIORITY) {
            if (authoritySet.contains("ROLE_" + role)) {
                return role;
            }
        }

        return authoritySet.stream()
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .orElse("USER");
    }
}
