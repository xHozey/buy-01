package com.media.mediaService.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import com.media.mediaService.securityFilters.*;;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain enableSpringSecurity(
            HttpSecurity http,
            GatewayIdentityFilter gatewayIdentityFilter) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll().anyRequest().authenticated())
                .addFilterBefore(gatewayIdentityFilter,
                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .build();

    }
}