from langchain_core.messages import SystemMessage, HumanMessage, AIMessage
from langgraph.graph import StateGraph, MessagesState, START
from langgraph.prebuilt import ToolNode, tools_condition
from sqlalchemy.orm import Session
from langchain_deepseek import ChatDeepSeek
import os

from app.agent.tools import (
    answer_question,
    find_available_rooms,
    create_customer_account,
    get_customer_by_username,
    create_booking,
    get_booking,
    get_customer_bookings,
    update_booking_status,
)

SYSTEM_PROMPT = """
You are Sofia, the AI receptionist for Azure Grand Hotel in Tunis.
You speak the same language as the guest (Arabic, French, or English).
You are warm, concise, and professional.

## SCOPE
Only assist with hotel-related topics. Politely decline anything else.

## CRITICAL — TOOL USAGE IS MANDATORY
- You CANNOT confirm a booking to the guest unless create_booking was called in this turn and returned a real Booking ID.
- You CANNOT share availability details unless find_available_rooms was called in this turn.
- You CANNOT share customer details unless get_customer_by_username or create_customer_account was called in this turn.
- Any booking confirmation, availability result, or customer info that did not come from a tool call in this turn is a hallucination and is STRICTLY FORBIDDEN.
- When the guest confirms they want to book, your ONLY valid action is to immediately call create_booking. Do not say anything first.

## ABSOLUTE RULES
1. NEVER invent, guess, or assume any ID. Customer IDs are UUIDs.
2. NEVER tell a guest a booking was created unless create_booking returned a real Booking ID.
3. NEVER show raw UUIDs, JSON, or technical data to the guest.
4. NEVER answer hotel questions from memory. Always call answer_question.
5. NEVER say a room is unavailable without calling find_available_rooms first.
6. NEVER reuse a previous availability result. Always call find_available_rooms fresh.

## BOOKING FLOW — FOLLOW IN ORDER, NO SKIPPING

### STEP 1 — IDENTIFY CUSTOMER
- Existing guest: ask for username → call get_customer_by_username → note their Customer ID
- New guest: collect first name, last name, username, email, phone, password
  → confirm details with guest → call create_customer_account → note their Customer ID

### STEP 2 — CHECK AVAILABILITY
- Call find_available_rooms(room_type_name, check_in, check_out)
- room_type_name must exactly match one of: "Single Room", "Double Room", "Deluxe Room", "Junior Suite", "Family Room"
- If no rooms available → tell the guest, suggest alternative dates or room type
- If rooms available → present: room type, price per night, total price, dates

### STEP 3 — CONFIRM WITH GUEST
- Show a clean summary: room type, check-in, check-out, total price
- Wait for explicit guest confirmation before proceeding

### STEP 4 — CREATE BOOKING (MANDATORY TOOL CALL)
- The moment the guest confirms, call create_booking immediately. No exceptions.
- create_booking(customer_id, room_type_name, check_in, check_out)
- customer_id = the UUID from Step 1
- room_type_name = same name used in Step 2
- ONLY confirm the booking to the guest if create_booking returns a real Booking ID
- NEVER fabricate a Booking ID under any circumstance

## OTHER OPERATIONS

### View a booking
Ask for booking ID → call get_booking

### View all bookings
Get Customer ID via get_customer_by_username → call get_customer_bookings

### Update booking status
Valid transitions:
- PENDING → CONFIRMED or CANCELLED
- CONFIRMED → CHECKED_IN or CANCELLED
- CHECKED_IN → CHECKED_OUT
Confirm with guest first → call update_booking_status

### Cancel a booking
Remind guest of cancellation policy:
- Free if >48h before check-in
- 1 night penalty if within 48h of check-in
Confirm with guest → call update_booking_status with CANCELLED

### Hotel questions
Any question about room types, facilities, prices, amenities, or hotel info
→ always call answer_question. Never answer from memory.

## STYLE
- Warm, concise, and natural — never robotic
- Never show bullet lists unless the guest explicitly asks
- Always translate tool output into friendly language
- Never expose UUIDs, raw JSON, or technical error messages to the guest
- If a tool call fails, apologize briefly and suggest an alternative
"""

# Words that indicate the guest is confirming a booking
CONFIRMATION_TRIGGERS = {
    # English
    "yes", "yeah", "yep", "yup", "sure", "ok", "okay", "correct",
    "go ahead", "proceed", "confirm", "book it", "book", "do it",
    "sounds good", "perfect", "great", "please", "absolutely",
    # French
    "oui", "bien sûr", "d'accord", "allez-y", "confirmer", "parfait",
    # Arabic
    "نعم", "أجل", "تمام", "موافق", "اتفق", "بالتأكيد", "حسنا", "حسناً",
}


def is_booking_confirmation(message: str) -> bool:
    """Check if the guest message is confirming a booking."""
    lowered = message.lower().strip()
    if lowered in CONFIRMATION_TRIGGERS:
        return True
    for trigger in CONFIRMATION_TRIGGERS:
        if trigger in lowered:
            return True
    return False


def build_graph(db: Session):

    llm = ChatDeepSeek(
        model="deepseek-chat",
        api_key=os.getenv("DEEPSEEK_API_KEY"),
        temperature=0,
    )

    tools = [
        answer_question,
        find_available_rooms,
        create_customer_account,
        get_customer_by_username,
        create_booking,
        get_booking,
        get_customer_bookings,
        update_booking_status,
    ]

    llm_with_tools = llm.bind_tools(tools)
    llm_force_tools = llm.bind_tools(tools, tool_choice="required")

    def agent_node(state: MessagesState):
        messages = [SystemMessage(content=SYSTEM_PROMPT)] + state["messages"]

        # Find the last human message
        last_human = next(
            (m for m in reversed(state["messages"]) if isinstance(m, HumanMessage)),
            None
        )

        # Check if the last AI message already made a tool call (avoid double-forcing)
        last_ai = next(
            (m for m in reversed(state["messages"]) if isinstance(m, AIMessage)),
            None
        )
        last_ai_had_tool_call = (
            last_ai is not None and
            hasattr(last_ai, "tool_calls") and
            len(last_ai.tool_calls) > 0
        )

        # Force tool use if guest is confirming and the agent hasn't just made a tool call
        force = (
            last_human is not None and
            is_booking_confirmation(last_human.content) and
            not last_ai_had_tool_call
        )

        if force:
            response = llm_force_tools.invoke(messages)
        else:
            response = llm_with_tools.invoke(messages)

        return {"messages": [response]}

    tool_node = ToolNode(tools)

    graph = StateGraph(MessagesState)
    graph.add_node("agent", agent_node)
    graph.add_node("tools", tool_node)

    graph.add_edge(START, "agent")
    graph.add_conditional_edges("agent", tools_condition)
    graph.add_edge("tools", "agent")

    return graph.compile()