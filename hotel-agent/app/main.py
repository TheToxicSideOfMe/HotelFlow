from fastapi import FastAPI
from app.database import engine, Base
from app.api import chat
from app.services.rag_service import load_hotel_data
from app.agent.auth import login, token_refresh_loop
from app.api.telegram import build_telegram_app
from contextlib import asynccontextmanager
from sqlalchemy.orm import Session
from app.database import SessionLocal
import asyncio

@asynccontextmanager
async def lifespan(app: FastAPI):
    Base.metadata.create_all(bind=engine)

    db: Session = SessionLocal()
    try:
        load_hotel_data(db)
        print("✅ Hotel knowledge base loaded")
    finally:
        db.close()

    await login()
    asyncio.create_task(token_refresh_loop())

    # Start Telegram bot
    telegram_app = build_telegram_app()
    await telegram_app.initialize()
    await telegram_app.start()
    await telegram_app.updater.start_polling()
    print("✅ Telegram bot started")

    yield

    # Graceful shutdown
    await telegram_app.updater.stop()
    await telegram_app.stop()
    await telegram_app.shutdown()

app = FastAPI(lifespan=lifespan)
app.include_router(chat.router, prefix="/api")