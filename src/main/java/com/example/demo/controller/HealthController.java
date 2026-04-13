package com.example.demo.controller;

import com.example.demo.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    private final UserRepository userRepository;

    public HealthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        long userCount = userRepository.count();
        log.info("Health check: {} users in DB", userCount);
        return Map.of(
            "status", "OK",
            "database", "H2 file-based",
            "userCount", userCount,
            "defaultUsers", userCount >= 2 ? "Present" : "Missing - check permissions"
        );
    }
}

