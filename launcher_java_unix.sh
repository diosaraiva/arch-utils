#!/usr/bin/env bash
# =============================================================================
#  PlantUML GUI - Unix launcher (Linux / macOS)
# -----------------------------------------------------------------------------
#  Interactive menu:
#    1) Run without compiling (java source-code launcher, no .class files)
#    2) Compile (only if needed) then run from compiled classes
#    3) Clean build artifacts (also restores the default configuration)
#    4) Restore default configuration
#    5) Exit
# =============================================================================

set -u

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
CONFIG_FILE="$SCRIPT_DIR/java_config.ini"

# -----------------------------------------------------------------------------
# Colors / formatting (degrade gracefully when the terminal has no color)
# -----------------------------------------------------------------------------
# ANSI escapes are used directly (portable and free of terminfo quirks);
# they are disabled when stdout is not a terminal or the terminal is "dumb".
if [ -t 1 ] && [ "${TERM:-dumb}" != "dumb" ] && [ -z "${NO_COLOR:-}" ]; then
    ESC=$(printf '\033')
    BOLD="${ESC}[1m"; RESET="${ESC}[0m"
    RED="${ESC}[31m"; GREEN="${ESC}[32m"
    YELLOW="${ESC}[33m"; BLUE="${ESC}[34m"; CYAN="${ESC}[36m"
else
    BOLD=""; RESET=""; RED=""; GREEN=""; YELLOW=""; BLUE=""; CYAN=""
fi

info()  { printf '%s[INFO]%s  %s\n'  "$CYAN"   "$RESET" "$*"; }
ok()    { printf '%s[ OK ]%s  %s\n'  "$GREEN"  "$RESET" "$*"; }
warn()  { printf '%s[WARN]%s  %s\n'  "$YELLOW" "$RESET" "$*"; }
err()   { printf '%s[FAIL]%s  %s\n'  "$RED"    "$RESET" "$*" >&2; }

