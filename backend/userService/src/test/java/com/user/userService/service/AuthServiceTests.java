package com.user.userService.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.user.userService.dto.LoginReq;
import com.user.userService.dto.RegisterReq;
import com.user.userService.dto.RegisterRes;
import com.user.userService.enums.Role;
import com.user.userService.model.User;
import com.user.userService.reposiroty.UserRepository;
import com.user.userService.utils.JwtUtil;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {
    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginReturnsGeneratedTokenForValidCredentials() {
        User user = user("user-1", "Alice", "alice@example.com", "password");
        user.setPassword(new BCryptPasswordEncoder().encode("password"));
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateAuthenticationToken(user)).thenReturn("jwt-token");

        assertEquals("jwt-token", authService.login(new LoginReq("alice@example.com", "password")));
        verify(jwtUtil).generateAuthenticationToken(user);
    }

    @Test
    void loginRejectsUnknownEmail() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.login(new LoginReq("missing@example.com", "password")));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(jwtUtil, never()).generateAuthenticationToken(any(User.class));
    }

    @Test
    void loginRejectsIncorrectPassword() {
        User user = user("user-1", "Alice", "alice@example.com", "password");
        user.setPassword(new BCryptPasswordEncoder().encode("different-password"));
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.login(new LoginReq("alice@example.com", "password")));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(jwtUtil, never()).generateAuthenticationToken(any(User.class));
    }

    @Test
    void registerRejectsMissingRole() {
        RegisterReq request = new RegisterReq("alice@example.com", "Alice", "password", null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.register(request));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterReq request = registerRequest();
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user("user-1", "Other", request.email(),
                "encoded")));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.register(request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerRejectsDuplicateName() {
        RegisterReq request = registerRequest();
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(userRepository.findByName(request.name())).thenReturn(Optional.of(user("user-1", request.name(),
                "other@example.com", "encoded")));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.register(request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerSavesEncodedPasswordAndReturnsSavedUserDetails() {
        RegisterReq request = registerRequest();
        User savedUser = user("user-1", request.name(), request.email(), "encoded-password");
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(userRepository.findByName(request.name())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        RegisterRes response = authService.register(request);

        assertEquals(new RegisterRes("user-1", "Alice", "alice@example.com"), response);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedRequest = userCaptor.getValue();
        assertNotEquals(request.password(), savedRequest.getPassword());
        assertTrue(new BCryptPasswordEncoder().matches(request.password(), savedRequest.getPassword()));
        assertEquals(request.role(), savedRequest.getRole());
        assertFalse(savedRequest.getPassword().isBlank());
    }

    private static RegisterReq registerRequest() {
        return new RegisterReq("alice@example.com", "Alice", "password", Role.CLIENT);
    }

    private static User user(String id, String name, String email, String password) {
        return new User(id, name, email, password, Role.CLIENT, null);
    }
}