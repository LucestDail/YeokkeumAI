"""SQLite 영속 — 문서·청크·감사로그. 온프렘/폐쇄망 친화(외부 DB 불필요, MVP).

Phase 3에서 PostgreSQL+pgvector로 교체 예정(인터페이스 유지).
"""

from __future__ import annotations

import json
import sqlite3
import time
import uuid
from contextlib import contextmanager
from pathlib import Path
from typing import Any, Iterator

from app.config import get_settings

_SCHEMA = """
CREATE TABLE IF NOT EXISTS documents (
    id TEXT PRIMARY KEY,
    filename TEXT NOT NULL,
    chars INTEGER NOT NULL,
    n_chunks INTEGER NOT NULL,
    created_at REAL NOT NULL
);
CREATE TABLE IF NOT EXISTS chunks (
    id TEXT PRIMARY KEY,
    doc_id TEXT NOT NULL,
    idx INTEGER NOT NULL,
    text TEXT NOT NULL,
    FOREIGN KEY (doc_id) REFERENCES documents(id)
);
CREATE TABLE IF NOT EXISTS audit (
    id TEXT PRIMARY KEY,
    ts REAL NOT NULL,
    actor TEXT NOT NULL,
    role TEXT NOT NULL,
    action TEXT NOT NULL,
    detail TEXT NOT NULL
);
"""


def _now() -> float:
    return time.time()


def new_id() -> str:
    return uuid.uuid4().hex[:16]


@contextmanager
def _conn() -> Iterator[sqlite3.Connection]:
    settings = get_settings()
    Path(settings.data_dir).mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(settings.db_path, timeout=10)
    conn.row_factory = sqlite3.Row
    try:
        conn.execute("PRAGMA journal_mode=WAL")
        yield conn
        conn.commit()
    finally:
        conn.close()


def init_db() -> None:
    with _conn() as c:
        c.executescript(_SCHEMA)


# ── documents / chunks ──
def add_document(filename: str, chunks: list[str]) -> str:
    doc_id = new_id()
    with _conn() as c:
        c.execute(
            "INSERT INTO documents(id,filename,chars,n_chunks,created_at) VALUES(?,?,?,?,?)",
            (doc_id, filename, sum(len(x) for x in chunks), len(chunks), _now()),
        )
        c.executemany(
            "INSERT INTO chunks(id,doc_id,idx,text) VALUES(?,?,?,?)",
            [(new_id(), doc_id, i, t) for i, t in enumerate(chunks)],
        )
    return doc_id


def list_documents() -> list[dict[str, Any]]:
    with _conn() as c:
        rows = c.execute(
            "SELECT id,filename,chars,n_chunks,created_at FROM documents ORDER BY created_at DESC"
        ).fetchall()
    return [dict(r) for r in rows]


def all_chunks() -> list[dict[str, Any]]:
    with _conn() as c:
        rows = c.execute(
            "SELECT ch.id, ch.doc_id, ch.idx, ch.text, d.filename "
            "FROM chunks ch JOIN documents d ON d.id=ch.doc_id"
        ).fetchall()
    return [dict(r) for r in rows]


# ── audit ──
def audit(actor: str, role: str, action: str, detail: dict[str, Any] | None = None) -> None:
    with _conn() as c:
        c.execute(
            "INSERT INTO audit(id,ts,actor,role,action,detail) VALUES(?,?,?,?,?,?)",
            (new_id(), _now(), actor, role, action, json.dumps(detail or {}, ensure_ascii=False)),
        )


def list_audit(limit: int = 200) -> list[dict[str, Any]]:
    with _conn() as c:
        rows = c.execute(
            "SELECT id,ts,actor,role,action,detail FROM audit ORDER BY ts DESC LIMIT ?", (limit,)
        ).fetchall()
    out = []
    for r in rows:
        d = dict(r)
        try:
            d["detail"] = json.loads(d["detail"])
        except (json.JSONDecodeError, TypeError):
            pass
        out.append(d)
    return out
