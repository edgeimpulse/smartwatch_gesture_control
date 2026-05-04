#!/usr/bin/env bash
set -euo pipefail

# ── Parse Edge Impulse arguments ──────────────────────────────────────────────
INPUT_DIR=""
OUTPUT_DIR=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --input-dir)  INPUT_DIR="$2";  shift 2 ;;
        --output-dir) OUTPUT_DIR="$2"; shift 2 ;;
        *) shift ;;
    esac
done

if [[ -z "$INPUT_DIR" || -z "$OUTPUT_DIR" ]]; then
    echo "ERROR: --input-dir and --output-dir are required"
    exit 1
fi

if [[ ! -d "$INPUT_DIR/edge-impulse-sdk" ]]; then
    echo "ERROR: $INPUT_DIR does not look like an EI C++ library export"
    echo "       Expected edge-impulse-sdk/ subdirectory inside it"
    exit 1
fi

echo "==> Injecting EI deployment from: $INPUT_DIR"
EI_DEST=/app/app/src/main/cpp/ei-deployment
rm -rf "$EI_DEST"
cp -r "$INPUT_DIR" "$EI_DEST"

echo "==> Building Wear OS APK"
cd /app
./gradlew assembleDebug --no-daemon --stacktrace

echo "==> Copying output"
APK=$(find app/build/outputs/apk/debug -name "*.apk" | head -1)
if [[ -z "$APK" ]]; then
    echo "ERROR: No APK found after build"
    exit 1
fi

mkdir -p "$OUTPUT_DIR"
cp "$APK" "$OUTPUT_DIR/smartgesturecontrol-debug.apk"

echo "==> Done: $OUTPUT_DIR/smartgesturecontrol-debug.apk"
