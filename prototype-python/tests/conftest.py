import pytest
from app import store
from app.config import clear_settings_cache


@pytest.fixture(autouse=True)
def _env(monkeypatch: pytest.MonkeyPatch, tmp_path):
    monkeypatch.setenv("DATA_DIR", str(tmp_path / "data"))
    monkeypatch.setenv("LLM_PROVIDER", "stub")
    monkeypatch.setenv("ADMIN_TOKEN", "adm")
    monkeypatch.setenv("USER_TOKEN", "usr")
    monkeypatch.setenv("INSECURE_OPEN_MODE", "false")
    clear_settings_cache()
    store.init_db()
    yield
    clear_settings_cache()
