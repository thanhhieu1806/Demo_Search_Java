package com.example.demo.controller;

public record AuthRequest(String username, String password, String email) {
    public String normalizedUsername() {
        if (username == null) {
            return null;
        }
        String normalized = username.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public String normalizedEmail() {
        if (email == null) {
            return null;
        }
        String normalized = email.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
