#!/usr/bin/env bash
# 16 KB-align uncompressed native libs, then sign with the committed
# app/keystore/mags-dev.jks (v1+v2+v3). Never generate a new key.
set -euo pipefail

APK=${1:?usage: align-and-sign-release-apk.sh <release.apk>}
ROOT=$(cd "$(dirname "$0")/.." && pwd)
KS="$ROOT/app/keystore/mags-dev.jks"
EXPECTED="${MAGS_RELEASE_CERT_SHA256:-04a999b18d0d8ecba01f398ea73d2e3ddcf5bcbcf2f3823f536c25d0446dad64}"

if [[ ! -f "$KS" ]]; then
  echo "Release keystore missing: $KS" >&2
  exit 1
fi

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [[ -z "$SDK" ]]; then
  echo "ANDROID_HOME / ANDROID_SDK_ROOT is not set" >&2
  exit 1
fi
BT=$(ls -d "$SDK"/build-tools/* 2>/dev/null | sort -V | tail -1)
if [[ -z "$BT" || ! -x "$BT/zipalign" || ! -x "$BT/apksigner" ]]; then
  echo "zipalign/apksigner not found under $SDK/build-tools" >&2
  exit 1
fi

ALIGNED=$(mktemp --suffix=.apk)
trap 'rm -f "$ALIGNED"' EXIT
# -P 16 page-aligns uncompressed .so to 16 KB (Pixel 10). -p is only 4 KB.
"$BT/zipalign" -P 16 -f 16 "$APK" "$ALIGNED"
"$BT/zipalign" -c -P 16 -v 16 "$ALIGNED"
"$BT/apksigner" sign \
  --ks "$KS" \
  --ks-key-alias mags-dev \
  --ks-pass pass:mags-dev \
  --key-pass pass:mags-dev \
  --v1-signing-enabled true \
  --v2-signing-enabled true \
  --v3-signing-enabled true \
  --out "$APK" \
  "$ALIGNED"

VERIFY=$("$BT/apksigner" verify --verbose --print-certs "$APK")
echo "$VERIFY"
CERT=$(printf '%s\n' "$VERIFY" | awk '/SHA-256 digest:/{print $NF; exit}' | tr -d ':' | tr 'A-F' 'a-f')
echo "release cert SHA-256=$CERT"
if [[ "$CERT" != "$EXPECTED" ]]; then
  echo "Signing cert mismatch: got $CERT want $EXPECTED" >&2
  exit 1
fi
echo "APK 16 KB-aligned and signed with pinned mags-dev cert"
