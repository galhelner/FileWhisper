import os
from datetime import datetime
from fastapi import APIRouter, Request, UploadFile, File, Header, HTTPException, status
from core.security import decode_access_token
from schemas.upload import UploadModel

router = APIRouter(prefix="/upload", tags=["upload"])

BASE_UPLOAD_FOLDER = "uploads"
os.makedirs(BASE_UPLOAD_FOLDER, exist_ok=True)


@router.post("/")
async def upload_file(file: UploadFile = File(...), request: Request = None, authorization: str = Header(...)):
    # Check Authorization header
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid auth header")

    token = authorization.split(" ")[1]
    payload = decode_access_token(token)
    current_user = payload.get("sub")
    if not current_user:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Unauthorized")

    filename = file.filename
    if not filename:
        raise HTTPException(status_code=400, detail="No selected file")

    db = request.app.mongodb  # get MongoDB instance

    # Check if file already exists for this user
    existing_file = await db["uploads"].find_one({"user_id": current_user, "filename": filename})
    if existing_file:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail={
                "error": "File already uploaded",
                "file_id": existing_file["_id"],
                "filename": existing_file["filename"]
            }
        )

    # Save file in per-user folder
    user_folder = os.path.join(BASE_UPLOAD_FOLDER, str(current_user))
    os.makedirs(user_folder, exist_ok=True)

    file_path = os.path.join(user_folder, filename)
    with open(file_path, "wb") as f:
        f.write(await file.read())

    # Save record in MongoDB
    upload_record = UploadModel(
        user_id=current_user,
        filename=filename,
        file_path=file_path,
        uploaded_at=datetime.utcnow()
    )
    await db["uploads"].insert_one(upload_record.dict())

    return {"message": "File uploaded successfully", "filename": filename}
