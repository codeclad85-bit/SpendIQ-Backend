package com.expense.expense_tracker.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(

                    "/api/health",

                    "/api/auth/register",
                    "/api/auth/login",

                    "/api/expenses",
                    "/api/expenses/**",

                    "/api/daily-limit",
                    "/api/daily-limit/**",

                    "/api/weekly-limit",
                    "/api/weekly-limit/**",

                    "/api/fifteen-day-limit",
                    "/api/fifteen-day-limit/**",

                    "/api/monthly-limit",
                    "/api/monthly-limit/**",

                    "/api/income",
                    "/api/income/**",

                    "/api/budget-plan",
                    "/api/budget-plan/**",

                    "/api/smart-budget",
                    "/api/smart-budget/**",

                    "/api/ai-advisor",
                    "/api/ai-advisor/**",

                    "/api/purchase-advisor",
                    "/api/purchase-advisor/**",

                    "/api/product-comparison",
                    "/api/product-comparison/**",

                    // SpendIQ PDF Reports
                    "/api/reports",
                    "/api/reports/**",

                    // SpendIQ Email Reports
                    "/api/report-email",
                    "/api/report-email/**"

                ).permitAll()

                .anyRequest().authenticated()
            );

        return http.build();
    }
}