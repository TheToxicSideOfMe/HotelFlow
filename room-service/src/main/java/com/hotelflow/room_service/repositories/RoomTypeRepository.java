package com.hotelflow.room_service.repositories;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hotelflow.room_service.models.RoomType;

public interface RoomTypeRepository extends JpaRepository<RoomType, String> {
    Optional<RoomType> findByNameIgnoreCase(String name);
}
