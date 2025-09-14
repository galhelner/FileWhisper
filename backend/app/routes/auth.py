from fastapi import APIRouter, Depends, Request, Header, HTTPException
from schemas.user import UserCreate, UserLogin
from services import auth_service
from core.security import decode_access_token

router = APIRouter(prefix="/auth", tags=["auth"])


@router.post("/register")
async def register(user: UserCreate, request: Request):
    db = request.app.mongodb
    return await auth_service.register_user(db, user)


@router.post("/login")
async def login(user: UserLogin, request: Request):
    db = request.app.mongodb
    return await auth_service.login_user(db, user)


@router.post("/logout")
async def logout(authorization: str = Header(...)):
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Invalid auth header")
    token = authorization.split(" ")[1]
    return await auth_service.logout_user(token)


@router.get("/me")
async def get_me(authorization: str = Header(...)):
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Invalid auth header")
    token = authorization.split(" ")[1]
    payload = decode_access_token(token)
    return {"user": payload["sub"], "full_name": payload.get("full_name", "N/A")}
