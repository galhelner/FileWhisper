from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    MONGO_URI: str
    MONGO_DB: str
    JWT_SECRET_KEY: str
    JWT_ALGORITHM: str = 'HS256'
    JWT_EXPIRE_MINUTES: int = 60

    class Config:
        env_file = '../../../.env'


settings = Settings()
