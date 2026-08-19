#!/usr/bin/env bash
# HWP/HWPX 파서 rhwp(github.com/edwardkim/rhwp, MIT)의 리눅스 x86-64 바이너리 빌드.
# 배포 대상(리눅스)에서 실행. Rust 미설치 환경도 Docker rust 이미지로 빌드 가능.
# 산출물을 RHWP_PATH(기본 /opt/yeokkeum/bin/rhwp)로 설치하면 HWP 업로드가 동작한다.
#
# 사용:  ./build-rhwp-linux.sh [빌드디렉토리]
set -euo pipefail

BUILD_DIR="${1:-/tmp/rhwp-build}"
INSTALL_TO="${RHWP_INSTALL:-/opt/yeokkeum/bin/rhwp}"

rm -rf "$BUILD_DIR"
git clone --depth 1 https://github.com/edwardkim/rhwp.git "$BUILD_DIR"

# rust-toolchain.toml(핀 버전)에 맞춰 Docker rust 이미지로 릴리스 빌드
docker run --rm -v "$BUILD_DIR":/src -w /src rust:latest \
  bash -c "cargo build --release --bin rhwp"

BIN="$BUILD_DIR/target/release/rhwp"
file "$BIN"
"$BIN" --version

echo
echo "설치:  sudo install -m755 '$BIN' '$INSTALL_TO'"
echo "정리:  sudo rm -rf '$BUILD_DIR'   # target/ 는 컨테이너 root 소유 → sudo 필요"
echo "연결:  /etc/yeokkeum.env 에 RHWP_PATH=$INSTALL_TO"
