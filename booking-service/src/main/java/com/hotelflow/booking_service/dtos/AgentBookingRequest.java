package com.hotelflow.booking_service.dtos;

import java.time.LocalDate;

import lombok.Data;

@Data
public class AgentBookingRequest {
    private String customerId;
    private String roomTypeName;  // e.g. "Single", "Junior Suite"
    private LocalDate checkIn;
    private LocalDate checkOut;
    private String notes;
}