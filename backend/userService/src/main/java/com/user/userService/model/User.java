package com.user.userService.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.user.userService.enums.Role;

@Document("user")
public class User {
    @Id
    String id;
    String name;
    String email;
    String password;
    Role role;
    String avatar;
}
