package com.hotelflow.user_service.dtos;

import java.time.LocalDateTime;


import com.hotelflow.user_service.models.User;
import com.hotelflow.user_service.models.User.UserRole;

import lombok.Data;


@Data
public class RegisterResponse {
    private String id;
    private String username;
    private String email;
    private String phone;
    private String name;
    private String lastName;
    private UserRole role;
    private LocalDateTime createdAt;

    public static RegisterResponse from(User user) {
        RegisterResponse response = new RegisterResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setRole(user.getRole());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}