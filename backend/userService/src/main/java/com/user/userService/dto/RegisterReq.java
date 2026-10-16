package com.user.userService.dto;

import com.user.userService.enums.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterReq(@Email @NotNull @NotBlank @Size(min = 3, max = 999) String email,
        @NotBlank @Size(min = 3, max = 900) String name,
        @NotBlank @Size(min = 6, max = 260) String password,
        @NotNull Role role) {
}