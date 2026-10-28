package com.user.userService.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import com.user.userService.dto.ProfileUpdateReq;
import com.user.userService.dto.UserRes;
import com.user.userService.enums.Role;
import com.user.userService.service.UserService;

@ExtendWith(MockitoExtension.class)
class UserControllerTests {
    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserController userController;

    @Test
    void getMeUsesAuthenticatedUserId() {
        UserRes response = response();
        when(authentication.getName()).thenReturn("user-1");
        when(userService.GetMe("user-1")).thenReturn(response);

        assertEquals(response, userController.GetMe(authentication));
        verify(userService).GetMe("user-1");
    }

    @Test
    void updateProfileUsesAuthenticatedUserIdAndRequest() {
        ProfileUpdateReq request = request();
        UserRes response = response();
        when(authentication.getName()).thenReturn("user-1");
        when(userService.UpdateProfile("user-1", request)).thenReturn(response);

        assertEquals(response, userController.UpdateProfile(authentication, request));
        verify(userService).UpdateProfile("user-1", request);
    }

    private static ProfileUpdateReq request() {
        return new ProfileUpdateReq("new@example.com", "New Name", "avatar.png", "password");
    }

    private static UserRes response() {
        return new UserRes("user-1", "New Name", "new@example.com", "avatar.png", Role.CLIENT);
    }
}