package com.hotelflow.room_service.services;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.hotelflow.room_service.models.RoomType;
import com.hotelflow.room_service.repositories.RoomTypeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RoomTypeService {

    
    private final RoomTypeRepository roomTypeRepository;

    public RoomType create(RoomType roomType) {
        roomType.setCreatedAt(LocalDateTime.now());
        return roomTypeRepository.save(roomType);
    }

    public RoomType getById(String id) {
        return roomTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("RoomType not found with id: " + id));
    }

    public List<RoomType> getAll() {
        return roomTypeRepository.findAll();
    }

    public RoomType update(String id, RoomType updated) {
        RoomType existing = getById(id);
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setPricePerNight(updated.getPricePerNight());
        existing.setCapacity(updated.getCapacity());
        existing.setImageUrls(updated.getImageUrls());
        return roomTypeRepository.save(existing);
    }

    public void delete(String id) {
        roomTypeRepository.deleteById(id);
    }
}