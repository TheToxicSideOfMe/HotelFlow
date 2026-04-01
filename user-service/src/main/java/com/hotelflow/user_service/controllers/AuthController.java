package com.hotelflow.user_service.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hotelflow.user_service.dtos.LoginRequest;
import com.hotelflow.user_service.dtos.LoginResponse;
import com.hotelflow.user_service.dtos.LogoutRequest;
import com.hotelflow.user_service.dtos.RegisterRequest;
import com.hotelflow.user_service.dtos.RegisterResponse;
import com.hotelflow.user_service.services.AuthService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private AuthService authService;
    
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> registerUser(@Valid @RequestBody RegisterRequest request){
        RegisterResponse user =authService.registerUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
        @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginUser(@Valid @RequestBody LoginRequest request, HttpServletResponse response){
        LoginResponse loginResponse = authService.loginUser(request);
                // Controller only handles HTTP-specific stuff (cookies)
        Cookie refreshTokenCookie = new Cookie("refreshToken", loginResponse.getRefreshToken());
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(false); // Set to true in production with HTTPS
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        response.addCookie(refreshTokenCookie);
        
        return ResponseEntity.ok(loginResponse);

    }

    @PostMapping("/logout")
    public ResponseEntity<String> logoutUser(@RequestBody LogoutRequest request){
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok("Logout successfully");
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshTokens(@CookieValue(name = "refreshToken") String token, HttpServletResponse response){
        LoginResponse loginResponse = authService.refreshTokens(token);
        Cookie refreshTokenCookie = new Cookie("refreshToken", loginResponse.getRefreshToken());
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(false);
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(7 * 24 * 60 * 60);
        response.addCookie(refreshTokenCookie);
        return ResponseEntity.ok(loginResponse);
    }
}
