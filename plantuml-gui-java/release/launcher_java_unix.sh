#!/usr/bin/env bash
# =============================================================================
#  PlantUML GUI - user launcher (Linux / macOS)
# -----------------------------------------------------------------------------
#  Double-click or run it: it just starts the app, no questions asked.
#  Developers who want the build/clean menu should use dev_java_unix.sh instead.
# =============================================================================

set -u

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JAR_NAME="plantuml-gui-java.jar"

if ! command -v java >/dev/null 2>&1; then
    echo "[FAIL]  'java' was not found on PATH. Install Java 17 or newer and try again." >&2
    exit 1
fi

# Unzipped release first, then the developer build output one level up.
for candidate in "$SCRIPT_DIR/$JAR_NAME" "$SCRIPT_DIR/../target/$JAR_NAME"; do
    [ -f "$candidate" ] && { JAR_FILE="$candidate"; break; }
done

if [ -z "${JAR_FILE:-}" ]; then
    echo "[FAIL]  $JAR_NAME was not found next to this script." >&2
    echo "        Download the release zip from https://github.com/diosaraiva/plantuml-gui/releases" >&2
    exit 1
fi

# Keep java_config.ini beside the JAR; otherwise the app would create it one level up.
exec java -Dplantumlgui.config="$SCRIPT_DIR/java_config.ini" -jar "$JAR_FILE" "$@"
