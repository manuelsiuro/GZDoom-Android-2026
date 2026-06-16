#!/bin/bash
# JNI signature gate: re-run javap on the seam classes and diff vs golden.
# The native side (GZDoom 4.15 glue + libtouchcontrols + libsaffal) resolves
# these classes/methods by name at runtime, so signature drift = runtime crash.
# Usage: bash .jni-golden/verify.sh        (after a :doom:compileDebug* build)
# Regenerate: bash .jni-golden/verify.sh --update
set -u
cd "$(dirname "$0")/.."
GOLD=.jni-golden
UPDATE=${1:-}

# AGP puts Kotlin output under tmp/kotlin-classes/debug and Java under javac/.../classes.
find_cp() {
  local module=$1
  echo "$module/build/tmp/kotlin-classes/debug:$module/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes"
}
DOOM_CP="$(find_cp doom)"
TC_CP="$(find_cp touchcontrols)"

check() {
  local cp=$1 cls=$2
  local out
  out=$(javap -s -p -classpath "$cp" "$cls" 2>&1)
  if [ "$UPDATE" = "--update" ]; then
    echo "$out" > "$GOLD/$cls.txt"
    echo "WROTE $cls"
    return
  fi
  if [ ! -f "$GOLD/$cls.txt" ]; then echo "?? no golden for $cls"; return; fi
  # Compare only native methods and field descriptors (the JNI-relevant surface).
  if diff <(grep -E "native|descriptor" "$GOLD/$cls.txt") \
          <(echo "$out" | grep -E "native|descriptor") >/dev/null; then
    echo "OK   $cls"
  else
    echo "DIFF $cls"
    diff <(grep -E "native|descriptor" "$GOLD/$cls.txt") \
         <(echo "$out" | grep -E "native|descriptor")
  fi
}

# Engine glue (Java_org_libsdl_app_NativeLib_*)
check "$DOOM_CP" org.libsdl.app.NativeLib
# SDL2 Java glue (org/libsdl/app2012/* registered in JNI_OnLoad)
check "$DOOM_CP" org.libsdl.app2012.SDLActivity
check "$DOOM_CP" org.libsdl.app2012.SDLAudioManager
check "$DOOM_CP" org.libsdl.app2012.SDLControllerManager
# Called by name from libtouchcontrols (JNITouchControlsUtils.cpp)
check "$DOOM_CP" org.libsdl.app.NativeConsoleBox
check "$DOOM_CP" com.opentouchgaming.androidcore.AssetFileAccess
# SAFFAL Java<->native seam
check "$DOOM_CP" com.opentouchgaming.saffal.FileJNI
check "$DOOM_CP" com.opentouchgaming.saffal.UtilsSAF
# Touch controls editing callbacks (resolved by name from libtouchcontrols)
check "$TC_CP" com.beloko.touchcontrols.TouchControlsEditing
check "$TC_CP" 'com.beloko.touchcontrols.TouchControlsEditing$ControlInfo'
check "$TC_CP" com.beloko.touchcontrols.TouchControlsSettings
check "$TC_CP" com.beloko.touchcontrols.CustomCommands
check "$TC_CP" com.beloko.touchcontrols.ShowKeyboard
