package com.hotelflow.user_service.services;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.hotelflow.user_service.models.RefreshToken;
import com.hotelflow.user_service.models.User;
import com.hotelflow.user_service.repositories.RefreshTokenRepository;

import jakarta.transaction.Transactional;

@Service
public class RefreshTokenService {
    
    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;
    
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    
    @Transactional
    public void saveRefreshToken(User user, String token) {
        // Delete old refresh tokens for this user
        refreshTokenRepository.deleteByUser(user);
        
        // Calculate expiry date
        Instant expiryDate = Instant.now().plusMillis(refreshTokenExpiration);
        
        // Save new refresh token
        RefreshToken refreshToken = new RefreshToken(token, user, expiryDate);
        refreshTokenRepository.save(refreshToken);
    }
}
