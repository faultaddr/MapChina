#!/usr/bin/env bash
set -euo pipefail

usage() {
    cat <<'EOF'
Usage: DEVICE_UDID=<simulator-udid> APP_PATH=<path-to-app> OUTPUT_DIR=<artifact-directory> CONFIGURATION=<Debug|Release> ios_performance_capture.sh
EOF
}

require_environment() {
    local name="$1"
    if [[ -z "${!name:-}" ]]; then
        printf 'Missing required environment variable: %s\n' "$name" >&2
        usage >&2
        exit 2
    fi
}

record_command() {
    printf '%q ' "$@" >> "$OUTPUT_DIR/commands.log"
    printf '\n' >> "$OUTPUT_DIR/commands.log"
}

run_and_preserve_output() {
    local log_path="$1"
    shift
    record_command "$@"
    "$@" >"$log_path" 2>&1
}

require_environment DEVICE_UDID
require_environment APP_PATH
require_environment OUTPUT_DIR
require_environment CONFIGURATION

if [[ "$APP_PATH" != *.app || ! -d "$APP_PATH" ]]; then
    printf 'APP_PATH must be an .app directory: %s\n' "$APP_PATH" >&2
    exit 2
fi

if [[ "$CONFIGURATION" != "Debug" && "$CONFIGURATION" != "Release" ]]; then
    printf 'CONFIGURATION must be Debug or Release: %s\n' "$CONFIGURATION" >&2
    exit 2
fi

mkdir -p "$OUTPUT_DIR"
: > "$OUTPUT_DIR/commands.log"

PLIST_PATH="$APP_PATH/Info.plist"
if [[ ! -f "$PLIST_PATH" ]]; then
    printf 'Missing application Info.plist: %s\n' "$PLIST_PATH" >&2
    exit 2
fi

BUNDLE_ID="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleIdentifier' "$PLIST_PATH")"
APP_PROCESS="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleExecutable' "$PLIST_PATH")"
CAPTURED_AT_UTC="$(date -u '+%Y-%m-%dT%H:%M:%SZ')"

cat > "$OUTPUT_DIR/metadata.env" <<EOF
DEVICE_UDID=$DEVICE_UDID
APP_PATH=$APP_PATH
CONFIGURATION=$CONFIGURATION
BUNDLE_ID=$BUNDLE_ID
APP_PROCESS=$APP_PROCESS
MAPCHINA_PERF_TRACE=1
CAPTURED_AT_UTC=$CAPTURED_AT_UTC
EOF

LOG_PID=""
cleanup() {
    local status=$?
    if [[ -n "$LOG_PID" ]]; then
        kill "$LOG_PID" 2>/dev/null || true
        wait "$LOG_PID" 2>/dev/null || true
    fi
    exit "$status"
}
trap cleanup EXIT

run_and_preserve_output "$OUTPUT_DIR/install.log" xcrun simctl install "$DEVICE_UDID" "$APP_PATH"
run_and_preserve_output "$OUTPUT_DIR/launch.log" env SIMCTL_CHILD_MAPCHINA_PERF_TRACE=1 xcrun simctl launch "$DEVICE_UDID" "$BUNDLE_ID"

record_command xcrun simctl spawn "$DEVICE_UDID" log stream --style compact --level debug
xcrun simctl spawn "$DEVICE_UDID" log stream --style compact --level debug >"$OUTPUT_DIR/device.log" 2>&1 &
LOG_PID=$!

run_and_preserve_output "$OUTPUT_DIR/animation-hitches.log" \
    xcrun xctrace record --template 'Animation Hitches' --device "$DEVICE_UDID" \
    --attach "$APP_PROCESS" --time-limit 30s --output "$OUTPUT_DIR/animation-hitches.trace" --no-prompt
run_and_preserve_output "$OUTPUT_DIR/time-profiler.log" \
    xcrun xctrace record --template 'Time Profiler' --device "$DEVICE_UDID" \
    --attach "$APP_PROCESS" --time-limit 30s --output "$OUTPUT_DIR/time-profiler.trace" --no-prompt
