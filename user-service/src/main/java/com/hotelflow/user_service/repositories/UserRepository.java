package com.hotelflow.user_service.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hotelflow.user_service.models.User;

public interface UserRepository extends JpaRepository<User,String>{
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
}
