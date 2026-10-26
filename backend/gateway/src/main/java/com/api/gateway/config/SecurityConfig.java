package com.api.gateway.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.api.gateway.securityFilters.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/v1/auth/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/products/mine").authenticated()
						.requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/media/images/**").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/media/products/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/products/**").hasRole("SELLER")
						.requestMatchers(HttpMethod.PUT, "/api/v1/products/**").hasRole("SELLER")
						.requestMatchers(HttpMethod.DELETE, "/api/v1/products/**").hasRole("SELLER")
						.requestMatchers(HttpMethod.POST, "/api/v1/media/**").hasRole("SELLER")
						.anyRequest().authenticated())
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(List.of("http://localhost:4200", "http://127.0.0.1:4200"));
		configuration.setAllowedMethods(List.of(
				HttpMethod.GET.name(), HttpMethod.POST.name(), HttpMethod.PUT.name(),
				HttpMethod.DELETE.name(), HttpMethod.OPTIONS.name()));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-User-Id", "X-User-Role"));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}