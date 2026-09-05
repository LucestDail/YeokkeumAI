# rhwp (HWP/HWPX 추출기) 조달·배포

> **왜 이 문서가 있나**: 저장소에 동봉된 `bin/rhwp` 는 **macOS arm64** 다.
> 리눅스 서버에서는 실행되지 않으므로 별도로 x86-64 바이너리를 넣어야 하는데,
> 그 사실과 절차가 어디에도 적혀 있지 않았다. 지금 라이브가 도는 것은 담당자가
> 기억하고 있어서이고, **재배포하거나 서버를 옮기면 HWP 기능이 조용히 죽는다**
> (업로드 시 503). 2026-09-05 기술 실사에서 R1 로 지목된 항목이다.

## 실물 정보

| | 값 |
|---|---|
| 출처 | https://github.com/edwardkim/rhwp (MIT) |
| 버전 | **v0.8.4** (`rhwp --version` 으로 확인) |
| 저장소 동봉 `bin/rhwp` | Mach-O 64-bit arm64 — **macOS 개발용** |
| `.25` 라이브 `/opt/yeokkeum/bin/rhwp` | ELF 64-bit x86-64 · 실행 확인됨 |

## 탐색 순서 (`HwpExtractor.resolveBinary`)

1. 환경변수 `RHWP_PATH`
2. 설정 `yeokkeum.doc.rhwp-path` (기본 `bin/rhwp`)
3. `PATH` 에서 `rhwp`

⇒ **온프렘/리눅스 배포에서는 `RHWP_PATH` 를 쓰는 것이 가장 안전하다.**
저장소의 arm64 바이너리를 실수로 집지 않는다.

## 리눅스 x86-64 바이너리 얻기

둘 중 하나. **어느 쪽을 썼는지 배포 노트에 남길 것.**

**A. 릴리스에서 받기** — 있으면 가장 간단하다
```bash
# 릴리스 페이지에서 linux x86-64 자산을 받아 배치
curl -fL -o rhwp <릴리스 URL>
chmod +x rhwp
./rhwp --version          # v0.8.4 인지 확인
```

**B. 소스에서 빌드** (Rust)
```bash
git clone https://github.com/edwardkim/rhwp && cd rhwp
cargo build --release
# target/release/rhwp 를 배치
```

## 배치

```bash
sudo install -m 755 rhwp /opt/yeokkeum/bin/rhwp
# systemd env 에 경로 고정 (권장)
echo 'RHWP_PATH=/opt/yeokkeum/bin/rhwp' | sudo tee -a /etc/yeokkeum.env
sudo systemctl restart yeokkeum
```

## 확인

**기동 로그에 결과가 남는다**(2026-09-05 추가, `StartupChecks.checkHwpBinary`):

```
HWP 추출기 사용 가능 · rhwp v0.8.4 (/opt/yeokkeum/bin/rhwp)     ← 정상
HWP 추출기를 쓸 수 없다 — HWP/HWPX 업로드는 503 이 된다 …        ← 아키텍처 불일치 등
```

```bash
sudo journalctl -u yeokkeum --since "5 min ago" | grep "HWP 추출기"
```

그전에는 **업로드해 봐야** 알 수 있었다. 이제 배포 직후 로그로 보인다.

## Docker 로 배포할 때

`Dockerfile` 에는 아직 rhwp 조달 스텝이 없다(실배포는 systemd 라 문제되지 않았다).
컨테이너로 옮긴다면 위 A/B 를 빌드 스테이지에 넣어야 한다 — 넣지 않으면
`docker compose up` 한 환경에서 HWP 가 항상 503 이다.
