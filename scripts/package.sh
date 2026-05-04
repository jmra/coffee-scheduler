#!/usr/bin/env bash
set -euo pipefail

APP_NAME="Coffee Scheduler"
APP_VERSION="0.1.0"
MAIN_CLASS="com.coffeescheduler.ui.Launcher"
MAIN_JAR="coffee-scheduler-0.1.0-SNAPSHOT.jar"
JDK_MODULES="java.base,java.desktop,java.logging,java.security.jgss,java.xml.crypto,jdk.unsupported"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
TARGET="$PROJECT_DIR/target"
LIB_DIR="$TARGET/lib"
INPUT_DIR="$TARGET/jpackage-input"
RUNTIME_DIR="$TARGET/runtime"
DIST_DIR="$TARGET/dist"

echo "=== Building project ==="
mvn -f "$PROJECT_DIR/pom.xml" clean package -DskipTests -q

echo "=== Preparing jpackage input ==="
rm -rf "$INPUT_DIR" "$RUNTIME_DIR" "$DIST_DIR"
mkdir -p "$INPUT_DIR"
cp "$TARGET/$MAIN_JAR" "$INPUT_DIR/"
cp "$LIB_DIR"/*.jar "$INPUT_DIR/"

echo "=== Creating custom JDK runtime with jlink ==="
jlink \
    --add-modules "$JDK_MODULES" \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress zip-6 \
    --output "$RUNTIME_DIR"

RUNTIME_SIZE=$(du -sh "$RUNTIME_DIR" | cut -f1)
echo "    Runtime size: $RUNTIME_SIZE"

echo "=== Packaging with jpackage ==="
# Detect platform
case "$(uname -s)" in
    Linux*)  TYPE="app-image" ;;
    Darwin*) TYPE="app-image" ;;
    MINGW*|MSYS*|CYGWIN*) TYPE="app-image" ;;
    *) TYPE="app-image" ;;
esac

jpackage \
    --name "$APP_NAME" \
    --app-version "$APP_VERSION" \
    --input "$INPUT_DIR" \
    --main-jar "$MAIN_JAR" \
    --main-class "$MAIN_CLASS" \
    --runtime-image "$RUNTIME_DIR" \
    --type "$TYPE" \
    --dest "$DIST_DIR"

echo "=== Done ==="
DIST_SIZE=$(du -sh "$DIST_DIR" | cut -f1)
echo "Output: $DIST_DIR"
echo "Total size: $DIST_SIZE"
ls -la "$DIST_DIR/"
