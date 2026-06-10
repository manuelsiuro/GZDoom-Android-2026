#!/bin/bash
# JNI signature gate: re-run javap on converted (Kotlin) classes and diff vs golden.
# Usage: bash .jni-golden/verify.sh
set -u
cd "$(dirname "$0")/.."
GOLD=.jni-golden

# AGP puts Kotlin output under tmp/kotlin-classes/debug and Java under javac/.../classes.
# Build a combined classpath covering both modules, both languages.
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
  if [ ! -f "$GOLD/$cls.txt" ]; then echo "?? no golden for $cls"; return; fi
  # Compare only native methods and field descriptors (the JNI-relevant surface).
  if diff <(grep -E "native|descriptor|gv;|swapBuffers|fopen|flen|JNI" "$GOLD/$cls.txt") \
          <(echo "$out" | grep -E "native|descriptor|gv;|swapBuffers|fopen|flen|JNI") >/dev/null; then
    echo "OK   $cls"
  else
    echo "DIFF $cls"
    diff <(grep -E "native|descriptor|gv;|swapBuffers|fopen|flen|JNI" "$GOLD/$cls.txt") \
         <(echo "$out" | grep -E "native|descriptor|gv;|swapBuffers|fopen|flen|JNI")
  fi
}

check "$DOOM_CP" net.nullsum.freedoom.NativeLib
check "$DOOM_CP" com.beloko.libsdl.SDLLib
check "$DOOM_CP" org.libsdl.app.SDLActivity
check "$TC_CP" com.beloko.AssetFileAccess
check "$TC_CP" com.beloko.touchcontrols.TouchControlsEditing
check "$TC_CP" 'com.beloko.touchcontrols.TouchControlsEditing$ControlInfo'
check "$TC_CP" com.beloko.touchcontrols.ActionInput
