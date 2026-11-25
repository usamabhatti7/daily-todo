#!/usr/bin/env bash
set -euo pipefail
V="${1:?usage: bump-version.sh 1.2}"
cd "$(dirname "$0")/.."
G=android/app/build.gradle.kts
CODE=$(grep -o "versionCode = [0-9]*" "$G" | grep -o "[0-9]*")
sed -i "s/versionCode = [0-9]*; versionName = \"[^\"]*\"/versionCode = $((CODE+1)); versionName = \"$V\"/" "$G"
sed -i "s/\"version\": \"[^\"]*\"/\"version\": \"$V.0\"/" desktop/package.json desktop/src-tauri/tauri.conf.json
sed -i "0,/^version = \"[^\"]*\"/s//version = \"$V.0\"/" desktop/src-tauri/Cargo.toml
sed -i "s/Daily To-Do v[0-9.]*/Daily To-Do v$V/" web/index.html desktop/src/index.html windows-quick-install/daily-todo.html
echo "Version set to v$V. Add a section to CHANGELOG.md, then commit and tag v$V."
