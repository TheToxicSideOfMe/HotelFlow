package com.hotelflow.booking_service.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table(name = "bookings")
@NoArgsConstructor
@AllArgsConstructor
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String roomId;
    private String customerId; // UUID string from User Service

    private LocalDate checkIn;
    private LocalDate checkOut;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    private BigDecimal totalPrice;

    private String notes; // receptionist notes

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum BookingStatus {
        PENDING,
        CONFIRMED,
        CHECKED_IN,
        CHECKED_OUT,
        CANCELLED
    }
}