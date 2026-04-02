package com.hotelflow.booking_service.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hotelflow.booking_service.dtos.CreateBookingRequest;
import com.hotelflow.booking_service.models.Booking;
import com.hotelflow.booking_service.models.Booking.BookingStatus;
import com.hotelflow.booking_service.services.BookingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    // ─── Create ───────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<Booking> createBooking(
            @RequestBody CreateBookingRequest request,
            @AuthenticationPrincipal String principal,
            Authentication authentication) {

        // if customer, force their own id — they can't book for someone else
        boolean isCustomer = authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER"));

        String customerId = isCustomer ? principal : request.getCustomerId();

        Booking booking = bookingService.createBooking(
                request.getRoomId(),
                customerId,
                request.getCheckIn(),
                request.getCheckOut(),
                request.getNotes()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<Booking> getById(
            @PathVariable String id,
            @AuthenticationPrincipal String principal,
            Authentication authentication) {

        Booking booking = bookingService.getById(id);

        // customer can only view their own booking
        if (hasRole(authentication, "CUSTOMER") && !booking.getCustomerId().equals(principal)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(booking);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<List<Booking>> getAll() {
        return ResponseEntity.ok(bookingService.getAll());
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<List<Booking>> getMyBookings(
            @AuthenticationPrincipal String principal) {
        return ResponseEntity.ok(bookingService.getByCustomerId(principal));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<List<Booking>> getByCustomerId(
            @PathVariable String customerId) {
        return ResponseEntity.ok(bookingService.getByCustomerId(customerId));
    }

    @GetMapping("/room/{roomId}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<List<Booking>> getByRoomId(
            @PathVariable String roomId) {
        return ResponseEntity.ok(bookingService.getByRoomId(roomId));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<List<Booking>> getByStatus(
            @PathVariable BookingStatus status) {
        return ResponseEntity.ok(bookingService.getByStatus(status));
    }

    // ─── Status Update ────────────────────────────────────────────────────────

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'RECEPTIONIST', 'ADMIN')")
    public ResponseEntity<Booking> updateStatus(
            @PathVariable String id,
            @RequestParam BookingStatus status,
            @AuthenticationPrincipal String principal,
            Authentication authentication) {

        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("CUSTOMER");

        return ResponseEntity.ok(
                bookingService.updateStatus(id, status, principal, role)
        );
    }

    // ─── Cancel (customer shortcut) ───────────────────────────────────────────

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Booking> cancel(
            @PathVariable String id,
            @AuthenticationPrincipal String principal) {
        return ResponseEntity.ok(bookingService.cancelBooking(id, principal));
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        bookingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
}