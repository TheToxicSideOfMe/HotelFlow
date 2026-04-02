package com.hotelflow.room_service.dtos;

import java.math.BigDecimal;

import com.hotelflow.room_service.models.Room.RoomStatus;

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
}
