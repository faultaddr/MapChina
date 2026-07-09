#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUTPUT_DIR="$ROOT_DIR/scripts/output/local_server"
PGDATA="${PGDATA:-$OUTPUT_DIR/pgdata}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-mapchina}"
DB_USER="${DB_USER:-mapchina}"
DB_PASSWORD="${DB_PASSWORD:-mapchina}"
PORT="${PORT:-8080}"
DB_URL="${DB_URL:-jdbc:postgresql://$DB_HOST:$DB_PORT/$DB_NAME}"
LAUNCH_LABEL="${LAUNCH_LABEL:-com.mapchina.localserver}"
LAUNCH_DOMAIN="gui/$(id -u)"
PLIST_FILE="$OUTPUT_DIR/$LAUNCH_LABEL.plist"
JAVA_BIN="${JAVA_BIN:-$(command -v java || true)}"
JAVA_HOME_FOR_SERVER="${JAVA_HOME:-}"
if [ -z "$JAVA_HOME_FOR_SERVER" ] && [ -n "$JAVA_BIN" ]; then
  JAVA_HOME_FOR_SERVER="$(cd "$(dirname "$JAVA_BIN")/.." && pwd)"
fi
SERVER_PATH="$(dirname "$JAVA_BIN"):/usr/local/bin:/opt/homebrew/bin:/usr/bin:/bin:/usr/sbin:/sbin"

mkdir -p "$OUTPUT_DIR"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

wait_for_health() {
  local url="http://127.0.0.1:$PORT/health"
  for _ in $(seq 1 60); do
    if curl -fsS "$url" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done
  echo "Server did not become healthy at $url" >&2
  echo "See $OUTPUT_DIR/server.log" >&2
  exit 1
}

xml_escape() {
  printf '%s' "$1" | sed \
    -e 's/&/\&amp;/g' \
    -e 's/</\&lt;/g' \
    -e 's/>/\&gt;/g' \
    -e 's/"/\&quot;/g' \
    -e "s/'/\&apos;/g"
}

write_launch_plist() {
  local server_bin="$ROOT_DIR/server/build/install/server/bin/server"
  cat > "$PLIST_FILE" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>Label</key>
  <string>$(xml_escape "$LAUNCH_LABEL")</string>
  <key>ProgramArguments</key>
  <array>
    <string>$(xml_escape "$server_bin")</string>
  </array>
  <key>WorkingDirectory</key>
  <string>$(xml_escape "$ROOT_DIR")</string>
  <key>EnvironmentVariables</key>
  <dict>
    <key>DB_URL</key>
    <string>$(xml_escape "$DB_URL")</string>
    <key>DB_USER</key>
    <string>$(xml_escape "$DB_USER")</string>
    <key>DB_PASSWORD</key>
    <string>$(xml_escape "$DB_PASSWORD")</string>
    <key>JWT_SECRET</key>
    <string>$(xml_escape "${JWT_SECRET:-mapchina-local-dev-secret}")</string>
    <key>PORT</key>
    <string>$(xml_escape "$PORT")</string>
    <key>JAVA_HOME</key>
    <string>$(xml_escape "$JAVA_HOME_FOR_SERVER")</string>
    <key>PATH</key>
    <string>$(xml_escape "$SERVER_PATH")</string>
  </dict>
  <key>RunAtLoad</key>
  <true/>
  <key>KeepAlive</key>
  <true/>
  <key>StandardOutPath</key>
  <string>$(xml_escape "$OUTPUT_DIR/server.log")</string>
  <key>StandardErrorPath</key>
  <string>$(xml_escape "$OUTPUT_DIR/server.log")</string>
</dict>
</plist>
EOF
}

write_launch_pid() {
  launchctl print "$LAUNCH_DOMAIN/$LAUNCH_LABEL" 2>/dev/null |
    awk -F'= ' '/pid =/ { print $2; exit }' > "$OUTPUT_DIR/server.pid" || true
}

