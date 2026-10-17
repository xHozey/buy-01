package com.user.userService.dto;

import com.user.userService.enums.Role;

public record UserRes(String id, String name, String email, String avatar, Role role) {
}