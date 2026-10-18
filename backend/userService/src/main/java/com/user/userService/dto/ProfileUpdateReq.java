package com.user.userService.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileUpdateReq(
	@Email @NotBlank @Size(max = 999) String email,
	@NotBlank @Size(min = 3, max = 900) String name,
	String avatar,
	@NotBlank @Size(min = 6, max = 260) String password) {
}