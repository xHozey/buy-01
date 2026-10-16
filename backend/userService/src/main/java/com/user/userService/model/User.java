package com.user.userService.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.user.userService.enums.Role;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document("user")
@AllArgsConstructor
@Data
@NoArgsConstructor
public class User {
    @Id
    String id;
    String name;
    String email;
    String password;
    Role role;
    String avatar;
}