# -----------------------------------------------------------------------------
# Configuration - every value lives in java_config.ini, shared with the app
# -----------------------------------------------------------------------------
# Reads one key from the INI file. Keys are dotted (app.language, launcher.srcDir),
# which are NOT valid shell variable names, so the file is parsed - never sourced.
# Usage: ini_get <key> [fallback]
ini_get() {
    _key="$1"
    _fallback="${2:-}"
    _value=""
    if [ -f "$CONFIG_FILE" ]; then
        _value="$(awk -v key="$_key" '
            { sub(/\r$/, "") }                       # tolerate CRLF files
            /^[[:space:]]*[#;]/ { next }             # skip comments
            {
                eq = index($0, "=")
                if (eq == 0) next
                k = substr($0, 1, eq - 1)
                v = substr($0, eq + 1)
                gsub(/^[[:space:]]+|[[:space:]]+$/, "", k)
                gsub(/^[[:space:]]+|[[:space:]]+$/, "", v)
                if (k == key) { print v; exit }
            }' "$CONFIG_FILE")"
    fi
    [ -n "$_value" ] && printf '%s\n' "$_value" || printf '%s\n' "$_fallback"
}

# Prints the path of the bundled factory default shipped in a module's resources dir.
config_template() {
    for _t in "$SCRIPT_DIR"/*/src/main/resources/java_config.ini; do
        [ -f "$_t" ] && { printf '%s\n' "$_t"; return 0; }
    done
    return 1
}

# Overwrites the active java_config.ini with the bundled default - the same
# behaviour as the 'Default' button in the app's Config tab.
restore_config() {
    _template="$(config_template)" || {
        err "Bundled default not found: <module>/src/main/resources/java_config.ini"
        return 1
    }
    cp "$_template" "$CONFIG_FILE" || { err "Could not write $CONFIG_FILE"; return 1; }
    ok "Configuration restored from $_template"
    load_settings
    return 0
}

# (Re)reads every launcher setting; called at start-up and after a restore.
load_settings() {
    PROJECT_DIR_NAME="$(ini_get launcher.projectDir plantuml-gui-java)"
    SRC_DIR="$(ini_get launcher.srcDir src/main/java)"
    RES_DIR="$(ini_get launcher.resDir src/main/resources)"
    OUT_DIR="$(ini_get launcher.outDir bin)"
    MAIN_CLASS="$(ini_get launcher.mainClass com.diosaraiva.plantumlgui.Main)"
    CLEAN_DIRS="$(ini_get launcher.cleanDirs 'bin out build target temp output')"
    JAVAC_RELEASE="$(ini_get launcher.javacRelease)"

    # The app resolves resources relative to user.dir, so always run from the module
    PROJECT_DIR="$SCRIPT_DIR/$PROJECT_DIR_NAME"
    [ -d "$PROJECT_DIR" ] || PROJECT_DIR="$SCRIPT_DIR"

    MAIN_REL_PATH="$(printf '%s' "$MAIN_CLASS" | tr '.' '/')"
    MAIN_SRC_FILE="$SRC_DIR/$MAIN_REL_PATH.java"
    MAIN_CLASS_FILE="$OUT_DIR/$MAIN_REL_PATH.class"
}

# Missing active config? Recreate it from the bundled default.
if [ -f "$CONFIG_FILE" ]; then
    load_settings
else
    restore_config >/dev/null 2>&1 || load_settings
fi

# -----------------------------------------------------------------------------
# JDK detection
# -----------------------------------------------------------------------------
# Prints the major version of the given tool (java/javac), e.g. 8, 11, 21, 26.
# Prints 0 when the tool is missing or the version cannot be parsed.
java_major_version() {
    tool="$1"
    command -v "$tool" >/dev/null 2>&1 || { echo 0; return; }

    # Preferred source: java.specification.version (plain "8", "11", "25", ...)
    raw="$("$tool" -XshowSettings:properties -version 2>&1 \
           | awk -F'= *' '/java\.specification\.version/ { gsub(/[ \t]/, "", $2); print $2; exit }')"

    # Fallback: first line of `-version`, e.g. openjdk version "25.0.1" 2025-10-21
    # NOTE: split on the double quotes (awk -F'"'), never a greedy `.*"` regex,
    # which would swallow the version string and yield an empty result.
    if [ -z "$raw" ]; then
        raw="$("$tool" -version 2>&1 | awk -F'"' 'NR==1 { print $2; exit }')"
    fi

    case "$raw" in
        1.*)          printf '%s\n' "$raw" | cut -d. -f2 ;;   # legacy "1.8.0_xxx"
        [0-9]*)       printf '%s\n' "$raw" | cut -d. -f1 | tr -cd '0-9' ;;
        *)            echo 0 ;;
    esac
}

require_java() {
    if ! command -v java >/dev/null 2>&1; then
        err "'java' was not found on PATH. Install a JDK (17+ recommended) and retry."
        return 1
    fi
    return 0
}

require_javac() {
    if ! command -v javac >/dev/null 2>&1; then
        err "'javac' was not found on PATH. A full JDK (not just a JRE) is required."
        return 1
    fi
    return 0
}

pause() {
    printf '\n%sPress ENTER to return to the menu...%s' "$BOLD" "$RESET"
    read -r _dummy || true
    printf '\n'
}

# =============================================================================
# Option 1 - Run directly from sources (no javac, no .class files)
# =============================================================================
# Uses the java source-code launcher:
#   * Java 11+  -> single-file source programs (JEP 330)
#   * Java 22+  -> multi-file source programs (JEP 458): classes referenced by
#                  Main.java are compiled in memory from the source path.
# This project spans many classes, so Java 22+ is required for it to work.
run_from_source() {
    require_java || return 1
    jv="$(java_major_version java)"
    case "$jv" in ''|*[!0-9]*) jv=0 ;; esac   # never let a bad parse break `-lt`

    if [ "$jv" -eq 0 ]; then
        warn "Could not determine the Java version from '$(java -version 2>&1 | head -n 1)'."
        warn "Assuming it supports source-code execution and continuing."
    elif [ "$jv" -lt 11 ]; then
        err "Java $jv detected. Source-code execution requires Java 11+ (Java 22+ for this project)."
        err "Use option 2 (compile then run) instead."
        return 1
    fi
    if [ "$jv" -ne 0 ] && [ "$jv" -lt 22 ]; then
        warn "Java $jv detected. Multi-file source execution needs Java 22+ (JEP 458)."
        warn "This project has multiple classes, so the run will most likely fail."
        printf '%sTry anyway? [y/N]: %s' "$BOLD" "$RESET"
        read -r answer || answer=""
        case "$answer" in
            [yY]|[yY][eE][sS]) ;;
            *) info "Aborted. Use option 2 (compile then run)."; return 0 ;;
        esac
    fi

    if [ ! -f "$MAIN_SRC_FILE" ]; then
        err "Main source not found: $PROJECT_DIR/$MAIN_SRC_FILE"
        return 1
    fi

    # In source-file mode the launcher looks for the *other* .java files of the
    # program on the class path, so the source root must be on -cp. The resource
    # dir is appended so classpath resources (i18n, plantuml jar) resolve too.
    CP="$SRC_DIR"
    [ -d "$RES_DIR" ] && CP="$SRC_DIR:$RES_DIR"

    info "Running from sources (Java $jv, no .class files generated)..."
    info "Any javac warnings below come from the in-memory compilation step."
    # Working dir is the project dir, so on-disk resource lookup also works.
    java -cp "$CP" "$MAIN_SRC_FILE"
    status=$?
    if [ "$status" -eq 0 ]; then ok "Application exited normally."; else err "Application exited with code $status."; fi
    return "$status"
}

# =============================================================================
# Option 2 - Compile if needed, then run from compiled classes
# =============================================================================
# Freshness check:
#   The build is considered UP TO DATE when
#     (a) the output dir exists and contains .class files, AND
#     (b) the main class file exists, AND
#     (c) no *.java file under $SRC_DIR is newer than that main class file.
#   'find -newer <ref>' compares modification timestamps and behaves the same on
#   GNU (Linux) and BSD (macOS) find, avoiding `stat` portability problems.
#   If any source is newer, javac runs; otherwise compilation is skipped.
is_build_fresh() {
    [ -d "$OUT_DIR" ] || return 1
    [ -f "$MAIN_CLASS_FILE" ] || return 1
    # any compiled class at all?
    [ -n "$(find "$OUT_DIR" -name '*.class' -type f 2>/dev/null | head -n 1)" ] || return 1
    # any source newer than the reference class file?
    newer="$(find "$SRC_DIR" -name '*.java' -type f -newer "$MAIN_CLASS_FILE" 2>/dev/null | head -n 1)"
    [ -z "$newer" ]
}

compile_project() {
    require_javac || return 1
    info "Compiling sources into '$OUT_DIR'..."
    mkdir -p "$OUT_DIR" || return 1

    # Collect sources into an argument file (safe for large projects / long paths)
    sources_file="$(mktemp "${TMPDIR:-/tmp}/plantumlgui-sources.XXXXXX")" || return 1
    find "$SRC_DIR" -name '*.java' -type f > "$sources_file"
    if [ ! -s "$sources_file" ]; then
        err "No .java files found under '$SRC_DIR'."
        rm -f "$sources_file"
        return 1
    fi

    if [ -n "$JAVAC_RELEASE" ]; then
        javac --release "$JAVAC_RELEASE" -encoding UTF-8 -d "$OUT_DIR" "@$sources_file"
    else
        javac -encoding UTF-8 -d "$OUT_DIR" "@$sources_file"
    fi
    status=$?
    rm -f "$sources_file"

    if [ "$status" -ne 0 ]; then
        err "Compilation failed (exit code $status)."
        return "$status"
    fi
    ok "Compilation finished ($(find "$OUT_DIR" -name '*.class' -type f | wc -l | tr -d ' ') class files)."
    return 0
}

compile_and_run() {
    require_java || return 1

    if is_build_fresh; then
        ok "Compiled classes are up to date - skipping javac."
    else
        info "Compiled classes are missing or outdated."
        compile_project || return 1
    fi

    # Classpath: compiled classes + resources dir, so resources bundled on the
    # classpath (i18n/*.properties, plantuml/*.jar, ...) are found by the loader.
    CP="$OUT_DIR"
    [ -d "$RES_DIR" ] && CP="$OUT_DIR:$RES_DIR"

    info "Running $MAIN_CLASS from compiled classes..."
    java -cp "$CP" "$MAIN_CLASS"
    status=$?
    if [ "$status" -eq 0 ]; then ok "Application exited normally."; else err "Application exited with code $status."; fi
    return "$status"
}

# =============================================================================
# Option 3 - Clean build artifacts + restore the default configuration
# =============================================================================
clean_artifacts() {
    found=""
    for d in $CLEAN_DIRS; do
        [ -d "$d" ] && found="$found $d"
    done
    stray_classes="$(find "$SRC_DIR" -name '*.class' -type f 2>/dev/null | wc -l | tr -d ' ')"

    printf '\n%sThe following will be removed (under %s):%s\n' "$BOLD" "$PROJECT_DIR" "$RESET"
    if [ -z "$found" ] && [ "$stray_classes" -eq 0 ]; then
        printf '  %s- nothing, no build artifacts found%s\n' "$YELLOW" "$RESET"
    fi
    for d in $found; do
        count="$(find "$d" -type f 2>/dev/null | wc -l | tr -d ' ')"
        printf '  %s- %s/%s (%s files)\n' "$YELLOW" "$d" "$RESET" "$count"
    done
    if [ "$stray_classes" -gt 0 ]; then
        printf '  %s- %s stray .class file(s) under %s%s\n' "$YELLOW" "$stray_classes" "$SRC_DIR" "$RESET"
    fi
    printf '\n%sAnd this file will be reset to the bundled defaults:%s\n' "$BOLD" "$RESET"
    printf '  %s- %s%s\n' "$YELLOW" "$CONFIG_FILE" "$RESET"

    printf '\n%sConfirm clean? [y/N]: %s' "$BOLD" "$RESET"
    read -r answer || answer=""
    case "$answer" in
        [yY]|[yY][eE][sS]) ;;
        *) info "Clean cancelled - nothing was removed or reset."; return 0 ;;
    esac

    removed_dirs=0
    removed_files=0
    for d in $found; do
        n="$(find "$d" -type f 2>/dev/null | wc -l | tr -d ' ')"
        if rm -rf -- "$d"; then
            removed_dirs=$((removed_dirs + 1))
            removed_files=$((removed_files + n))
            printf '  %sremoved%s %s/\n' "$GREEN" "$RESET" "$d"
        else
            err "Could not remove $d"
        fi
    done
    if [ "$stray_classes" -gt 0 ]; then
        find "$SRC_DIR" -name '*.class' -type f -exec rm -f -- {} +
        removed_files=$((removed_files + stray_classes))
        printf '  %sremoved%s %s stray .class file(s)\n' "$GREEN" "$RESET" "$stray_classes"
    fi

    ok "Clean complete: $removed_dirs directory(ies), $removed_files file(s) removed."
    # A clean also puts the configuration back to its factory state.
    restore_config || return 1
    cd "$PROJECT_DIR" 2>/dev/null || true
    return 0
}

# =============================================================================
# Option 4 - Restore the default configuration only
# =============================================================================
restore_config_interactive() {
    _template="$(config_template)" || {
        err "Bundled default not found: <module>/src/main/resources/java_config.ini"
        return 1
    }
    printf '\n%sOverwrite the active configuration with the bundled defaults?%s\n' "$BOLD" "$RESET"
    printf '  %sfrom%s %s\n' "$CYAN" "$RESET" "$_template"
    printf '  %sto  %s %s\n' "$CYAN" "$RESET" "$CONFIG_FILE"

    printf '\n%sConfirm restore? [y/N]: %s' "$BOLD" "$RESET"
    read -r answer || answer=""
    case "$answer" in
        [yY]|[yY][eE][sS]) ;;
        *) info "Restore cancelled - the configuration was left untouched."; return 0 ;;
    esac

    restore_config || return 1
    cd "$PROJECT_DIR" 2>/dev/null || true
    return 0
}

# =============================================================================
# Menu
# =============================================================================
show_menu() {
    printf '\n%s%s========================================%s\n' "$BOLD" "$BLUE" "$RESET"
    printf '%s%s         PlantUML GUI - Launcher        %s\n'   "$BOLD" "$BLUE" "$RESET"
    printf '%s%s========================================%s\n'   "$BOLD" "$BLUE" "$RESET"
    printf '  project : %s\n' "$PROJECT_DIR"
    printf '  config  : %s\n' "$CONFIG_FILE"
    printf '  java    : %s (major %s)\n' "$(java -version 2>&1 | head -n 1)" "$(java_major_version java)"
    if is_build_fresh; then
        printf '  build   : %sup to date%s\n' "$GREEN" "$RESET"
    elif [ -f "$MAIN_CLASS_FILE" ]; then
        printf '  build   : %soutdated%s\n' "$YELLOW" "$RESET"
    else
        printf '  build   : %snot compiled%s\n' "$RED" "$RESET"
    fi
    printf '%s----------------------------------------%s\n' "$BLUE" "$RESET"
    printf '  %s1)%s Run without compiling (source mode)\n'  "$BOLD" "$RESET"
    printf '  %s2)%s Compile (if needed) and run\n'          "$BOLD" "$RESET"
    printf '  %s3)%s Clean build artifacts + reset config\n' "$BOLD" "$RESET"
    printf '  %s4)%s Restore default configuration\n'        "$BOLD" "$RESET"
    printf '  %s5)%s Exit\n'                                 "$BOLD" "$RESET"
    printf '%s----------------------------------------%s\n' "$BLUE" "$RESET"
}

main() {
    cd "$PROJECT_DIR" || { err "Cannot enter project directory: $PROJECT_DIR"; exit 1; }
    require_java || exit 1

    while true; do
        show_menu
        printf '%sChoose an option [1-5]: %s' "$BOLD" "$RESET"
        if ! read -r choice; then
            printf '\n'
            info "Input closed - exiting."
            exit 0
        fi
        choice="$(printf '%s' "$choice" | tr -d '[:space:]')"   # trim whitespace

        case "$choice" in
            1) run_from_source; pause ;;
            2) compile_and_run; pause ;;
            3) clean_artifacts; pause ;;
            4) restore_config_interactive; pause ;;
            5) info "Bye!"; exit 0 ;;
            "") err "No option entered. Please type a number between 1 and 5." ;;
            *) err "Invalid option: '$choice'. Please type a number between 1 and 5." ;;
        esac
    done
}

main "$@"
