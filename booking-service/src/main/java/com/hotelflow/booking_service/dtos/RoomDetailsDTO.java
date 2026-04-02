package com.hotelflow.booking_service.dtos;

import java.math.BigDecimal;


import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomDetailsDTO {
    private String id;
    private String roomNumber;

    @Enumerated(EnumType.STRING)
    private RoomStatus status;
    private BigDecimal pricePerNight;

        public enum RoomStatus {
        AVAILABLE,
        OCCUPIED,
        MAINTENANCE,
        OUT_OF_SERVICE
    }
}
