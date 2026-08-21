#!/bin/bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
VERSION="9.5.0"
CACHE="$HOME/.gradle/organiza-bootstrap"
ZIP="$CACHE/gradle-$VERSION-bin.zip"
DIST="$CACHE/gradle-$VERSION"
GEN="$CACHE/wrapper-generator"

mkdir -p "$CACHE"

if command -v gradle >/dev/null 2>&1; then
  GRADLE_BIN="$(command -v gradle)"
else
  if [ ! -x "$DIST/bin/gradle" ]; then
    echo "A descarregar Gradle $VERSION..."
    rm -f "$ZIP"
    curl -fL "https://services.gradle.org/distributions/gradle-$VERSION-bin.zip" -o "$ZIP"
    rm -rf "$DIST"
    unzip -q "$ZIP" -d "$CACHE"
  fi
  GRADLE_BIN="$DIST/bin/gradle"
fi

rm -rf "$GEN"
mkdir -p "$GEN"
printf 'rootProject.name = "wrapper-generator"\n' > "$GEN/settings.gradle"

(
  cd "$GEN"
  "$GRADLE_BIN" --no-daemon wrapper --gradle-version "$VERSION" --distribution-type bin
)

rm -rf "$ROOT/gradle/wrapper"
mkdir -p "$ROOT/gradle"
cp -R "$GEN/gradle/wrapper" "$ROOT/gradle/"
cp "$GEN/gradlew" "$ROOT/gradlew"
cp "$GEN/gradlew.bat" "$ROOT/gradlew.bat"
chmod +x "$ROOT/gradlew"

echo
echo "Gradle Wrapper $VERSION criado com sucesso em:"
echo "$ROOT"
echo "Podes agora abrir/sincronizar o projeto no Android Studio."
