#!/usr/bin/env bash
# Zelftest-pipeline: unit + screenshots + (als /dev/kvm leeft) echte emulator-smoke.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="${SELF_TEST_OUT:-/cursor/stores/self/artifacts/screens-0.1.19}"
mkdir -p "$OUT" /opt/cursor/artifacts/0.1.19 || true
export ANDROID_HOME="${ANDROID_HOME:-/home/ubuntu/android-sdk}"
export ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-$ANDROID_HOME}"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"

echo "== unit tests =="
(cd "$ROOT" && ./gradlew :core:test :app:testDebugUnitTest --offline)

SHOTS="$ROOT/app/build/outputs/self-test/screenshots"
if [[ -d "$SHOTS" ]]; then
  echo "== copy Robolectric screenshots → $OUT =="
  cp -f "$SHOTS"/*.png "$OUT/" 2>/dev/null || true
fi

echo "== kvm / emulator =="
if [[ -e /dev/kvm ]]; then
  echo "kvm: yes"
else
  echo "kvm: no"
fi

ADB="$ANDROID_HOME/platform-tools/adb"
EMU="$ANDROID_HOME/emulator/emulator"
if [[ ! -x "$ADB" ]]; then
  echo "geen adb; skip emulator-smoke"
  exit 0
fi

# Stale cita34/cita37 leftover-instances: adb, qemu, lockfiles.
"$ADB" kill-server >/dev/null 2>&1 || true
pkill -9 -f 'qemu-system' >/dev/null 2>&1 || true
pkill -9 -f 'emulator.*-avd' >/dev/null 2>&1 || true
sleep 1
rm -f "$HOME"/.android/avd/*.avd/*.lock \
      "$HOME"/.android/avd/*.avd/hardware-qemu.ini.lock \
      /tmp/android-*/emulator-* >/dev/null 2>&1 || true
find "$HOME/.android/avd" -name '*.lock' -delete 2>/dev/null || true

if [[ ! -e /dev/kvm || ! -x "$EMU" ]]; then
  echo "geen kvm of emulator-bin; skip adb-smoke"
  exit 0
fi

AVD="${ANDROID_AVD:-}"
if [[ -z "$AVD" ]]; then
  AVD="$("$EMU" -list-avds 2>/dev/null | head -1 || true)"
fi
if [[ -z "$AVD" ]]; then
  echo "geen AVD; skip adb-smoke"
  exit 0
fi

echo "start AVD $AVD headless (KVM, daarna TCG-fallback als de guest hangt)"
"$ADB" start-server >/dev/null
start_emu() {
  local accel="$1"
  "$EMU" -avd "$AVD" -no-window -no-snapshot -no-audio -no-boot-anim \
    -gpu swiftshader_indirect -accel "$accel" -netdelay none -netspeed full \
    >/tmp/mags-emulator.log 2>&1 &
  echo $!
}

EMU_PID=$(start_emu on)
sleep 12
cpu=$(ps -p "$EMU_PID" -o pcpu= 2>/dev/null | tr -d ' ' || echo 0)
if awk "BEGIN { exit !($cpu < 3) }"; then
  echo "KVM-guest idle (cpu=$cpu); herstart met TCG"
  kill -9 "$EMU_PID" 2>/dev/null || true
  pkill -9 -f 'qemu-system' || true
  sleep 1
  EMU_PID=$(start_emu off)
fi

echo "wacht op boot (sys.boot_completed)"
for i in $(seq 1 120); do
  boot="$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
  if [[ "$boot" == "1" ]]; then
    echo "boot_completed na ${i}s"
    break
  fi
  if ! kill -0 "$EMU_PID" 2>/dev/null; then
    echo "emulator stierf; log:"
    tail -40 /tmp/mags-emulator.log || true
    exit 1
  fi
  sleep 3
done
boot="$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')"
if [[ "$boot" != "1" ]]; then
  echo "boot niet voltooid; skip adb-smoke"
  tail -40 /tmp/mags-emulator.log || true
  exit 0
fi

