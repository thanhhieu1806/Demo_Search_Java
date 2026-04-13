package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tmsra")
public class TmsraProperties {

    private static final int DEFAULT_TIMEOUT_SECONDS = 30;

    private String baseUrl = "https://113.161.43.188:9093/RegistrationAuthority/tmsra/restapi";
    private String username = "DL_01";
    private String password = "a12345678@";
    private String language = "0";
    private int timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;
    private boolean insecureSsl = true;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = trimToNull(baseUrl);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = trimToNull(username);
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = trimToNull(password);
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = trimToNull(language);
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : DEFAULT_TIMEOUT_SECONDS;
    }

    public boolean isInsecureSsl() {
        return insecureSsl;
    }

    public void setInsecureSsl(boolean insecureSsl) {
        this.insecureSsl = insecureSsl;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
