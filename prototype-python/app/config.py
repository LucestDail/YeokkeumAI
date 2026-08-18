"""설정 — 환경변수 전용(온프렘/폐쇄망 기본값 안전)."""

from __future__ import annotations

from functools import lru_cache
from pathlib import Path
from typing import Optional

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict

_ROOT = Path(__file__).resolve().parents[1]


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    # --- LLM 게이트웨이 (벤더무관; OpenAI 호환 엔드포인트/국산 게이트웨이/OpenRouter) ---
    llm_provider: str = Field(default="auto", validation_alias="LLM_PROVIDER")  # auto|openai_compat|stub
    llm_base_url: str = Field(default="https://openrouter.ai/api/v1", validation_alias="LLM_BASE_URL")
    llm_api_key: Optional[str] = Field(default=None, validation_alias="LLM_API_KEY")
    llm_model: str = Field(default="deepseek/deepseek-chat", validation_alias="LLM_MODEL")
    llm_timeout_seconds: int = Field(default=120, validation_alias="LLM_TIMEOUT_SECONDS", ge=5, le=600)

    # --- 인증(RBAC) — secure-by-default: 토큰 없으면 CLOSED, 명시 open만 개방 ---
    admin_token: Optional[str] = Field(default=None, validation_alias="ADMIN_TOKEN")
    user_token: Optional[str] = Field(default=None, validation_alias="USER_TOKEN")
    insecure_open_mode: bool = Field(default=False, validation_alias="INSECURE_OPEN_MODE")

    # --- 저장 ---
    data_dir: str = Field(default_factory=lambda: str(_ROOT / "data"), validation_alias="DATA_DIR")

    # --- RAG ---
    rag_chunk_chars: int = Field(default=1200, validation_alias="RAG_CHUNK_CHARS", ge=200, le=8000)
    rag_top_k: int = Field(default=5, validation_alias="RAG_TOP_K", ge=1, le=20)

    @property
    def db_path(self) -> str:
        return str(Path(self.data_dir) / "ieum.db")


@lru_cache
def get_settings() -> Settings:
    return Settings()


def clear_settings_cache() -> None:
    get_settings.cache_clear()
