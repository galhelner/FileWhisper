from fastapi import HTTPException, status
from core.security import create_access_token, revoke_token
from passlib.context import CryptContext

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")


def hash_password(password: str) -> str:
    return pwd_context.hash(password)


def verify_password(plain_password: str, hashed_password: str) -> bool:
    return pwd_context.verify(plain_password, hashed_password)


async def register_user(db, user_data):
    users_collection = db["users"]
    existing = await users_collection.find_one({"email": user_data.email})
    if existing:
        raise HTTPException(status_code=400, detail="Email already registered")

    hashed_pw = hash_password(user_data.password)
    user_dict = {"full_name": user_data.full_name, "email": user_data.email, "password": hashed_pw}
    result = await users_collection.insert_one(user_dict)
    return {"id": str(result.inserted_id), "email": user_data.email}


async def login_user(db, user_data):
    users_collection = db["users"]
    user = await users_collection.find_one({"email": user_data.email})
    if not user or not verify_password(user_data.password, user["password"]):
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED,
                            detail="Invalid credentials")

    token = create_access_token({"sub": str(user["_id"]), "full_name": user.get("full_name", "")})
    return {"token": token, "full_name": user.get("full_name", "")}


async def logout_user(token: str):
    revoke_token(token)
    return {"msg": "Successfully logged out"}
