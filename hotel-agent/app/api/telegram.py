import os
import logging
from telegram import Update
from telegram.ext import Application, MessageHandler, filters, ContextTypes
from app.database import SessionLocal
from app.database import get_db
from app.api.chat import chat, ChatRequest

logger = logging.getLogger(__name__)


async def handle_message(update: Update, context: ContextTypes.DEFAULT_TYPE):
    if not update.message or not update.message.text:
        return

    user_message = update.message.text
    session_id = str(update.message.chat_id)

    await update.message.chat.send_action("typing")

    try:
        db = SessionLocal()
        try:
            request = ChatRequest(session_id=session_id, message=user_message)
            response = await chat(request, db)  # ← await
            await update.message.reply_text(response.response)
        finally:
            db.close()

    except Exception as e:
        logger.error(f"Error handling Telegram message: {e}")
        await update.message.reply_text("Sorry, something went wrong. Please try again.")


def build_telegram_app() -> Application:
    token = os.getenv("TELEGRAM_BOT_TOKEN")
    if not token:
        raise ValueError("TELEGRAM_BOT_TOKEN is not set in .env")

    app = Application.builder().token(token).build()
    app.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, handle_message))
    return app