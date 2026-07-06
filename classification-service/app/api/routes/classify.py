# Accepts validated classification jobs from gateway and schedules asynchronous extraction processing

from fastapi import APIRouter, BackgroundTasks, Depends, status

from app.core.security import verify_bearer_token
from app.models.schemas import ClassifyRequest
from app.workers.async_pipeline import run_pipeline

router = APIRouter()


@router.post("/classify", status_code=status.HTTP_202_ACCEPTED)
def classify(
    request: ClassifyRequest,
    background_tasks: BackgroundTasks,
    token_payload: dict = Depends(verify_bearer_token),
):
    background_tasks.add_task(
        run_pipeline,
        request.bucket_url,
        request.deal_id,
        request.uploaded_by_user_id,
    )

    return {
        "status": "accepted",
        "message": "Classification job started",
        "deal_id": request.deal_id,
    }