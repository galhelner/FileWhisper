from fastapi import APIRouter, Header, HTTPException, status, Request
from schemas.upload import UploadModel
from core.security import decode_access_token
from datetime import datetime
from bson import ObjectId
from pathlib import Path

router = APIRouter(prefix="/files", tags=["files"])


@router.get("/")
async def get_my_files(request: Request, authorization: str = Header(...)):
    # ----- Check Authorization -----
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

    # ----- Fetch files from MongoDB -----
    db = request.app.mongodb
    cursor = db["uploads"].find({"user_id": current_user})
    user_uploads = await cursor.to_list(length=None)

    # ----- Convert to dicts and serialize datetime -----
    uploads_dict = []
    for upload in user_uploads:
        uploads_dict.append({
            "id": str(upload["_id"]),
            "user_id": str(upload["user_id"]),
            "filename": upload["filename"],
            "file_path": upload["file_path"],
            "uploaded_at":
                upload["uploaded_at"].isoformat() if
                isinstance(upload["uploaded_at"], datetime) else upload["uploaded_at"]
        })

    return {"files": uploads_dict}


@router.delete("/{file_id}")
async def delete_file(file_id: str, request: Request, authorization: str = Header(...)):
    # ----- Check Authorization -----
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

    db = request.app.mongodb

    # ---- Find file in Mongo ----
    upload_doc = await db["uploads"].find_one({"_id": ObjectId(file_id)})
    if not upload_doc:
        raise HTTPException(status_code=404, detail="Invalid file_id")

    if upload_doc["user_id"] != current_user:
        raise HTTPException(status_code=403, detail="Unauthorized")

    # ---- Delete from Mongo ----
    delete_result = await db["uploads"].delete_one({"_id": ObjectId(file_id)})
    if delete_result.deleted_count == 0:
        raise HTTPException(status_code=500, detail="Failed to delete file from database")

    # ---- Delete file from disk ----
    file_path = Path(upload_doc["file_path"])
    if file_path.exists():
        try:
            file_path.unlink()
        except Exception as e:
            raise HTTPException(status_code=500, detail=f"File deleted from DB but not from disk: {str(e)}")

    return {"message": f"File {file_id} deleted successfully"}
