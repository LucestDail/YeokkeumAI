"""LLM 게이트웨이 — 벤더무관.

- OpenAI 호환 ``/chat/completions`` 엔드포인트면 무엇이든 연결(OpenRouter, 사내 게이트웨이,
  vLLM/Ollama, 국산 K-AI 게이트웨이). ``LLM_BASE_URL``/``LLM_MODEL``/``LLM_API_KEY`` 로 핫스왑.
- 키가 없으면 **오프라인 stub**로 폴백(폐쇄망 데모·결정적 테스트). 실제 LLM 없이도 파이프라인 동작.
"""

from __future__ import annotations

import json
import logging
from typing import Any, Iterator

import httpx

from app.config import Settings

logger = logging.getLogger("ieum.gateway")

Message = dict[str, str]


class Provider:
    name = "base"

    def __init__(self, settings: Settings) -> None:
        self.settings = settings

    @property
    def model(self) -> str:
        return self.settings.llm_model

    def chat(self, messages: list[Message], *, temperature: float = 0.3, max_tokens: int = 1024) -> tuple[str, dict]:
        raise NotImplementedError

    def stream(self, messages: list[Message], *, temperature: float = 0.3, max_tokens: int = 1024) -> Iterator[str]:
        # 기본: chat 결과를 토큰(공백)으로 쪼개 흘림
        text, _ = self.chat(messages, temperature=temperature, max_tokens=max_tokens)
        for tok in _chunk_stream(text):
            yield tok


def _chunk_stream(text: str) -> Iterator[str]:
    buf = ""
    for ch in text:
        buf += ch
        if ch in " \n。.!?，,":
            yield buf
            buf = ""
    if buf:
        yield buf


class StubProvider(Provider):
    """오프라인 결정적 응답. 실제 추론 없이 파이프라인(채팅·요약·RAG 인용)을 시연/검증."""

    name = "stub"

    @property
    def model(self) -> str:
        return "stub"

    def chat(self, messages: list[Message], *, temperature: float = 0.3, max_tokens: int = 1024) -> tuple[str, dict]:
        sys = next((m["content"] for m in messages if m.get("role") == "system"), "")
        last_user = next((m["content"] for m in reversed(messages) if m.get("role") == "user"), "")
        if "요약" in sys or "summari" in sys.lower():
            body = _naive_summary(last_user)
            text = f"[요약·stub] {body}"
        elif "근거" in sys or "context" in sys.lower():
            # RAG: 시스템프롬프트에 삽입된 근거를 인용해 답
            text = f"[근거기반·stub] 제공된 근거를 바탕으로 답변합니다. {last_user[:120]}"
        else:
            text = f"[이음·stub 응답] 입력을 확인했습니다: {last_user[:200]}"
        usage = {"prompt_tokens": sum(len(m['content']) for m in messages) // 4, "completion_tokens": len(text) // 4}
        return text, usage


class OpenAICompatProvider(Provider):
    name = "openai_compat"

    def _headers(self) -> dict[str, str]:
        h = {"Content-Type": "application/json"}
        key = (self.settings.llm_api_key or "").strip()
        if key:
            h["Authorization"] = f"Bearer {key}"
        return h

    def _url(self) -> str:
        return (self.settings.llm_base_url or "").rstrip("/") + "/chat/completions"

    def chat(self, messages: list[Message], *, temperature: float = 0.3, max_tokens: int = 1024) -> tuple[str, dict]:
        payload = {"model": self.model, "messages": messages, "temperature": temperature, "max_tokens": max_tokens}
        with httpx.Client(timeout=self.settings.llm_timeout_seconds) as client:
            r = client.post(self._url(), headers=self._headers(), json=payload)
            r.raise_for_status()
            data = r.json()
        msg = (data.get("choices") or [{}])[0].get("message") or {}
        return str(msg.get("content") or ""), (data.get("usage") or {})

    def stream(self, messages: list[Message], *, temperature: float = 0.3, max_tokens: int = 1024) -> Iterator[str]:
        payload = {"model": self.model, "messages": messages, "temperature": temperature,
                   "max_tokens": max_tokens, "stream": True}
        try:
            with httpx.Client(timeout=self.settings.llm_timeout_seconds) as client:
                with client.stream("POST", self._url(), headers=self._headers(), json=payload) as r:
                    r.raise_for_status()
                    for line in r.iter_lines():
                        if not line or not line.startswith("data:"):
                            continue
                        chunk = line[len("data:"):].strip()
                        if chunk == "[DONE]":
                            break
                        try:
                            obj = json.loads(chunk)
                            delta = (obj.get("choices") or [{}])[0].get("delta") or {}
                            piece = delta.get("content")
                            if piece:
                                yield piece
                        except json.JSONDecodeError:
                            continue
        except httpx.HTTPError as exc:  # 스트림 실패 시 비스트림 폴백
            logger.warning("stream failed, fallback to chat: %s", exc)
            text, _ = self.chat(messages, temperature=temperature, max_tokens=max_tokens)
            for tok in _chunk_stream(text):
                yield tok


def _naive_summary(text: str, max_sentences: int = 3) -> str:
    parts = [s.strip() for s in text.replace("\n", " ").replace("。", ".").split(".") if s.strip()]
    if not parts:
        return text[:200]
    return ". ".join(parts[:max_sentences]) + ("." if parts else "")


def get_provider(settings: Settings) -> Provider:
    prov = (settings.llm_provider or "auto").strip().lower()
    if prov == "stub":
        return StubProvider(settings)
    if prov == "openai_compat":
        return OpenAICompatProvider(settings)
    # auto: 키 있으면 openai_compat, 없으면 stub(오프라인)
    if (settings.llm_api_key or "").strip():
        return OpenAICompatProvider(settings)
    return StubProvider(settings)
