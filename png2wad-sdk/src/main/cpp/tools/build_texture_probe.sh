#!/usr/bin/env bash
# Builds the host-side TextureReader diagnostic (see texture_probe.cpp).
#   ./build_texture_probe.sh && /tmp/texture_probe path/to/freedoom2.wad
set -euo pipefail

CPP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
OUT="${1:-/tmp/texture_probe}"

clang++ -std=c++17 -O2 -I"$CPP_DIR" \
    "$CPP_DIR/tools/texture_probe.cpp" \
    "$CPP_DIR/TextureReader.cpp" \
    "$CPP_DIR/WadFile.cpp" \
    -o "$OUT"

echo "Built: $OUT"
