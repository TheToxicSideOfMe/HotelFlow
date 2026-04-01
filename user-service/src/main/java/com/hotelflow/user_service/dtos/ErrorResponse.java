package com.hotelflow.user_service.dtos;


import java.time.LocalDateTime;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    
    // Custom constructor that auto-sets timestamp
    public ErrorResponse(int status, String error, String message, String path) {
        this.timestamp = LocalDateTime.now();  // ← Auto-set timestamp
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }
}