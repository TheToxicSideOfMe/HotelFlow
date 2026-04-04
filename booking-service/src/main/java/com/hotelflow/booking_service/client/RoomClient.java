package com.hotelflow.booking_service.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.hotelflow.booking_service.dtos.RoomDetailsDTO;
import com.hotelflow.booking_service.dtos.RoomTypeDTO;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RoomClient {

    private final RestTemplate restTemplate;

    @Value("${room-service.url}")
    private String roomServiceUrl;

    // ─── Room Service Communication ───────────────────────────────────────────

    public RoomDetailsDTO getRoomDetails(String roomId) {
        try {
            return restTemplate.getForObject(
                roomServiceUrl + "/api/rooms/" + roomId + "/details",
                RoomDetailsDTO.class
            );
        } catch (Exception e) {
            e.printStackTrace(); // add this
            throw new RuntimeException("Room not found or Room Service unavailable: " + e.getMessage()); // and this
        }
    }

    public List<RoomDetailsDTO> getRoomsByType(String roomTypeId) {
        try {
            RoomDetailsDTO[] rooms = restTemplate.getForObject(
                roomServiceUrl + "/api/rooms/room-type/" + roomTypeId,
                RoomDetailsDTO[].class
            );
            return rooms != null ? List.of(rooms) : List.of();
        } catch (Exception e) {
            throw new RuntimeException("Could not fetch rooms by type: " + e.getMessage());
        }
    }
    public RoomDetailsDTO getRoomByNumber(String roomNumber) {
        try {
            return restTemplate.getForObject(
                roomServiceUrl + "/api/rooms/by-number/" + roomNumber,
                RoomDetailsDTO.class
            );
        } catch (Exception e) {
            throw new RuntimeException("Room not found: " + e.getMessage());
        }
    }

    public RoomTypeDTO getRoomTypeByName(String name) {
    try {
        return restTemplate.getForObject(
            roomServiceUrl + "/api/room-types/by-name/" + name,
            RoomTypeDTO.class
        );
    } catch (Exception e) {
        throw new RuntimeException("Room type not found: " + e.getMessage());
    }
}
    
}
