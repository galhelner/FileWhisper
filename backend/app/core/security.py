from datetime import datetime, timedelta
from jose import JWTError, jwt
from core.config import settings

# temporary blacklist for logout
blacklist = set()


def create_access_token(data: dict, expires_delta: int = None):
    to_encode = data.copy()
    expire = datetime.utcnow() + timedelta(
        minutes=expires_delta or settings.JWT_EXPIRE_MINUTES
    )
    to_encode.update({"exp": expire})
    return jwt.encode(to_encode, settings.JWT_SECRET_KEY, algorithm=settings.JWT_ALGORITHM)


def decode_access_token(token: str):
    if token in blacklist:
        raise JWTError("Token is revoked")

    payload = jwt.decode(token, settings.JWT_SECRET_KEY, algorithms=[settings.JWT_ALGORITHM])
    return payload


def revoke_token(token: str):
    blacklist.add(token)
