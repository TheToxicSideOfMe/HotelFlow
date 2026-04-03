import httpx
import os
from datetime import date
from langchain_core.tools import tool
from app.agent.auth import get_valid_token
from app.services.rag_service import search_knowledge_base

GATEWAY_URL = os.getenv("GATEWAY_URL", "http://localhost:8080")


def auth_headers() -> dict:
    return {"Authorization": f"Bearer {get_valid_token()}"}


# ── 1. RAG ────────────────────────────────────────────────────────────────────

@tool
async def answer_question(question: str) -> str:
    """Answer hotel-related questions using the knowledge base."""
    from app.database import SessionLocal
    db = SessionLocal()
    try:
        context = search_knowledge_base(question, db)
        return context if context else "No relevant information found."
    finally:
        db.close()


# ── 2. Rooms ──────────────────────────────────────────────────────────────────

@tool
async def search_available_rooms() -> str:
    """Get all currently available rooms."""
    try:
        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"{GATEWAY_URL}/api/rooms/status",
                params={"status": "AVAILABLE"},
                headers=auth_headers()
            )
            response.raise_for_status()
            rooms = response.json()
            if not rooms:
                return "No available rooms at the moment."
            
            # Format explicitly so the agent can't confuse room number with room ID
            lines = []
            for r in rooms:
                lines.append(
                    f"Room ID (use this for booking): {r['id']} | "
                    f"Room Number: {r['roomNumber']} | "
                    f"Type: {r.get('roomType', {}).get('name', 'N/A')} | "
                    f"Price: {r.get('roomType', {}).get('pricePerNight', 'N/A')} TND/night | "
                    f"Capacity: {r.get('roomType', {}).get('capacity', 'N/A')} guests"
                )
            return "\n".join(lines)
    except Exception as e:
        return f"Could not fetch available rooms: {str(e)}"


@tool
async def get_room_type_details(room_type_id: str) -> str:
    """Get details for a specific room type by ID."""
    try:
        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"{GATEWAY_URL}/api/room-types/{room_type_id}",
                headers=auth_headers()
            )
            response.raise_for_status()
            return str(response.json())
    except Exception as e:
        return f"Could not fetch room type details: {str(e)}"

@tool
async def check_room_availability(
    room_id: str,
    check_in: str,
    check_out: str
) -> str:
    """Check if a room is available for specific dates before booking. Dates must be YYYY-MM-DD format."""
    try:
        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"{GATEWAY_URL}/api/bookings/room/{room_id}",
                headers=auth_headers()
            )
            response.raise_for_status()
            bookings = response.json()

            requested_in = date.fromisoformat(check_in)
            requested_out = date.fromisoformat(check_out)

            for b in bookings:
                if b["status"] in ("CANCELLED", "CHECKED_OUT"):
                    continue
                existing_in = date.fromisoformat(b["checkIn"])
                existing_out = date.fromisoformat(b["checkOut"])

                # overlap condition
                if requested_in < existing_out and requested_out > existing_in:
                    return (
                        f"Room is not available. It is already booked "
                        f"from {b['checkIn']} to {b['checkOut']} (status: {b['status']})."
                    )

            return f"Room is available from {check_in} to {check_out}. You can proceed with booking."

    except Exception as e:
        return f"Could not check room availability: {str(e)}"
    


# ── 3. Customers ──────────────────────────────────────────────────────────────

@tool
async def create_customer_account(
    username: str,
    email: str,
    password: str,
    name: str,
    last_name: str,
    phone: str
) -> str:
    """Create a new customer account."""
    try:
        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"{GATEWAY_URL}/api/auth/register",
                json={
                    "username": username,
                    "email": email,
                    "password": password,
                    "name": name,
                    "lastName": last_name,
                    "phone": phone
                }
            )
            response.raise_for_status()
            data = response.json()
            return f"Account created successfully. Customer ID: {data['id']}, username: {data['username']}."
    except Exception as e:
        return f"Could not create customer account: {str(e)}"


@tool
async def get_customer_by_username(username: str) -> str:
    """Look up a customer by their username to retrieve their ID."""
    try:
        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"{GATEWAY_URL}/api/users/by-username/{username}",
                headers=auth_headers()
            )
            response.raise_for_status()
            data = response.json()
            return f"Customer found. ID: {data['id']}, name: {data['name']} {data['lastName']}, email: {data['email']}."
    except Exception as e:
        return f"Could not find customer: {str(e)}"


