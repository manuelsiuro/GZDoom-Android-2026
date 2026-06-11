#!/usr/bin/env bash
# Builds the host-side diagnostic CLI (see png2wad_cli.cpp).
# Run from the cpp directory or anywhere; paths are resolved relative to this script.
set -euo pipefail

CPP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="${1:-/tmp/png2wad_cli}"

clang++ -std=c++17 -O2 -I"$CPP_DIR" \
    "$CPP_DIR/tools/png2wad_cli.cpp" \
    "$CPP_DIR/Generator.cpp" \
    "$CPP_DIR/DoomMap.cpp" \
    "$CPP_DIR/WadFile.cpp" \
    "$CPP_DIR/Config.cpp" \
    "$CPP_DIR/INIFile.cpp" \
    "$CPP_DIR/Toolbox.cpp" \
    -o "$OUT"

echo "Built: $OUT"
