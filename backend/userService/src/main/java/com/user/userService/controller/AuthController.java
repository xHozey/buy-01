package com.user.userService.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.user.userService.dto.LoginReq;
import com.user.userService.dto.LoginRes;
import com.user.userService.dto.RegisterReq;
import com.user.userService.dto.RegisterRes;
import com.user.userService.service.AuthService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public RegisterRes Register(@RequestBody @Valid RegisterReq req) {
        return authService.register(req);
    }

    @PostMapping("/login")
    public LoginRes Login(@RequestBody @Valid LoginReq req) {
        String token = authService.login(req);
        return new LoginRes(token);
    }
}