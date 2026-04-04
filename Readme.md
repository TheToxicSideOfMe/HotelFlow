# HotelFlow 🏨

An AI-powered hotel management system built on a microservices architecture. Guests interact with **Sofia**, an intelligent receptionist agent via Telegram, who can answer questions, check room availability, and create real bookings — all backed by a production-grade Spring Boot backend.

---

## Architecture Overview

```
Telegram
    │
    ▼
┌─────────────────┐
│   hotel-agent   │  FastAPI · LangGraph · RAG · DeepSeek
│   (AI Agent)    │
└────────┬────────┘
         │ JWT
         ▼
┌─────────────────┐
│   api-gateway   │  Spring Boot · JWT validation · routing
└────────┬────────┘
         │
    ┌────┴─────┬──────────────┐
    ▼          ▼              ▼
booking-   room-service   user-service
service    Spring Boot    Spring Boot
Spring     Rooms &        Auth &
Boot       Room Types     User mgmt
    │          │
    └────┬─────┘
         ▼
    PostgreSQL
    + pgvector
```

---

## Services

| Service | Stack | Responsibility |
|---|---|---|
| `api-gateway` | Spring Boot | JWT validation, request routing to all services |
| `booking-service` | Spring Boot | Bookings, availability, status transitions |
| `room-service` | Spring Boot | Rooms, room types, availability queries |
| `user-service` | Spring Boot | User registration, authentication, JWT issuance |
| `hotel-agent` | FastAPI + LangGraph | AI agent, RAG knowledge base, Telegram bot |

---

## The AI Agent — Sofia

Sofia is a multilingual hotel receptionist (Arabic, French, English) built with **LangGraph** and **DeepSeek**. She handles the full guest journey through a set of real tool calls — nothing is hardcoded or simulated.

### Tools
| Tool | Description |
|---|---|
| `answer_question` | RAG-powered Q&A over the hotel knowledge base |
| `find_available_rooms` | Queries live availability by room type and dates |
| `get_customer_by_username` | Fetches a customer's ID from user-service |
| `create_customer_account` | Registers a new guest account |
| `create_booking` | Creates a real booking via booking-service |
| `get_booking` | Retrieves booking details by ID |
| `get_customer_bookings` | Lists all bookings for a customer |
| `update_booking_status` | Manages status transitions (PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT) |

### Booking Flow
```
1. Identify customer (username lookup or new account)
        ↓
2. Check availability (room type name + dates)
        ↓
3. Present summary & wait for confirmation
        ↓
4. Create booking (customer ID + room type name)
   Backend resolves the room automatically — no UUIDs touch the LLM
```

### RAG
The knowledge base (hotel info, room types, policies, pricing) is embedded using local embeddings and stored in **pgvector**. Sofia calls `answer_question` for any hotel-related query rather than relying on model memory.

---

## Tech Stack

**Backend**
- Java 17 · Spring Boot 3 · Spring Security · JWT
- RestTemplate for inter-service communication
- PostgreSQL · pgvector
- JPA / Hibernate

**AI Agent**
- Python · FastAPI
- LangGraph · LangChain
- DeepSeek
- pgvector + local embeddings for RAG
- python-telegram-bot

---

## Key Design Decisions

**Agent-specific booking endpoint** — Instead of passing room UUIDs through the LLM (which caused hallucinations), the backend exposes a `/api/bookings/agent` endpoint that accepts a room type name and resolves the actual room internally. The LLM only ever handles human-readable names.

**Forced tool calling** — On booking confirmation, LangGraph forces a tool call using `tool_choice="required"`, preventing the model from fabricating responses without actually calling `create_booking`.

**JWT flowing through the agent** — The hotel-agent authenticates as a service account and includes a valid JWT on every request to the gateway, keeping the same auth contract as the rest of the system.

---

## Screenshots

<img width="737" height="816" alt="Screenshot_20260404_162618" src="https://github.com/user-attachments/assets/2dc3bd74-1e6b-4c2e-a5bb-d1af4ce720bc" />
<img width="713" height="692" alt="Screenshot_20260404_163024" src="https://github.com/user-attachments/assets/d22dec25-12aa-480e-b86a-d31c73a66f82" />
<img width="727" height="679" alt="Screenshot_20260404_162719" src="https://github.com/user-attachments/assets/53d13cc1-b4c8-4a3a-83a9-1b01971c4f71" />

---

## License

MIT
