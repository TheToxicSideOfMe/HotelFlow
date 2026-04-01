package com.hotelflow.room_service.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.hotelflow.room_service.models.Room;
import com.hotelflow.room_service.models.RoomType;
import com.hotelflow.room_service.models.Room.RoomStatus;
import com.hotelflow.room_service.repositories.RoomRepository;
import com.hotelflow.room_service.repositories.RoomTypeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomTypeRepository roomTypeRepository;

    public Room create(Room room, String roomTypeId) {
        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new RuntimeException("RoomType not found with id: " + roomTypeId));
        room.setRoomType(roomType);
        room.setStatus(RoomStatus.AVAILABLE);
        room.setCreatedAt(LocalDateTime.now());
        return roomRepository.save(room);
    }

    public Room getById(String id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found with id: " + id));
    }

    public List<Room> getAll() {
        return roomRepository.findAll();
    }

    public List<Room> getByStatus(RoomStatus status) {
        return roomRepository.findByStatus(status);
    }

    public List<Room> getByRoomType(Long roomTypeId) {
        return roomRepository.findByRoomTypeId(roomTypeId);
    }

    public Room updateStatus(String id, RoomStatus status) {
        Room room = getById(id);
        room.setStatus(status);
        return roomRepository.save(room);
    }

    public Room update(String id, Room updated, String roomTypeId) {
        Room existing = getById(id);
        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new RuntimeException("RoomType not found with id: " + roomTypeId));
        existing.setRoomNumber(updated.getRoomNumber());
        existing.setFloor(updated.getFloor());
        existing.setRoomType(roomType);
        return roomRepository.save(existing);
    }

    public void delete(String id) {
        roomRepository.deleteById(id);
    }
}