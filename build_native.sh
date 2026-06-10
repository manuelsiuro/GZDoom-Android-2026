#!/bin/bash
# Builds the native engine (.so) into doom/src/main/libs/<abi>/ via ndk-build.
# Gradle does NOT build the native code; it only packages the .so produced here
# (doom/build.gradle.kts: jniLibs.srcDirs("src/main/libs")). Run this whenever the
# native sources under doom/src/main/jni change, then `./gradlew :doom:assembleDebug`.
#
# The native stack is emileb's maintained, FMOD-free Freedoom/GZDoom engine
# (github.com/emileb/gzdoom @ freedoom_for_android_gz1.9_no_fmod) plus its deps,
# vendored under doom/src/main/jni. Build/ABI/toolchain settings: jni/Application.mk.
set -e

# Locate the NDK (honour env, else highest installed under the SDK).
if   [[ -n "$ANDROID_NDK_HOME" ]]; then NDK_DIR="$ANDROID_NDK_HOME"
elif [[ -n "$ANDROID_NDK_ROOT" ]]; then NDK_DIR="$ANDROID_NDK_ROOT"
else
    SDK_DIR="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}"
    NDK_DIR=$(ls -d "$SDK_DIR"/ndk/* 2>/dev/null | sort -V | tail -1)
fi
NDK_BUILD="$NDK_DIR/ndk-build"
[[ -x "$NDK_BUILD" ]] || { echo "ERROR: ndk-build not found. Set ANDROID_NDK_HOME." >&2; exit 1; }
echo "Using NDK: $NDK_DIR"

PROJ="$(cd "$(dirname "$0")" && pwd)/doom/src/main"
NDK_PROJECT_PATH="$PROJ" "$NDK_BUILD" \
    NDK_APPLICATION_MK="$PROJ/jni/Application.mk" -j"$(sysctl -n hw.ncpu 2>/dev/null || nproc)" "$@"

echo "Done. .so installed under doom/src/main/libs/{armeabi-v7a,arm64-v8a}/"
