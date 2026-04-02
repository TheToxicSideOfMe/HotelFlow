package com.hotelflow.booking_service.services;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;

import com.hotelflow.booking_service.client.RoomClient;
import com.hotelflow.booking_service.dtos.RoomDetailsDTO;
import com.hotelflow.booking_service.dtos.RoomDetailsDTO.RoomStatus;
import com.hotelflow.booking_service.models.Booking;
import com.hotelflow.booking_service.models.Booking.BookingStatus;
import com.hotelflow.booking_service.repositories.BookingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RoomClient roomClient;

    private boolean isRoomAvailable(String roomId, LocalDate checkIn, LocalDate checkOut) {
        return !bookingRepository.existsOverlappingBooking(roomId, checkIn, checkOut);
    }

    // ─── Create ───────────────────────────────────────────────────────────────

    public Booking createBooking(String roomId, String customerId, LocalDate checkIn, LocalDate checkOut, String notes) {

        if (!checkOut.isAfter(checkIn)) {
            throw new RuntimeException("Check-out must be after check-in");
        }

        RoomDetailsDTO room = roomClient.getRoomDetails(roomId);

        if (room.getStatus() == RoomStatus.MAINTENANCE || room.getStatus() == RoomStatus.OUT_OF_SERVICE) {
            throw new RuntimeException("Room is not available for booking");
        }

        if (!isRoomAvailable(roomId, checkIn, checkOut)) {
            throw new RuntimeException("Room is already booked for the selected dates");
        }

        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        BigDecimal totalPrice = room.getPricePerNight().multiply(BigDecimal.valueOf(nights));

        Booking booking = new Booking();
        booking.setRoomId(roomId);
        booking.setCustomerId(customerId);
        booking.setCheckIn(checkIn);
        booking.setCheckOut(checkOut);
        booking.setStatus(BookingStatus.PENDING);
        booking.setTotalPrice(totalPrice);
        booking.setNotes(notes);
        booking.setCreatedAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());

        return bookingRepository.save(booking);
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    public Booking getById(String id) {
        return bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found with id: " + id));
    }

    public List<Booking> getAll() {
        return bookingRepository.findAll();
    }

    public List<Booking> getByCustomerId(String customerId) {
        return bookingRepository.findByCustomerId(customerId);
    }

    public List<Booking> getByStatus(BookingStatus status) {
        return bookingRepository.findByStatus(status);
    }

    public List<Booking> getByRoomId(String roomId) {
        return bookingRepository.findByRoomId(roomId);
    }

    // ─── Status Updates ───────────────────────────────────────────────────────

    public Booking updateStatus(String id, BookingStatus newStatus, String requesterId, String requesterRole) {
        Booking booking = getById(id);

        validateStatusTransition(booking.getStatus(), newStatus, requesterRole);

        // customer can only cancel their own booking
        if (requesterRole.equals("CUSTOMER")) {
            if (!booking.getCustomerId().equals(requesterId)) {
                throw new RuntimeException("You can only cancel your own bookings");
            }
        }

        booking.setStatus(newStatus);
        booking.setUpdatedAt(LocalDateTime.now());
        return bookingRepository.save(booking);
    }

    private void validateStatusTransition(BookingStatus current, BookingStatus next, String role) {
        if (role.equals("CUSTOMER")) {
            // customer can only cancel a PENDING booking
            if (next != BookingStatus.CANCELLED) {
                throw new RuntimeException("Customers can only cancel bookings");
            }
            if (current != BookingStatus.PENDING) {
                throw new RuntimeException("Only PENDING bookings can be cancelled by customers");
            }
            return;
        }

        // receptionist / admin flow
        boolean valid = switch (current) {
            case PENDING    -> next == BookingStatus.CONFIRMED || next == BookingStatus.CANCELLED;
            case CONFIRMED  -> next == BookingStatus.CHECKED_IN || next == BookingStatus.CANCELLED;
            case CHECKED_IN -> next == BookingStatus.CHECKED_OUT;
            default         -> false;
        };

        if (!valid) {
            throw new RuntimeException("Invalid status transition: " + current + " → " + next);
        }
    }

    // ─── Cancel (customer shortcut) ───────────────────────────────────────────

    public Booking cancelBooking(String id, String customerId) {
        Booking booking = getById(id);

        if (!booking.getCustomerId().equals(customerId)) {
            throw new RuntimeException("You can only cancel your own bookings");
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("Only PENDING bookings can be cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setUpdatedAt(LocalDateTime.now());
        return bookingRepository.save(booking);
    }

    // ─── Delete (admin only) ──────────────────────────────────────────────────

    public void delete(String id) {
        bookingRepository.deleteById(id);
    }
}