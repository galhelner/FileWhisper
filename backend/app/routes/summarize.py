from fastapi import APIRouter, Request, Header, HTTPException, status
from fastapi.responses import JSONResponse
from bson import ObjectId
import PyPDF2
from core.security import decode_access_token
from services.ai_service import get_summary
from schemas.upload import UploadModel

router = APIRouter(prefix="/summarize", tags=["file"])


@router.post("/file/")
async def summarize_file(request: Request, authorization: str = Header(...)):
    # ---- JWT Verify ----
    if not authorization.startswith("Bearer "):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED, detail="Invalid auth header"
        )

    token = authorization.split(" ")[1]
    payload = decode_access_token(token)
    current_user = payload.get("sub")
    if not current_user:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED, detail="Unauthorized"
        )

    # ---- Parse JSON Body ----
    body = await request.json()
    file_id = body.get("file_id")
    style = body.get("style")
    length = body.get("length")

    if not file_id:
        raise HTTPException(status_code=400, detail="No file_id provided")
    if not style:
        raise HTTPException(status_code=400, detail="No style provided")
    if not length:
        raise HTTPException(status_code=400, detail="No length provided")

    db = request.app.mongodb

    # ---- Find file in Mongo ----
    upload_doc = await db["uploads"].find_one({"_id": ObjectId(file_id)})
    if not upload_doc:
        raise HTTPException(status_code=404, detail="Invalid file_id")

    if upload_doc["user_id"] != current_user:
        raise HTTPException(status_code=403, detail="Unauthorized")

    # ---- Convert to Pydantic schema ----
    upload_record = UploadModel(
        user_id=str(upload_doc["user_id"]),
        filename=upload_doc["filename"],
        file_path=upload_doc["file_path"],
        uploaded_at=upload_doc["uploaded_at"],
    )

    file_path = upload_record.file_path

    # ---- Read file ----
    text = ""
    if file_path.lower().endswith(".txt"):
        with open(file_path, "r", encoding="utf-8") as f:
            text = f.read()
    elif file_path.lower().endswith(".pdf"):
        with open(file_path, "rb") as f:
            pdf_reader = PyPDF2.PdfReader(f)
            for page in pdf_reader.pages:
                text += page.extract_text() + "\n"
    else:
        raise HTTPException(status_code=400, detail="Unsupported file type")

    # ---- Summarize ----
    summary = await get_summary(text, style, length)

    return JSONResponse(
        {"summary": summary, "filename": upload_record.filename}
    )
