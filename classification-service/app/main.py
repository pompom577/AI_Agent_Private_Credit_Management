from dotenv import load_dotenv
from fastapi import FastAPI

from app.api.routes.classify import router as classify_router

load_dotenv()

app = FastAPI(title="Classification Service")

app.include_router(classify_router)


@app.get("/health")
def health_check():
    return {"status": "ok"}