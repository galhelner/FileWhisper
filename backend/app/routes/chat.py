from fastapi import APIRouter, Header, HTTPException, status, Request
from typing import List
from bson import ObjectId
from datetime import datetime
from schemas.upload import UploadModel
from schemas.chat_history import ChatHistoryModel
from services.ai_service import ask_model
from core.security import decode_access_token

router = APIRouter(prefix="/chat", tags=["chat"])


def parse_history(history_text: str) -> List[dict]:
    messages = []
    for line in history_text.splitlines():
        if line.startswith("User:"):
            messages.append({
                "sender": "user",
                "text": line.replace("User:", "").strip(),
                "is_html": False
            })
        elif line.startswith("Assistant:"):
            messages.append({
                "sender": "assistant",
                "text": line.replace("Assistant:", "").strip(),
                "is_html": False
            })
    return messages


@router.post("/")
async def ask_question(
    request: Request,
    authorization: str = Header(...),
    data: dict = None
):
    # ----- JWT verification -----
    if not authorization.startswith("Bearer "):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid auth header"
        )
    token = authorization.split(" ")[1]
    payload = decode_access_token(token)
    current_user = payload.get("sub")
    if not current_user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Unauthorized"
        )

    # ----- Validate input -----
    if not data or "context_file_id" not in data:
        raise HTTPException(status_code=400, detail="Missing context file id")
    if "question" not in data:
        raise HTTPException(status_code=400, detail="Missing question")

    context_file_id = data["context_file_id"]
    question = data["question"]

    # ----- Call AI service -----
    db = request.app.mongodb
    answer = await ask_model(db, current_user, context_file_id, question)
    return {"answer": answer}


@router.get("/history")
async def get_history(
    request: Request,
    context_file_id: str,
    authorization: str = Header(...)
):
    # ----- JWT verification -----
    if not authorization.startswith("Bearer "):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid auth header"
        )
    token = authorization.split(" ")[1]
    payload = decode_access_token(token)
    current_user = payload.get("sub")
    if not current_user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Unauthorized"
        )

    # ----- Fetch chat history from MongoDB -----
    db = request.app.mongodb

    # Find the chat history document linked to this user and file
    chat_history_doc = await db["chat_history"].find_one({
        "upload_id": context_file_id
    })

    # Ensure the file belongs to the current user
    upload_doc = await db["uploads"].find_one({"_id": ObjectId(context_file_id), "user_id": current_user})
    if not upload_doc:
        raise HTTPException(status_code=404, detail="Context file not found or unauthorized")

    if not chat_history_doc:
        return {"chat_history": []}

    return {"chat_history": parse_history(chat_history_doc["history_text"])}
