from fastapi.testclient import TestClient
from app.main import app


def test_health():
    assert TestClient(app).get("/health").json() == {"status": "ok"}


def test_index_renders():
    r = TestClient(app).get("/")
    assert r.status_code == 200 and "이음" in r.text
