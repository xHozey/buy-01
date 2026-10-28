package com.product.productService.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import com.product.productService.securityFilters.GatewayIdentityFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        @Bean
        public SecurityFilterChain enableSpringSecurity(
                        HttpSecurity http,
                        GatewayIdentityFilter gatewayIdentityFilter) throws Exception {
                return http
                                .csrf(csrf -> csrf.disable())
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/actuator/health").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                                                .anyRequest()
                                                .authenticated())
                                .addFilterBefore(gatewayIdentityFilter,
                                                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                                .build();

        }
}