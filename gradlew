#!/bin/sh
APP_HOME=$(cd "$(dirname "$0")" && pwd -P)
CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

if [ -n "$JAVA_HOME" ]; then JAVACMD="$JAVA_HOME/bin/java"
else JAVACMD="java"; fi

# Download wrapper jar if missing
if [ ! -s "$CLASSPATH" ] || [ "$(wc -c < "$CLASSPATH" 2>/dev/null || echo 0)" -lt 10000 ]; then
    echo "Downloading gradle-wrapper.jar..." >&2
    mkdir -p "$APP_HOME/gradle/wrapper"
    if command -v curl >/dev/null 2>&1; then
        curl -fsSL "https://github.com/gradle/gradle/raw/v8.12.0/gradle/wrapper/gradle-wrapper.jar" \
            -o "$CLASSPATH" 2>/dev/null || true
    fi
fi

exec "$JAVACMD" \
    -Dorg.gradle.appname="$(basename "$0")" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain "$@"
