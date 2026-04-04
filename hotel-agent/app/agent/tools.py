import httpx
import os, re
from langchain_core.tools import tool
from app.agent.auth import get_valid_token
from app.services.rag_service import search_knowledge_base

GATEWAY_URL = os.getenv("GATEWAY_URL", "http://localhost:8080")


def auth_headers() -> dict:
    return {"Authorization": f"Bearer {get_valid_token()}"}


# ── 1. RAG ────────────────────────────────────────────────────────────────────

@tool
async def answer_question(question: str) -> str:
    """Answer hotel-related questions using the knowledge base.
    Use this for any question about hotel facilities, policies, prices, or room types."""
    from app.database import SessionLocal
    db = SessionLocal()
    try:
        context = search_knowledge_base(question, db)
        return context if context else "No relevant information found."
    finally:
        db.close()


# ── 2. Rooms ──────────────────────────────────────────────────────────────────

@tool
async def find_available_rooms(room_type_name: str, check_in: str, check_out: str) -> str:
    """
    Find available rooms for a specific room type and date range.
    room_type_name is the plain name of the room type, e.g. 'Single', 'Double', 'Junior Suite'.
    Dates must be in YYYY-MM-DD format.
    Call this every time availability needs to be checked — never reuse a previous result.
    """
    try:
        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"{GATEWAY_URL}/api/bookings/available-rooms",
                params={
                    "roomTypeName": room_type_name,
                    "checkIn": check_in,
                    "checkOut": check_out
                },
                headers=auth_headers()
            )
            response.raise_for_status()
            rooms = response.json()

            if not rooms:
                return "No rooms available for the selected type and dates."

            lines = []
            for r in rooms:
                lines.append(
                    f"Room Number: {r['roomNumber']} | "
                    f"Price: {r['pricePerNight']} TND/night"
                )
            return "\n".join(lines)
    except Exception as e:
        return f"Could not fetch available rooms: {str(e)}"


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
    """Look up a customer by their username to retrieve their Customer ID."""
    try:
        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"{GATEWAY_URL}/api/users/by-username/{username}",
                headers=auth_headers()
            )
            response.raise_for_status()
            data = response.json()
            return (
                f"Customer found. "
                f"Customer ID: {data['id']} | "
                f"Name: {data['name']} {data['lastName']} | "
                f"Email: {data['email']}"
            )
    except Exception as e:
        return f"Could not find customer: {str(e)}"


# ── 4. Bookings ───────────────────────────────────────────────────────────────

@tool
async def create_booking(
    customer_id: str,
    room_type_name: str,
    check_in: str,
    check_out: str,
    notes: str = ""
) -> str:
    """
    Create a booking for a customer.
    customer_id must be the UUID returned by get_customer_by_username or create_customer_account.
    room_type_name is the plain room type name shown to the guest, e.g. 'Single', 'Double', 'Junior Suite'.
    Dates must be in YYYY-MM-DD format.
    NEVER fabricate a Booking ID — only this tool returns a real one.
    """
    if not is_valid_uuid(customer_id):
        return (
            f"BOOKING FAILED. '{customer_id}' is not a valid customer ID. "
            f"Call get_customer_by_username or create_customer_account to get the real Customer ID."
        )
    try:
        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"{GATEWAY_URL}/api/bookings/agent",
                json={
                    "customerId": customer_id,
                    "roomTypeName": room_type_name,
                    "checkIn": check_in,
                    "checkOut": check_out,
                    "notes": notes
                },
                headers=auth_headers()
            )

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
    """Get details of a specific booking by its ID."""
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
    """Get all bookings for a specific customer by their Customer ID."""
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


# ── Helpers ───────────────────────────────────────────────────────────────────

def is_valid_uuid(value: str) -> bool:
    return bool(re.match(
        r'^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$',
        value.strip().lower()
    ))