"""RAG — 문서 색인(청킹) + 순수 파이썬 BM25 검색 + 근거 인용 답변.

외부 임베딩/벡터DB 의존 없이 온프렘/폐쇄망에서 동작(MVP). 한국어=CJK bigram 토크나이저.
Phase 1에서 임베딩 하이브리드(pgvector)+재랭커로 고도화 예정.
"""

from __future__ import annotations

import math
import re
from collections import Counter
from typing import Any

from app import store
from app.config import Settings
from app.gateway import get_provider

_WORD_RE = re.compile(r"[A-Za-z0-9]+")
_CJK_RE = re.compile(r"[㄰-㆏가-힣぀-ヿ一-鿿]")


def tokenize(text: str) -> list[str]:
    """라틴 단어 + CJK 문자 bigram(단일문자 포함)."""
    t = text.lower()
    toks = _WORD_RE.findall(t)
    cjk = _CJK_RE.findall(t)
    toks.extend(cjk)
    for i in range(len(cjk) - 1):
        toks.append(cjk[i] + cjk[i + 1])
    return toks


def chunk_text(text: str, chunk_chars: int) -> list[str]:
    """문단 경계 우선으로 chunk_chars 근처에서 분할."""
    paras = [p.strip() for p in re.split(r"\n\s*\n", text) if p.strip()]
    if not paras:
        paras = [text.strip()] if text.strip() else []
    chunks: list[str] = []
    buf = ""
    for p in paras:
        if buf and len(buf) + len(p) + 1 > chunk_chars:
            chunks.append(buf)
            buf = p
        else:
            buf = (buf + "\n" + p) if buf else p
        while len(buf) > chunk_chars * 1.5:  # 초장문 문단 강제분할
            chunks.append(buf[:chunk_chars])
            buf = buf[chunk_chars:]
    if buf:
        chunks.append(buf)
    return chunks


def ingest(filename: str, text: str, settings: Settings) -> dict[str, Any]:
    chunks = chunk_text(text, settings.rag_chunk_chars)
    if not chunks:
        raise ValueError("빈 문서")
    doc_id = store.add_document(filename, chunks)
    return {"doc_id": doc_id, "filename": filename, "n_chunks": len(chunks)}


def _bm25_scores(query_tokens: list[str], docs_tokens: list[list[str]], k1: float = 1.5, b: float = 0.75) -> list[float]:
    n = len(docs_tokens)
    if n == 0:
        return []
    dls = [len(d) for d in docs_tokens]
    avgdl = (sum(dls) / n) or 1.0
    df: Counter[str] = Counter()
    for d in docs_tokens:
        for term in set(d):
            df[term] += 1
    scores = [0.0] * n
    q_terms = set(query_tokens)
    tfs = [Counter(d) for d in docs_tokens]
    for term in q_terms:
        if term not in df:
            continue
        idf = math.log(1 + (n - df[term] + 0.5) / (df[term] + 0.5))
        for i in range(n):
            tf = tfs[i].get(term, 0)
            if tf == 0:
                continue
            denom = tf + k1 * (1 - b + b * dls[i] / avgdl)
            scores[i] += idf * (tf * (k1 + 1)) / denom
    return scores


def search(query: str, settings: Settings, top_k: int | None = None) -> list[dict[str, Any]]:
    chunks = store.all_chunks()
    if not chunks:
        return []
    docs_tokens = [tokenize(c["text"]) for c in chunks]
    scores = _bm25_scores(tokenize(query), docs_tokens)
    ranked = sorted(zip(chunks, scores), key=lambda x: x[1], reverse=True)
    k = top_k or settings.rag_top_k
    out = []
    for c, s in ranked[:k]:
        if s <= 0:
            continue
        out.append({
            "filename": c["filename"], "idx": c["idx"], "score": round(float(s), 4),
            "snippet": c["text"][:240], "text": c["text"],
        })
    return out


def query(question: str, settings: Settings, top_k: int | None = None) -> dict[str, Any]:
    hits = search(question, settings, top_k=top_k)
    if not hits:
        return {"answer": "관련 근거를 찾지 못했습니다. 문서를 먼저 등록하거나 질문을 구체화해주세요.",
                "citations": [], "grounded": False}
    context = "\n\n".join(f"[근거 {i+1}] ({h['filename']} #{h['idx']})\n{h['text']}" for i, h in enumerate(hits))
    system = (
        "당신은 공공기관 업무보조 AI입니다. 아래 <근거>만을 바탕으로 한국어로 정확히 답하세요. "
        "근거에 없으면 모른다고 정직히 답하고 지어내지 마세요. 답변 끝에 사용한 [근거 n]을 표기하세요.\n\n"
        f"<근거>\n{context}\n</근거>"
    )
    provider = get_provider(settings)
    answer, usage = provider.chat([{"role": "system", "content": system},
                                   {"role": "user", "content": question}])
    citations = [{"filename": h["filename"], "idx": h["idx"], "score": h["score"], "snippet": h["snippet"]}
                 for h in hits]
    return {"answer": answer, "citations": citations, "grounded": True, "model": provider.model, "usage": usage}
