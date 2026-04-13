package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository
    ) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .securityContext(securityContext -> securityContext
                        .securityContextRepository(securityContextRepository)
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                )
                .authorizeHttpRequests(authorize -> authorize
                        // Cho phép tất cả OPTIONS (preflight)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Static assets của React build
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/static/**",
                                "/favicon.ico",
                                "/manifest.json",
                                "/asset-manifest.json",
                                "/logo192.png",
                                "/logo512.png",
                                "/robots.txt"
                        ).permitAll()
                        // React routes (SPA)
                        .requestMatchers("/login", "/register", "/home", "/error").permitAll()
                        // API công khai
                        .requestMatchers("/api/login", "/api/register", "/api/me", "/api/health", "/api/tmsra/**").permitAll()
                        // Cho phép xem h2-console nếu cần (nhưng ở đây đang tắt)
                        .requestMatchers("/h2-console/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            // Trả về 401 cho API thay vì redirect đến login page
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"Bạn chưa đăng nhập hoặc phiên làm việc đã hết hạn\"}");
                        })
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(200);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"message\":\"Logged out\"}");
                        })
                        .addLogoutHandler((request, response, authentication) -> {
                            jakarta.servlet.http.Cookie userCookie = new jakarta.servlet.http.Cookie("demo_username", null);
                            userCookie.setPath("/");
                            userCookie.setMaxAge(0);
                            response.addCookie(userCookie);

                            jakarta.servlet.http.Cookie roleCookie = new jakarta.servlet.http.Cookie("demo_role", null);
                            roleCookie.setPath("/");
                            roleCookie.setMaxAge(0);
                            response.addCookie(roleCookie);

                            jakarta.servlet.http.Cookie emailCookie = new jakarta.servlet.http.Cookie("demo_email", null);
                            emailCookie.setPath("/");
                            emailCookie.setMaxAge(0);
                            response.addCookie(emailCookie);

                            jakarta.servlet.http.Cookie idCookie = new jakarta.servlet.http.Cookie("demo_userid", null);
                            idCookie.setPath("/");
                            idCookie.setMaxAge(0);
                            response.addCookie(idCookie);
                        })
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        
        String frontendUrls = System.getenv("FRONTEND_URLS");
        String frontendUrl  = System.getenv("FRONTEND_URL");

        if (frontendUrls != null && !frontendUrls.isBlank()) {
            List<String> origins = Arrays.stream(frontendUrls.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            configuration.setAllowedOrigins(origins);

        } else if (frontendUrl != null && !frontendUrl.isBlank()) {
            configuration.setAllowedOrigins(List.of(frontendUrl.trim()));

        } else {
            configuration.setAllowedOriginPatterns(List.of("*"));
        }

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Set-Cookie", "Authorization"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