start_server() {
  local server_bin="$ROOT_DIR/server/build/install/server/bin/server"
  if command -v launchctl >/dev/null 2>&1 && [ "$(uname)" = "Darwin" ]; then
    write_launch_plist
    launchctl bootout "$LAUNCH_DOMAIN/$LAUNCH_LABEL" >/dev/null 2>&1 || true
    : > "$OUTPUT_DIR/server.log"
    launchctl bootstrap "$LAUNCH_DOMAIN" "$PLIST_FILE"
    launchctl kickstart -k "$LAUNCH_DOMAIN/$LAUNCH_LABEL" >/dev/null 2>&1 || true
    write_launch_pid
  else
    nohup env \
      DB_URL="$DB_URL" \
      DB_USER="$DB_USER" \
      DB_PASSWORD="$DB_PASSWORD" \
      JWT_SECRET="${JWT_SECRET:-mapchina-local-dev-secret}" \
      PORT="$PORT" \
      JAVA_HOME="$JAVA_HOME_FOR_SERVER" \
      PATH="$SERVER_PATH" \
      "$server_bin" >"$OUTPUT_DIR/server.log" 2>&1 < /dev/null &
    server_pid="$!"
    echo "$server_pid" > "$OUTPUT_DIR/server.pid"
    disown "$server_pid" 2>/dev/null || true
  fi
}

require_command pg_ctl
require_command pg_isready
require_command initdb
require_command psql
require_command createdb
require_command curl
require_command java
require_command python3

if ! pg_isready -h "$DB_HOST" -p "$DB_PORT" >/dev/null 2>&1; then
  if [ ! -s "$PGDATA/PG_VERSION" ]; then
    echo "Initializing PostgreSQL data directory at $PGDATA"
    initdb -D "$PGDATA" --auth=trust >/dev/null
  fi
  echo "Starting PostgreSQL on $DB_HOST:$DB_PORT"
  pg_ctl -D "$PGDATA" -l "$OUTPUT_DIR/postgres.log" -o "-h $DB_HOST -p $DB_PORT" start
else
  echo "PostgreSQL is already accepting connections on $DB_HOST:$DB_PORT"
fi

if ! psql -h "$DB_HOST" -p "$DB_PORT" -d postgres -tAc "SELECT 1 FROM pg_roles WHERE rolname = '$DB_USER'" | grep -q 1; then
  psql -h "$DB_HOST" -p "$DB_PORT" -d postgres -c "CREATE ROLE $DB_USER LOGIN PASSWORD '$DB_PASSWORD'"
fi
psql -h "$DB_HOST" -p "$DB_PORT" -d postgres -c "ALTER ROLE $DB_USER WITH LOGIN PASSWORD '$DB_PASSWORD'" >/dev/null

if ! psql -h "$DB_HOST" -p "$DB_PORT" -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname = '$DB_NAME'" | grep -q 1; then
  createdb -h "$DB_HOST" -p "$DB_PORT" -O "$DB_USER" "$DB_NAME"
fi

if curl -fsS "http://127.0.0.1:$PORT/health" >/dev/null 2>&1; then
  echo "Ktor server is already healthy on port $PORT"
else
  echo "Starting Ktor server on port $PORT"
  (
    cd "$ROOT_DIR"
    ./gradlew :server:installDist >"$OUTPUT_DIR/server-build.log" 2>&1
  )
  start_server
  wait_for_health
fi

if command -v launchctl >/dev/null 2>&1 && [ "$(uname)" = "Darwin" ]; then
  write_launch_pid
fi

echo "Seeding map data"
(
  cd "$ROOT_DIR"
  DB_HOST="$DB_HOST" \
  DB_PORT="$DB_PORT" \
  DB_NAME="$DB_NAME" \
  DB_USER="$DB_USER" \
  DB_PASSWORD="$DB_PASSWORD" \
    python3 scripts/seed_database.py
) | tee "$OUTPUT_DIR/seed.log"

wait_for_health

cat > "$OUTPUT_DIR/env" <<EOF
DB_HOST=$DB_HOST
DB_PORT=$DB_PORT
DB_NAME=$DB_NAME
DB_USER=$DB_USER
DB_PASSWORD=$DB_PASSWORD
DB_URL=$DB_URL
PORT=$PORT
MAPCHINA_API_BASE_URL=http://127.0.0.1:$PORT
ANDROID_EMULATOR_API_BASE_URL=http://10.0.2.2:$PORT
EOF

echo
echo "Local server is ready:"
echo "  Host: http://127.0.0.1:$PORT"
echo "  Android emulator: http://10.0.2.2:$PORT"
echo "  Logs: $OUTPUT_DIR/server.log"
