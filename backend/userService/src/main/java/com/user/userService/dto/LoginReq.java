package com.user.userService.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LoginReq(@Email @NotNull @NotBlank @Size(min = 3, max = 999) String email,
        @NotBlank() @Size(min = 6, max = 260) String password) {
}