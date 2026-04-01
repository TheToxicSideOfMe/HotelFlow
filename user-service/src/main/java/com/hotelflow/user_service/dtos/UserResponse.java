package com.hotelflow.user_service.dtos;

import com.hotelflow.user_service.models.User.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse {
    private String id;
    private String username;
    private String name;
    private String lastName;
    private String email;
    private String phone;
    private UserRole role;
}