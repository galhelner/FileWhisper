from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from contextlib import asynccontextmanager
from motor.motor_asyncio import AsyncIOMotorClient
from core.config import settings
from routes import auth, upload, summarize, files, chat
import uvicorn


@asynccontextmanager
async def lifespan(_: FastAPI):
    # server startup code
    await startup_db_client()
    yield
    # server shutdown code
    await shutdown_db_client()


# Global MongoDB client
async def startup_db_client():
    try:
        print("🔌 Connecting to MongoDB...")
        app.mongodb_client = AsyncIOMotorClient(settings.MONGO_URI)
        await app.mongodb_client.admin.command("ping")
        print("✅ Connected to MongoDB!")
        app.mongodb = app.mongodb_client[settings.MONGO_DB]
    except Exception as e:
        print("❌ Could not connect to MongoDB:", e)
        raise e


async def shutdown_db_client():
    app.mongodb_client.close()

# ----- CORS -----
origins = [
    "http://localhost:3000",   # frontend dev server
    "http://127.0.0.1:3000",   # frontend dev server
]

app = FastAPI(title="FileWhisper Backend", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routes
app.include_router(auth.router)
app.include_router(upload.router)
app.include_router(summarize.router)
app.include_router(files.router)
app.include_router(chat.router)

if __name__ == "__main__":
    uvicorn.run("main:app", host="0.0.0.0", port=5000, reload=True)