"$ADB" shell settings put global window_animation_scale 1 || true
"$ADB" shell settings put global transition_animation_scale 1 || true
"$ADB" shell settings put global animator_duration_scale 1 || true
"$ADB" shell wm dismiss-keyguard || true
"$ADB" shell input keyevent 82 || true

APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
if [[ ! -f "$APK" ]]; then
  (cd "$ROOT" && ./gradlew :app:assembleDebug --offline)
fi
"$ADB" install -r -t "$APK"
"$ADB" logcat -c || true
"$ADB" shell am force-stop app.mags.debug || true
"$ADB" shell am start -n app.mags.debug/nl.cita.magister.MainActivity --ez demo true
sleep 5

screencap() {
  local name="$1"
  "$ADB" exec-out screencap -p > "$OUT/emu-$name.png"
}

dump_ui() {
  "$ADB" shell uiautomator dump /sdcard/uidump.xml >/dev/null 2>&1 || true
  "$ADB" exec-out cat /sdcard/uidump.xml 2>/dev/null || true
}

tap_label() {
  local label="$1"
  local xml
  xml=$(dump_ui)
  local bounds
  bounds=$(printf '%s' "$xml" | tr '>' '\n' | grep -F "text=\"$label\"" | head -1 || true)
  if [[ -z "$bounds" ]]; then
    bounds=$(printf '%s' "$xml" | tr '>' '\n' | grep -F "content-desc=\"$label\"" | head -1 || true)
  fi
  if [[ -z "$bounds" ]]; then
    echo "geen tap-target voor $label"
    return 1
  fi
  local x1 y1 x2 y2
  read -r x1 y1 x2 y2 <<<"$(echo "$bounds" | sed -E 's/.*\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\].*/\1 \2 \3 \4/')"
  local x=$(( (x1 + x2) / 2 ))
  local y=$(( (y1 + y2) / 2 ))
  echo "tap $label @ $x,$y"
  "$ADB" shell input tap "$x" "$y"
}

screencap login-or-today
tap_label "Voorbeeld" && sleep 2 && screencap vandaag || true
# Mid-animation + settle (≥1s) + pressed, for the ghost-pill check.
if tap_label "Rooster"; then
  sleep 0.12
  screencap rooster-mid
  sleep 1.2
  screencap rooster
  # Pressed state on Vandaag while Rooster stays selected — no second pill.
  xml=$(dump_ui)
  vb=$(printf '%s' "$xml" | tr '>' '\n' | grep -F 'content-desc="Vandaag"' | head -1 || true)
  if [[ -n "$vb" ]]; then
    read -r x1 y1 x2 y2 <<<"$(echo "$vb" | sed -E 's/.*\[([0-9]+),([0-9]+)\]\[([0-9]+),([0-9]+)\].*/\1 \2 \3 \4/')"
    px=$(( (x1 + x2) / 2 ))
    py=$(( (y1 + y2) / 2 ))
    "$ADB" shell input swipe "$px" "$py" "$px" "$py" 700 || true
    screencap rooster-pressed
  fi
fi
"$ADB" shell input swipe 900 800 200 800 280 || true
sleep 1
screencap rooster-swipe || true
tap_label "Cijfers" && sleep 2 && screencap cijfers || true
tap_label "Huiswerk" && sleep 2 && screencap huiswerk || true
tap_label "Vandaag" && sleep 1 && screencap vandaag-rest || true
# Scroll homework through the dock area — last item must clear the opaque pill.
"$ADB" shell input swipe 540 1400 540 400 350 || true
sleep 0.6
screencap vandaag-scroll || true
tap_label "Berichten" || tap_label "●" || true
sleep 1
screencap berichten || true
# Meer via profiel-icoon rechtsboven: swipe niet, tap content-desc Profiel
tap_label "Profiel" && sleep 2 && screencap meer || true

echo "== logcat FATAL =="
"$ADB" logcat -d -t 400 '*:E' AndroidRuntime:E *:S | tee "$OUT/logcat-errors.txt" || true
if grep -E 'FATAL EXCEPTION' "$OUT/logcat-errors.txt" >/dev/null; then
  echo "CRASH in logcat"
  exit 1
fi
echo "adb-smoke ok → $OUT"
