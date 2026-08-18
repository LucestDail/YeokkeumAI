"""RBAC — admin/user 토큰. secure-by-default: 토큰 미설정 시 CLOSED(명시 open만 개방)."""

from __future__ import annotations

import hmac
from dataclasses import dataclass
from typing import Optional

from fastapi import Header, HTTPException

from app.config import get_settings

_ROLE_RANK = {"none": 0, "user": 1, "admin": 2}


@dataclass
class Principal:
    actor: str
    role: str


def _eq(a: str, b: str) -> bool:
    return bool(a) and bool(b) and hmac.compare_digest(a, b)


def _present(authorization: Optional[str], x_api_key: Optional[str]) -> str:
    if authorization and authorization.lower().startswith("bearer "):
        return authorization[7:].strip()
    if x_api_key:
        return x_api_key.strip()
    return ""


def resolve_principal(authorization: Optional[str], x_api_key: Optional[str]) -> Principal:
    s = get_settings()
    admin = (s.admin_token or "").strip()
    user = (s.user_token or "").strip()
    token = _present(authorization, x_api_key)
    if admin and _eq(token, admin):
        return Principal(actor=f"token:{token[:6]}", role="admin")
    if user and _eq(token, user):
        return Principal(actor=f"token:{token[:6]}", role="user")
    if not admin and not user:
        # 토큰 자체가 구성 안 됨 → secure-by-default
        if s.insecure_open_mode:
            return Principal(actor="open", role="admin")
        return Principal(actor="anon", role="none")
    return Principal(actor="anon", role="none")


def require_role(min_role: str = "user"):
    """FastAPI 의존성 팩토리. min_role 이상만 통과."""

    def _dep(
        authorization: Optional[str] = Header(default=None),
        x_api_key: Optional[str] = Header(default=None, alias="X-API-Key"),
    ) -> Principal:
        p = resolve_principal(authorization, x_api_key)
        if _ROLE_RANK.get(p.role, 0) < _ROLE_RANK.get(min_role, 1):
            raise HTTPException(status_code=401 if p.role == "none" else 403, detail="unauthorized")
        return p

    return _dep
