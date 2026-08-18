import pytest
from fastapi.testclient import TestClient
from app.config import clear_settings_cache
from app.main import app


def test_secure_by_default_closed(monkeypatch: pytest.MonkeyPatch):
    monkeypatch.delenv("ADMIN_TOKEN", raising=False)
    monkeypatch.delenv("USER_TOKEN", raising=False)
    monkeypatch.setenv("INSECURE_OPEN_MODE", "false")
    clear_settings_cache()
    c = TestClient(app)
    assert c.get("/health").status_code == 200
    assert c.post("/api/rag/query", json={"query": "x"}).status_code == 401


def test_audit_admin_only():
    c = TestClient(app)
    assert c.get("/api/audit", headers={"Authorization": "Bearer usr"}).status_code == 403
    assert c.get("/api/audit", headers={"Authorization": "Bearer adm"}).status_code == 200


def test_audit_records_actions():
    c = TestClient(app)
    c.post("/api/summarize", json={"text": "가나다. 라마바. 사아자."}, headers={"Authorization": "Bearer usr"})
    items = c.get("/api/audit", headers={"Authorization": "Bearer adm"}).json()["items"]
    assert any(i["action"] == "summarize" for i in items)
