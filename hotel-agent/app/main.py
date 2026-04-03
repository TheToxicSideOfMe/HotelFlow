from fastapi import FastAPI
from app.database import engine, Base
from app.api import chat
from app.services.rag_service import load_hotel_data
from contextlib import asynccontextmanager
from sqlalchemy.orm import Session
from app.database import SessionLocal

@asynccontextmanager
async def lifespan(app: FastAPI):
    Base.metadata.create_all(bind=engine)
    db: Session = SessionLocal()
    try:
        load_hotel_data(db)
        print("✅ Hotel knowledge base loaded")
    finally:
        db.close()
    yield

app = FastAPI(lifespan=lifespan)

app.include_router(chat.router, prefix="/api")