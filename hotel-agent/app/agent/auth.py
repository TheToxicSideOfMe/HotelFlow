import httpx
import asyncio
import os
from dotenv import load_dotenv

load_dotenv()

GATEWAY_URL = os.getenv("GATEWAY_URL", "http://localhost:8080")
AGENT_USERNAME = os.getenv("AGENT_USERNAME")
AGENT_PASSWORD = os.getenv("AGENT_PASSWORD")

# In-memory token storage
_access_token: str | None = None
_refresh_token: str | None = None


async def login():
    """Login and store both tokens."""
    global _access_token, _refresh_token

    async with httpx.AsyncClient() as client:
        response = await client.post(
            f"{GATEWAY_URL}/api/auth/login",
            json={"username": AGENT_USERNAME, "password": AGENT_PASSWORD}
        )
        response.raise_for_status()
        data = response.json()

        _access_token = data["accessToken"]

        # Extract refresh token from cookies
        _refresh_token = response.cookies.get("refreshToken")

        print("✅ Agent logged in successfully")


async def refresh_token():
    """Use refresh token cookie to get a new access token."""
    global _access_token, _refresh_token

    async with httpx.AsyncClient() as client:
        response = await client.post(
            f"{GATEWAY_URL}/api/auth/refresh",
            cookies={"refreshToken": _refresh_token}
        )

        if response.status_code == 200:
            data = response.json()
            _access_token = data["accessToken"]
            _refresh_token = response.cookies.get("refreshToken", _refresh_token)
            print("🔄 Token refreshed successfully")
        else:
            # Refresh failed, re-login
            print("⚠️ Refresh failed, re-logging in...")
            await login()


def get_valid_token() -> str:
    """Return the current access token. Called by every tool."""
    if _access_token is None:
        raise RuntimeError("Agent is not authenticated. Token is None.")
    return _access_token


async def token_refresh_loop():
    """Background task — refreshes token every 10 minutes."""
    while True:
        await asyncio.sleep(600)  # 10 minutes
        await refresh_token()