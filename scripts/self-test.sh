#!/usr/bin/env bash
# Zelftest-pipeline: unit + screenshots + (als de emulator leeft) install/screencap/logcat.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${SELF_TEST_OUT:-/cursor/stores/self/artifacts/screens-0.1.8}"
mkdir -p "$OUT" || true
export ANDROID_HOME="${ANDROID_HOME:-/home/ubuntu/android-sdk}"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"

echo "== unit tests =="
(cd "$ROOT" && ./gradlew :core:test :app:testDebugUnitTest --offline)

SHOTS="$ROOT/app/build/outputs/self-test/screenshots"
if [[ -d "$SHOTS" ]]; then
  echo "== copy screenshots → $OUT =="
  cp -f "$SHOTS"/*.png "$OUT/" 2>/dev/null || true
fi

echo "== kvm / emulator =="
if [[ -e /dev/kvm ]]; then
  echo "kvm: yes"
else
  echo "kvm: no"
fi
if command -v adb >/dev/null; then
  adb start-server >/dev/null || true
fi
if ! command -v adb >/dev/null || ! adb devices | grep -q 'emulator.*device'; then
  echo "geen emulator-device; skip adb-smoke (unit + screenshots staan in $OUT)"
  exit 0
fi

APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
adb install -r -t "$APK"
adb logcat -c || true
adb shell am force-stop app.mags.debug || true
adb shell am start -n app.mags.debug/nl.cita.magister.MainActivity --ez demo true
sleep 4

screencap() {
  local name="$1"
  adb exec-out screencap -p > "$OUT/emu-$name.png"
}

screencap login-or-today
tap_text() {
  local label="$1"
  local bounds
  bounds=$(adb exec-out uiautomator dump /dev/tty 2>/dev/null | tr '>' '\n' | grep -F "text=\"$label\"" | head -1 || true)
  if [[ -z "$bounds" ]]; then
    echo "geen tap-target voor $label"
    return 1
  fi
  local x1 y1 x2 y2
  read -r x1 y1 x2 y2 <<<"$(echo "$bounds" | sed -E 's/.*\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\].*/\1 \2 \3 \4/')"
  local x=$(( (x1 + x2) / 2 ))
  local y=$(( (y1 + y2) / 2 ))
  adb shell input tap "$x" "$y"
}

tap_text "Voorbeeld" && sleep 2 && screencap vandaag || true
tap_text "Rooster" && sleep 2 && screencap rooster || true
adb shell input swipe 900 800 200 800 280 || true
sleep 1
screencap rooster-swipe || true
tap_text "Cijfers" && sleep 2 && screencap cijfers || true
tap_text "Huiswerk" && sleep 2 && screencap huiswerk || true
tap_text "Berichten" || tap_text "●" || true
screencap berichten || true

echo "== logcat crashes =="
adb logcat -d -t 200 '*:E' AndroidRuntime:E *:S | tee "$OUT/logcat-errors.txt" || true
if grep -E 'FATAL EXCEPTION|AndroidRuntime' "$OUT/logcat-errors.txt" >/dev/null; then
  echo "CRASH in logcat"
  exit 1
fi
echo "adb-smoke ok → $OUT"
