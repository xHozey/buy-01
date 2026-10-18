package com.user.userService.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import jakarta.validation.Valid;

import com.user.userService.dto.ProfileUpdateReq;
import com.user.userService.dto.UserRes;
import com.user.userService.service.UserService;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@AllArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public UserRes GetMe(Authentication authentication) {
        return userService.GetMe(authentication.getName());
    }

    @PutMapping("/me")
    public UserRes UpdateProfile(Authentication authentication, @Valid @RequestBody ProfileUpdateReq req) {
        return userService.UpdateProfile(authentication.getName(), req);
    }

}