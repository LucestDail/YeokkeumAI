from fastapi.testclient import TestClient
from app.main import app


def test_chat_requires_auth():
    assert TestClient(app).post("/api/chat", json={"message": "안녕"}).status_code == 401


def test_chat_stub_stream():
    r = TestClient(app).post("/api/chat", json={"message": "테스트 질문"},
                             headers={"Authorization": "Bearer usr"})
    assert r.status_code == 200
    assert "data:" in r.text and "stub" in r.text and "[DONE]" in r.text
