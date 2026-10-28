package com.user.userService.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.user.userService.dto.ProfileUpdateReq;
import com.user.userService.dto.UserRes;
import com.user.userService.enums.Role;
import com.user.userService.model.User;
import com.user.userService.reposiroty.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getMeReturnsUserDetails() {
        User user = user("user-1", "Alice", "alice@example.com", "avatar.png");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        assertEquals(new UserRes("user-1", "Alice", "alice@example.com", "avatar.png", Role.CLIENT),
                userService.GetMe("user-1"));
    }

    @Test
    void getMeThrowsNotFoundForUnknownUser() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userService.GetMe("missing"));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void updateProfileUpdatesUserWithValidPassword() {
        User user = user("user-1", "Alice", "alice@example.com", "old-avatar.png");
        user.setPassword(PASSWORD_ENCODER.encode("password"));
        ProfileUpdateReq request = request();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(user));
        when(userRepository.findByName(request.name())).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserRes response = userService.UpdateProfile("user-1", request);

        assertEquals(new UserRes("user-1", "New Name", "new@example.com", "new-avatar.png", Role.CLIENT), response);
        assertEquals(request.email(), user.getEmail());
        assertEquals(request.name(), user.getName());
        assertEquals(request.avatar(), user.getAvatar());
        verify(userRepository).save(user);
    }

    @Test
    void updateProfileThrowsNotFoundForUnknownUser() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userService.UpdateProfile("missing", request()));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateProfileRejectsInvalidPassword() {
        User user = user("user-1", "Alice", "alice@example.com", "old-avatar.png");
        user.setPassword(PASSWORD_ENCODER.encode("different-password"));
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userService.UpdateProfile("user-1", request()));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateProfileRejectsEmailOwnedByAnotherUser() {
        User user = authenticatedUser();
        User otherUser = user("user-2", "Other", "new@example.com", "avatar.png");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.of(otherUser));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userService.UpdateProfile("user-1", request()));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateProfileRejectsNameOwnedByAnotherUser() {
        User user = authenticatedUser();
        User otherUser = user("user-2", "New Name", "other@example.com", "avatar.png");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(userRepository.findByName("New Name")).thenReturn(Optional.of(otherUser));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userService.UpdateProfile("user-1", request()));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }

    private static ProfileUpdateReq request() {
        return new ProfileUpdateReq("new@example.com", "New Name", "new-avatar.png", "password");
    }

    private static User authenticatedUser() {
        User user = user("user-1", "Alice", "alice@example.com", "old-avatar.png");
        user.setPassword(PASSWORD_ENCODER.encode("password"));
        return user;
    }

    private static User user(String id, String name, String email, String avatar) {
        return new User(id, name, email, "password", Role.CLIENT, avatar);
    }
}