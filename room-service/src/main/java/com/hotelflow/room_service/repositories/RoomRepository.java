package com.hotelflow.room_service.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hotelflow.room_service.models.Room;
import com.hotelflow.room_service.models.Room.RoomStatus;

public interface RoomRepository extends JpaRepository<Room,String> {
    List<Room> findByStatus(RoomStatus status);
    List<Room> findByRoomTypeId(String roomTypeId);
    Optional<Room> findByRoomNumber(String roomNumber);
}
