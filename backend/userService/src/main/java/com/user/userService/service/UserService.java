package com.user.userService.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.user.userService.dto.ProfileUpdateReq;
import com.user.userService.dto.UserRes;
import com.user.userService.model.User;
import com.user.userService.reposiroty.UserRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UserService {
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserRes GetMe(String userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return new UserRes(user.getId(), user.getName(), user.getEmail(), user.getAvatar(), user.getRole());
    }

    public UserRes UpdateProfile(String userId, ProfileUpdateReq req) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid password");
        }

        userRepo.findByEmail(req.email()).ifPresent(existingUser -> {
            if (!existingUser.getId().equals(user.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
            }
        });

        userRepo.findByName(req.name()).ifPresent(existingUser -> {
            if (!existingUser.getId().equals(user.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Name is already registered");
            }
        });

        user.setEmail(req.email());
        user.setName(req.name());
        user.setAvatar(req.avatar());
        User savedUser = userRepo.save(user);

        return new UserRes(savedUser.getId(), savedUser.getName(), savedUser.getEmail(), savedUser.getAvatar(),
                savedUser.getRole());
    }
}