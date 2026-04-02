package com.hotelflow.booking_service.dtos;

import java.time.LocalDate;

import lombok.Data;

@Data
public class CreateBookingRequest {
    private String roomId;
    private String customerId; // only used by RECEPTIONIST/ADMIN
    private LocalDate checkIn;
    private LocalDate checkOut;
    private String notes;
}