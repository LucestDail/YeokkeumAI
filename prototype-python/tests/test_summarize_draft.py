from fastapi.testclient import TestClient
from app.main import app

H = {"Authorization": "Bearer usr"}


def test_summarize():
    r = TestClient(app).post("/api/summarize", json={"text": "첫문장. 둘째문장. 셋째문장. 넷째문장."}, headers=H)
    assert r.status_code == 200 and "요약" in r.json()["summary"]


def test_draft():
    r = TestClient(app).post("/api/draft", json={"kind": "공문", "brief": "회의 일정 안내"}, headers=H)
    assert r.status_code == 200 and r.json()["kind"] == "공문"
