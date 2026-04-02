package com.hotelflow.booking_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.hotelflow.booking_service.dtos.RoomDetailsDTO;

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
            throw new RuntimeException("Room not found or Room Service unavailable");
        }
    }
}
