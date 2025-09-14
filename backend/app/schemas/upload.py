from pydantic import BaseModel
from datetime import datetime


class UploadModel(BaseModel):
    user_id: str
    filename: str
    file_path: str
    uploaded_at: datetime
