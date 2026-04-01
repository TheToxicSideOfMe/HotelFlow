package com.hotelflow.user_service.dtos;


import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;
    @JsonIgnore
    private String refreshToken;
    private String tokenType="Bearer";
    private Long expiresIn;
    private RegisterResponse user;

}
