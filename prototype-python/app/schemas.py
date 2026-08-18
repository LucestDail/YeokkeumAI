"""API 요청/응답 스키마."""

from __future__ import annotations

from typing import Optional

from pydantic import BaseModel, Field


class ChatRequest(BaseModel):
    message: str = Field(min_length=1)
    system: Optional[str] = None
    stream: bool = True


class SummarizeRequest(BaseModel):
    text: str = Field(min_length=1)


class DraftRequest(BaseModel):
    kind: str = Field(default="보고서")  # 보고서/공문/회의록/RFP 등
    brief: str = Field(min_length=1)


class IngestRequest(BaseModel):
    filename: str = Field(min_length=1)
    text: str = Field(min_length=1)


class RagQueryRequest(BaseModel):
    query: str = Field(min_length=1)
    top_k: Optional[int] = None
