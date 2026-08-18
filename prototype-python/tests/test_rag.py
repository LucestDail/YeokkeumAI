from fastapi.testclient import TestClient
from app.main import app

H = {"Authorization": "Bearer usr"}


def test_ingest_and_query_citation():
    c = TestClient(app)
    doc = "조달청은 나라장터를 운영한다.\n\n웹접근성은 KWCAG 2.2를 따른다.\n\n개인정보는 암호화하여 보관한다."
    ing = c.post("/api/docs", json={"filename": "지침.txt", "text": doc}, headers=H)
    assert ing.status_code == 200 and ing.json()["n_chunks"] >= 1
    q = c.post("/api/rag/query", json={"query": "웹접근성 지침은 무엇을 따르나"}, headers=H)
    assert q.status_code == 200
    j = q.json()
    assert j["grounded"] is True and len(j["citations"]) >= 1
    assert j["citations"][0]["filename"] == "지침.txt"


def test_query_no_docs_is_honest():
    j = TestClient(app).post("/api/rag/query", json={"query": "존재하지않는질의"}, headers=H).json()
    assert j["grounded"] is False
