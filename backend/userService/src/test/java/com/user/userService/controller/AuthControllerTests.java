package com.user.userService.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.user.userService.dto.LoginReq;
import com.user.userService.dto.LoginRes;
import com.user.userService.dto.RegisterReq;
import com.user.userService.dto.RegisterRes;
import com.user.userService.enums.Role;
import com.user.userService.service.AuthService;

@ExtendWith(MockitoExtension.class)
class AuthControllerTests {
    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Test
    void registerDelegatesRequestAndReturnsServiceResponse() {
        RegisterReq request = registerRequest();
        RegisterRes response = new RegisterRes("user-1", "Alice", "alice@example.com");
        when(authService.register(request)).thenReturn(response);

        assertEquals(response, authController.Register(request));
        verify(authService).register(request);
    }

    @Test
    void loginWrapsServiceTokenInResponse() {
        LoginReq request = new LoginReq("alice@example.com", "password");
        when(authService.login(request)).thenReturn("jwt-token");

        LoginRes response = authController.Login(request);

        assertEquals("jwt-token", response.token());
        verify(authService).login(request);
    }

    private static RegisterReq registerRequest() {
        return new RegisterReq("alice@example.com", "Alice", "password", Role.CLIENT);
    }
}