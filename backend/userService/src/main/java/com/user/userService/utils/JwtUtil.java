package com.user.userService.utils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.user.userService.model.User;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {
    private final Key secretKey;
    private final long EXPIRATION = 10 * 1000 * 1000 * 60;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAuthenticationToken(User user) {
        return Jwts.builder().subject(user.getId()).claim("name", user.getName())
                .claim("role", "ROLE_" + user.getRole()).issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION)).signWith(secretKey).compact();
    }

}