package com.user.userService.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.user.userService.dto.LoginReq;
import com.user.userService.dto.RegisterReq;
import com.user.userService.dto.RegisterRes;
import com.user.userService.model.User;
import com.user.userService.reposiroty.UserRepository;
import com.user.userService.utils.JwtUtil;

@Service
public class AuthService {
    private final UserRepository userRepo;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepo, JwtUtil jwtUtil) {
        this.userRepo = userRepo;
        this.jwtUtil = jwtUtil;
    }

    public String login(LoginReq req) {
        User user = userRepo.findByEmail(req.email())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        return jwtUtil.generateAuthenticationToken(user);
    }

    public RegisterRes register(RegisterReq req) {
        if (req.role() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role must be CLIENT or SELLER");
        }

        if (userRepo.findByEmail(req.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        if (userRepo.findByName(req.name()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Name is already registered");
        }

        User user = new User(null, req.name(), req.email(), passwordEncoder.encode(req.password()), req.role(), null);
        User savedUser = userRepo.save(user);

        return new RegisterRes(savedUser.getId(), savedUser.getName(), savedUser.getEmail());
    }

}
