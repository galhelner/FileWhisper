from pydantic import BaseModel
from datetime import datetime


class ChatHistoryModel(BaseModel):
    upload_id: str
    history_text: str
