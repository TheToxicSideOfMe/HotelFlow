package com.hotelflow.booking_service.repositories;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.hotelflow.booking_service.models.Booking;
import com.hotelflow.booking_service.models.Booking.BookingStatus;

@Repository
public interface BookingRepository extends JpaRepository<Booking,String> {
    List<Booking>findByCustomerId(String id);
    List<Booking>findByStatus(BookingStatus status);
    List<Booking>findByRoomId(String id);


    @Query("""
        SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
        FROM Booking b
        WHERE b.roomId = :roomId
          AND b.status <> 'CANCELLED'
          AND (
               (:checkIn < b.checkOut) AND (:checkOut > b.checkIn)
          )
    """)
    Boolean existsOverlappingBooking(
        String roomId,
        LocalDate checkIn,
        LocalDate checkOut
    );
    
    @Query("""
        SELECT b.roomId FROM Booking b
        WHERE b.status NOT IN ('CANCELLED', 'CHECKED_OUT')
        AND b.checkIn < :checkOut
        AND b.checkOut > :checkIn
    """)
    List<String> findBookedRoomIds(
        @Param("checkIn") LocalDate checkIn,
        @Param("checkOut") LocalDate checkOut
    );
}
