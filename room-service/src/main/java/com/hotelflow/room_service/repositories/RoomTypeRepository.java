package com.hotelflow.room_service.repositories;


import org.springframework.data.jpa.repository.JpaRepository;

import com.hotelflow.room_service.models.RoomType;

public interface RoomTypeRepository extends JpaRepository<RoomType, String> {

}
