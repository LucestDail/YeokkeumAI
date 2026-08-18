"""이음 FastAPI 엔트리 — 게이트웨이·RAG·문서·감사(RBAC, secure-by-default)."""

from __future__ import annotations

import contextlib
import json
import logging
from collections.abc import AsyncIterator, Iterator

from fastapi import Depends, FastAPI, HTTPException
from fastapi.responses import HTMLResponse, StreamingResponse

from app import rag, store
from app.auth import Principal, require_role
from app.config import get_settings
from app.gateway import get_provider
from app.schemas import ChatRequest, DraftRequest, IngestRequest, RagQueryRequest, SummarizeRequest
from app.ui import INDEX_HTML

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("ieum")


@contextlib.asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncIterator[None]:
    store.init_db()
    s = get_settings()
    if not (s.admin_token or s.user_token):
        if s.insecure_open_mode:
            logger.warning("SECURITY: 토큰 미설정 + INSECURE_OPEN_MODE=true → 전 API OPEN(로컬 전용).")
        else:
            logger.error("SECURITY: 토큰 미설정 → secure-by-default CLOSED. ADMIN_TOKEN/USER_TOKEN 설정 필요.")
    logger.info("이음 시작 · provider=%s · model=%s", get_provider(s).name, get_provider(s).model)
    yield


app = FastAPI(title="Ieum AI Platform", version="0.1.0", lifespan=lifespan)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.get("/", response_class=HTMLResponse)
def index() -> HTMLResponse:
    return HTMLResponse(INDEX_HTML)


def _sse(gen: Iterator[str]) -> Iterator[str]:
    for tok in gen:
        yield f"data: {json.dumps({'t': tok}, ensure_ascii=False)}\n\n"
    yield "data: [DONE]\n\n"


@app.post("/api/chat")
def chat(body: ChatRequest, p: Principal = Depends(require_role("user"))) -> StreamingResponse:
    s = get_settings()
    provider = get_provider(s)
    messages = []
    if body.system:
        messages.append({"role": "system", "content": body.system})
    messages.append({"role": "user", "content": body.message})
    store.audit(p.actor, p.role, "chat", {"model": provider.model, "chars": len(body.message)})
    return StreamingResponse(_sse(provider.stream(messages)), media_type="text/event-stream")


@app.post("/api/summarize")
def summarize(body: SummarizeRequest, p: Principal = Depends(require_role("user"))) -> dict[str, object]:
    s = get_settings()
    provider = get_provider(s)
    text, usage = provider.chat(
        [{"role": "system", "content": "다음 텍스트를 한국어로 3문장 이내로 요약하세요."},
         {"role": "user", "content": body.text}],
        max_tokens=512,
    )
    store.audit(p.actor, p.role, "summarize", {"model": provider.model, "chars": len(body.text)})
    return {"summary": text, "model": provider.model, "usage": usage}


@app.post("/api/draft")
def draft(body: DraftRequest, p: Principal = Depends(require_role("user"))) -> dict[str, object]:
    s = get_settings()
    provider = get_provider(s)
    text, usage = provider.chat(
        [{"role": "system", "content": f"당신은 공공기관 문서 작성 보조입니다. '{body.kind}' 초안을 한국어로 작성하세요."},
         {"role": "user", "content": body.brief}],
        max_tokens=1024,
    )
    store.audit(p.actor, p.role, "draft", {"model": provider.model, "kind": body.kind})
    return {"draft": text, "kind": body.kind, "model": provider.model, "usage": usage}


@app.post("/api/docs")
def ingest_doc(body: IngestRequest, p: Principal = Depends(require_role("user"))) -> dict[str, object]:
    s = get_settings()
    try:
        res = rag.ingest(body.filename, body.text, s)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc))
    store.audit(p.actor, p.role, "ingest", {"filename": body.filename, "n_chunks": res["n_chunks"]})
    return res


@app.get("/api/docs")
def list_docs(p: Principal = Depends(require_role("user"))) -> dict[str, object]:
    return {"items": store.list_documents()}


@app.post("/api/rag/query")
def rag_query(body: RagQueryRequest, p: Principal = Depends(require_role("user"))) -> dict[str, object]:
    s = get_settings()
    res = rag.query(body.query, s, top_k=body.top_k)
    store.audit(p.actor, p.role, "rag_query", {"query": body.query[:120], "grounded": res["grounded"],
                                               "n_citations": len(res["citations"])})
    return res


@app.get("/api/audit")
def audit_log(limit: int = 200, p: Principal = Depends(require_role("admin"))) -> dict[str, object]:
    return {"items": store.list_audit(limit=min(max(limit, 1), 1000))}