# ── 4. Bookings ───────────────────────────────────────────────────────────────

@tool
async def create_booking(
    room_id: str,
    customer_id: str,
    check_in: str,
    check_out: str,
    notes: str = ""
) -> str:
    """Create a booking for a customer. Dates must be in YYYY-MM-DD format."""
    try:
        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"{GATEWAY_URL}/api/bookings",
                json={
                    "roomId": room_id,
                    "customerId": customer_id,
                    "checkIn": check_in,
                    "checkOut": check_out,
                    "notes": notes
                },
                headers=auth_headers()
            )

            # Handle every non-2xx explicitly
            if not response.is_success:
                try:
                    error_msg = response.json().get("error", response.text)
                except Exception:
                    error_msg = response.text
                return (
                    f"BOOKING FAILED. Do not tell the guest the booking was successful. "
                    f"HTTP {response.status_code}: {error_msg}"
                )

            data = response.json()
            
            # Verify the response actually contains a booking ID
            if "id" not in data:
                return (
                    "BOOKING FAILED. Do not tell the guest the booking was successful. "
                    "The server did not return a booking confirmation."
                )

            return (
                f"Booking created successfully. "
                f"Booking ID: {data['id']}, "
                f"check-in: {data['checkIn']}, "
                f"check-out: {data['checkOut']}, "
                f"total price: {data['totalPrice']} TND, "
                f"status: {data['status']}."
            )
    except Exception as e:
        return (
            f"BOOKING FAILED. Do not tell the guest the booking was successful. "
            f"Error: {str(e)}"
        )


@tool
async def get_booking(booking_id: str) -> str:
    """Get details of a specific booking by ID."""
    try:
        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"{GATEWAY_URL}/api/bookings/{booking_id}",
                headers=auth_headers()
            )
            response.raise_for_status()
            data = response.json()
            return (
                f"Booking ID: {data['id']}, "
                f"room: {data['roomId']}, "
                f"customer: {data['customerId']}, "
                f"check-in: {data['checkIn']}, "
                f"check-out: {data['checkOut']}, "
                f"status: {data['status']}, "
                f"total: {data['totalPrice']} TND, "
                f"notes: {data.get('notes', 'none')}."
            )
    except Exception as e:
        return f"Could not fetch booking: {str(e)}"


@tool
async def get_customer_bookings(customer_id: str) -> str:
    """Get all bookings for a specific customer."""
    try:
        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"{GATEWAY_URL}/api/bookings/customer/{customer_id}",
                headers=auth_headers()
            )
            response.raise_for_status()
            bookings = response.json()
            if not bookings:
                return "No bookings found for this customer."
            result = []
            for b in bookings:
                result.append(
                    f"- Booking {b['id']}: {b['checkIn']} → {b['checkOut']}, "
                    f"status: {b['status']}, total: {b['totalPrice']} TND"
                )
            return "\n".join(result)
    except Exception as e:
        return f"Could not fetch customer bookings: {str(e)}"


@tool
async def cancel_booking(booking_id: str) -> str:
    """Cancel a booking by ID."""
    try:
        async with httpx.AsyncClient() as client:
            response = await client.patch(
                f"{GATEWAY_URL}/api/bookings/{booking_id}/cancel",
                headers=auth_headers()
            )
            response.raise_for_status()
            return f"Booking {booking_id} has been successfully cancelled."
    except Exception as e:
        return f"Could not cancel booking: {str(e)}"


@tool
async def update_booking_status(booking_id: str, status: str) -> str:
    """Update booking status. Valid values: PENDING, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED."""
    valid_statuses = {"PENDING", "CONFIRMED", "CHECKED_IN", "CHECKED_OUT", "CANCELLED"}
    if status.upper() not in valid_statuses:
        return f"Invalid status '{status}'. Must be one of: {', '.join(valid_statuses)}."
    try:
        async with httpx.AsyncClient() as client:
            response = await client.patch(
                f"{GATEWAY_URL}/api/bookings/{booking_id}/status",
                params={"status": status.upper()},
                headers=auth_headers()
            )
            response.raise_for_status()
            return f"Booking {booking_id} status updated to {status.upper()}."
    except Exception as e:
        return f"Could not update booking status: {str(e)}"