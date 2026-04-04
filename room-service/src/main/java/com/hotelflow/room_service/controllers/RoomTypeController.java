package com.hotelflow.room_service.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import com.hotelflow.room_service.models.RoomType;
import com.hotelflow.room_service.repositories.RoomTypeRepository;
import com.hotelflow.room_service.services.RoomTypeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/room-types")
@RequiredArgsConstructor
public class RoomTypeController {

    private final RoomTypeService roomTypeService;
    private final RoomTypeRepository roomTypeRepository;

    // ✅ Create room type
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoomType> createRoom(
            @RequestBody RoomType roomType
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(roomTypeService.create(roomType));
    }

    // ✅ Get by ID
    @GetMapping("/{id}")
    public ResponseEntity<RoomType> getRoomById(@PathVariable String id) {
        return ResponseEntity.ok(roomTypeService.getById(id));
    }

    // ✅ Get all
    @GetMapping
    public ResponseEntity<List<RoomType>> getAllRooms() {
        return ResponseEntity.ok(roomTypeService.getAll());
    }

    // ✅ Update roomtype
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoomType> updateRoomStatus(
            @PathVariable String id,
            @RequestBody RoomType roomType
    ) {
        return ResponseEntity.ok(roomTypeService.update(id, roomType));
    }

    // ✅ Delete roomtype
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRoom(@PathVariable String id) {
        roomTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-name/{name}")
    public ResponseEntity<RoomType> getByName(@PathVariable String name) {
        return ResponseEntity.ok(
            roomTypeRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new RuntimeException("Room type not found: " + name))
        );
    }
}
