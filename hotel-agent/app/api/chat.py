from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from sqlalchemy.orm import Session
from langchain_core.messages import HumanMessage, AIMessage
from langchain_openai import ChatOpenAI
from app.database import get_db
from app.models.conversation import Conversation
from app.agent.graph import build_graph
import json
import os

router = APIRouter()

class ChatRequest(BaseModel):
    session_id: str
    message: str

class ChatResponse(BaseModel):
    session_id: str
    response: str

def serialize_messages(messages: list) -> str:
    serialized = []
    for m in messages:
        if isinstance(m, HumanMessage):
            serialized.append({"role": "human", "content": m.content})
        elif isinstance(m, AIMessage):
            serialized.append({"role": "ai", "content": m.content})
    return json.dumps(serialized)

def deserialize_messages(raw: str) -> list:
    messages = []
    for m in json.loads(raw):
        if m["role"] == "human":
            messages.append(HumanMessage(content=m["content"]))
        elif m["role"] == "ai":
            messages.append(AIMessage(content=m["content"]))
    return messages

@router.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest, db: Session = Depends(get_db)):

    conversation = db.query(Conversation).filter(
        Conversation.session_id == request.session_id
    ).first()

    if not conversation:
        conversation = Conversation(session_id=request.session_id, messages="[]")
        db.add(conversation)
        db.commit()
        db.refresh(conversation)

    history = deserialize_messages(conversation.messages)
    history.append(HumanMessage(content=request.message))

    try:
        graph = build_graph(db)
        result = await graph.ainvoke({"messages": history})
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Agent error: {str(e)}")

    final_messages = result["messages"]
    ai_response = next(
        (m.content for m in reversed(final_messages) if isinstance(m, AIMessage)),
        "Sorry, I couldn't generate a response."
    )

    clean_history = [m for m in final_messages if isinstance(m, (HumanMessage, AIMessage))]
    conversation.messages = serialize_messages(clean_history)
    db.commit()

    return ChatResponse(session_id=request.session_id, response=ai_response)