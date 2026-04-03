from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from sqlalchemy.orm import Session
from langchain_core.messages import HumanMessage, AIMessage
from langchain_ollama import ChatOllama
from app.database import get_db
from app.models.conversation import Conversation
from app.services.rag_service import search_knowledge_base
from langchain_openai import ChatOpenAI
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



llm = ChatOpenAI(
    model="deepseek-chat",
    api_key=os.getenv("DEEPSEEK_API_KEY"),
    base_url="https://api.deepseek.com"
)

@router.post("/chat", response_model=ChatResponse)
def chat(request: ChatRequest, db: Session = Depends(get_db)):

    # 1. Load or create conversation
    conversation = db.query(Conversation).filter(
        Conversation.session_id == request.session_id
    ).first()

    if not conversation:
        conversation = Conversation(session_id=request.session_id, messages="[]")
        db.add(conversation)
        db.commit()
        db.refresh(conversation)

    # 2. Load history
    history = deserialize_messages(conversation.messages)

    # 3. RAG — retrieve relevant context
    context = search_knowledge_base(request.message, db)

    # 4. Build prompt with context injected
    system_prompt = f"""You are a friendly and professional hotel receptionist at the Azure Grand Hotel in Tunis.
    Greet guests warmly, respond naturally to small talk, and answer hotel-related questions using the information below.
    Only say you don't have information if the guest asks something specific about the hotel that isn't covered.
    
    Hotel Knowledge:
    {context}"""

    messages = [
        ("system", system_prompt),
        *[(("human" if isinstance(m, HumanMessage) else "ai"), m.content) for m in history],
        ("human", request.message)
    ]

    # 5. Call LLM
    try:
        response = llm.invoke(messages)
        ai_response = response.content
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"LLM error: {str(e)}")

    # 6. Save history
    history.append(HumanMessage(content=request.message))
    history.append(AIMessage(content=ai_response))
    conversation.messages = serialize_messages(history)
    db.commit()

    return ChatResponse(session_id=request.session_id, response=ai_response)