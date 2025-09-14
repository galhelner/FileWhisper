from fastapi import APIRouter, Depends, Request, Header, HTTPException
from schemas.user import UserCreate, UserLogin
from services import auth_service
from core.security import decode_access_token

router = APIRouter(prefix="/auth", tags=["auth"])