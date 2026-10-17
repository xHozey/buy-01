package com.user.userService.service;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.user.userService.dto.UserRes;
import com.user.userService.model.User;
import com.user.userService.reposiroty.UserRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepo;

    public UserRes getMe(String userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return new UserRes(user.getId(), user.getName(), user.getEmail(), user.getAvatar(), user.getRole());
    }
}