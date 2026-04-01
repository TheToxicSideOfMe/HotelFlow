package com.hotelflow.room_service.models;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table(name = "rooms")
@NoArgsConstructor
@AllArgsConstructor
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String roomNumber; // "101", "202"
    private int floor;

    @Enumerated(EnumType.STRING)
    private RoomStatus status; // AVAILABLE, OCCUPIED, MAINTENANCE, OUT_OF_SERVICE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id")
    private RoomType roomType;

    private LocalDateTime createdAt;

    
    public enum RoomStatus {
    AVAILABLE,
    OCCUPIED,
    MAINTENANCE,
    OUT_OF_SERVICE
}
}