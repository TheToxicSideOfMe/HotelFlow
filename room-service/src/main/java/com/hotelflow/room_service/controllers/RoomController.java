package com.hotelflow.room_service.controllers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hotelflow.room_service.dtos.RoomDetailsDTO;
import com.hotelflow.room_service.models.Room;
import com.hotelflow.room_service.models.Room.RoomStatus;
import com.hotelflow.room_service.repositories.RoomRepository;
import com.hotelflow.room_service.services.RoomService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final RoomRepository roomRepository;

    // ✅ Create room
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Room> createRoom(
            @RequestBody Room room,
            @RequestParam String roomTypeId
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(roomService.create(room, roomTypeId));
    }
    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Room>> createRooms(
            @RequestBody List<Room> rooms,
            @RequestParam String roomTypeId
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(roomService.createAll(rooms, roomTypeId));
    }

    // ✅ Get by ID
    @GetMapping("/{id}")
    public ResponseEntity<Room> getRoomById(@PathVariable String id) {
        return ResponseEntity.ok(roomService.getById(id));
    }

    // ✅ Get all
    @GetMapping
    public ResponseEntity<List<Room>> getAllRooms() {
        return ResponseEntity.ok(roomService.getAll());
    }

    // ✅ Get by status
    @GetMapping("/status")
    public ResponseEntity<List<Room>> getRoomsByStatus(
            @RequestParam RoomStatus status
    ) {
        return ResponseEntity.ok(roomService.getByStatus(status));
    }

    // ✅ Get by room type
    @GetMapping("/room-type/{roomTypeId}")
    public ResponseEntity<List<RoomDetailsDTO>> getRoomsByRoomType(
            @PathVariable String roomTypeId
    ) {
        return ResponseEntity.ok(
            roomService.getByRoomType(roomTypeId)
                .stream()
                .map(room -> new RoomDetailsDTO(
                    room.getId(),
                    room.getRoomNumber(),
                    room.getStatus(),
                    room.getRoomType().getPricePerNight()
                ))
                .collect(Collectors.toList())
        );
    }
    @GetMapping("/by-number/{roomNumber}")
    public ResponseEntity<RoomDetailsDTO> getRoomByNumber(@PathVariable String roomNumber) {
        Room room = roomRepository.findByRoomNumber(roomNumber)
                .orElseThrow(() -> new RuntimeException("Room not found: " + roomNumber));
        return ResponseEntity.ok(new RoomDetailsDTO(
            room.getId(),
            room.getRoomNumber(),
            room.getStatus(),
            room.getRoomType().getPricePerNight()
        ));
    }

    // ✅ Update status
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Room> updateRoomStatus(
            @PathVariable String id,
            @RequestParam RoomStatus status
    ) {
        return ResponseEntity.ok(roomService.updateStatus(id, status));
    }

    @GetMapping("/{id}/details")
    public ResponseEntity<RoomDetailsDTO> getRoomDetails(@PathVariable String id) {
        Room room = roomService.getById(id);
        return ResponseEntity.ok(new RoomDetailsDTO(
            room.getId(),
            room.getRoomNumber(),
            room.getStatus(),
            room.getRoomType().getPricePerNight()
        ));
    }

    // ✅ Update room
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Room> updateRoom(
            @PathVariable String id,
            @RequestBody Room updatedRoom,
            @RequestParam String roomTypeId
    ) {
        return ResponseEntity.ok(roomService.update(id, updatedRoom, roomTypeId));
    }

    // ✅ Delete room
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRoom(@PathVariable String id) {
        roomService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
