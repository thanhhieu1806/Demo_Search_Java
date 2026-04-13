package com.example.demo.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.support.ErrorPageFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ErrorPageFilterConfig {

    @Bean
    @ConditionalOnBean(ErrorPageFilter.class)
    public FilterRegistrationBean<ErrorPageFilter> disableErrorPageFilter(ErrorPageFilter filter) {

        // Tắt ErrorPageFilter mặc định của Spring Boot
        // để tránh xung đột với routing phía frontend (React)
        // nếu không tắt, khi refresh trang (ví dụ /home, /admin)
        // sẽ bị lỗi 404 do Spring hiểu nhầm là API

        FilterRegistrationBean<ErrorPageFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);

        return registration;
    }
}