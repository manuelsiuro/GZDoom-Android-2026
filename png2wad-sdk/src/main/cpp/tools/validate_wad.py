#!/usr/bin/env python3
"""Validate a png2wad-generated PWAD the way the GZDoom 1.9.0 Android engine does.

Checks, per map:
  - PWAD magic + directory consistency (every lump's data lies within the file).
  - Canonical lump ORDER after the marker, simulating gzdoom p_setup.cpp GetMapIndex():
    required THINGS, LINEDEFS, SIDEDEFS, VERTEXES, SECTORS in order, with the
    optional SEGS/SSECTORS/NODES/REJECT/BLOCKMAP/BEHAVIOR allowed to be absent.
    A wrong order here is exactly what makes the engine abort with
    "'THINGS' not found in <map>".
  - Per-lump record-size multiples (THINGS%10, LINEDEFS%14, SIDEDEFS%30,
    VERTEXES%4, SECTORS%26).
  - Geometry cross-references: linedef vertex/sidedef indices in range, every
    linedef has a right sidedef, sidedef sector indices in range, >=1 player-1
    start, no zero-length lines.

Usage: validate_wad.py <file.wad>   (exit 0 = PASS, 1 = FAIL)
"""
import sys, struct

# gzdoom GetMapIndex check table (name, required)
CHECK = [("", True), ("THINGS", True), ("LINEDEFS", True), ("SIDEDEFS", True),
         ("VERTEXES", True), ("SEGS", False), ("SSECTORS", False), ("NODES", False),
         ("SECTORS", True), ("REJECT", False), ("BLOCKMAP", False), ("BEHAVIOR", False)]
REC = {"THINGS": 10, "LINEDEFS": 14, "SIDEDEFS": 30, "VERTEXES": 4, "SECTORS": 26}


def fail(msg):
    print(f"  *** FAIL: {msg}")
    return False


def is_marker(name):
    return name.startswith("MAP") or (len(name) == 4 and name[0] == "E" and name[2] == "M")


def check_map_order(names_after_marker, mapname):
    """Returns True if the lump sequence passes GetMapIndex (stops at next non-map lump)."""
    last = 0
    ok = True
    for nm in names_after_marker:
        # advance through the table from last+1 like GetMapIndex
        found = None
        i = last + 1
        while i < len(CHECK):
            if nm.upper() == CHECK[i][0]:
                found = i
                break
            if CHECK[i][1]:  # required and not matched -> this lump ends the map
                found = -2
                break
            i += 1
        if found is None:      # ran off the table: next map begins
            break
        if found == -2:
            # next map marker is fine; anything else before all required => engine I_Error
            if is_marker(nm):
                break
            ok = fail(f"{mapname}: lump '{nm}' out of order -> engine would abort "
                      f"('{CHECK[last+1][0]}' expected next)")
            break
        last = found
    # did we reach SECTORS (index 8)?
    if last < 8:
        ok = fail(f"{mapname}: never reached required SECTORS in order (stopped at table idx {last})")
    return ok


def main():
    fn = sys.argv[1]
    data = open(fn, "rb").read()
    n = len(data)
    magic = data[0:4].decode("ascii", "replace")
    numlumps, diroff = struct.unpack("<ii", data[4:12])
    print(f"file={fn} size={n} magic={magic!r} numlumps={numlumps} diroffset={diroff}")
    ok = True
    if magic != "PWAD":
        ok = fail("expected PWAD")
    if not (12 <= diroff <= n and diroff + 16 * numlumps <= n):
        return done(fail("directory out of range"))

    lumps = []
    for i in range(numlumps):
        o = diroff + 16 * i
        pos, size = struct.unpack("<ii", data[o:o + 8])
        name = data[o + 8:o + 16].split(b"\x00")[0].decode("ascii", "replace")
        lumps.append((name, pos, size))
        if size and not (12 <= pos and pos + size <= diroff):
            ok = fail(f"lump '{name}' data [{pos},{pos+size}) outside data region")
        if name in REC and size % REC[name]:
            ok = fail(f"lump '{name}' size {size} not a multiple of {REC[name]}")

    # group into maps and check order + geometry
    i = 0
    L = {nm: data[pos:pos + size] for nm, pos, size in lumps}
    markers = [k for k in (nm for nm, _, _ in lumps) if is_marker(k)]
    print("map markers:", markers)
    while i < len(lumps):
        nm = lumps[i][0]
        if is_marker(nm):
            after = [lumps[j][0] for j in range(i + 1, len(lumps))]
            ok &= check_map_order(after, nm)
        i += 1

    ok &= check_geometry(L)
    return done(ok)


def check_geometry(L):
    if not all(k in L for k in ("VERTEXES", "SIDEDEFS", "LINEDEFS", "SECTORS", "THINGS")):
        return fail("missing one of the 5 required map lumps")
    verts = [struct.unpack("<hh", L["VERTEXES"][i:i+4]) for i in range(0, len(L["VERTEXES"]), 4)]
    nsd = len(L["SIDEDEFS"]) // 30
    nsc = len(L["SECTORS"]) // 26
    nv = len(verts)
    ok = True
    for idx in range(0, len(L["LINEDEFS"]), 14):
        v1, v2, fl, ty, tg, rt, lf = struct.unpack("<hhhhhhh", L["LINEDEFS"][idx:idx+14])
        li = idx // 14
        if not (0 <= v1 < nv and 0 <= v2 < nv):
            ok = fail(f"linedef {li} bad vertex {v1},{v2}")
        elif verts[v1] == verts[v2]:
            ok = fail(f"linedef {li} zero-length")
        if rt == -1:
            ok = fail(f"linedef {li} has no right sidedef")
        if rt != -1 and not (0 <= rt < nsd):
            ok = fail(f"linedef {li} bad right sidedef {rt}")
        if lf != -1 and not (0 <= lf < nsd):
            ok = fail(f"linedef {li} bad left sidedef {lf}")
    for idx in range(0, len(L["SIDEDEFS"]), 30):
        sec, = struct.unpack("<h", L["SIDEDEFS"][idx+28:idx+30])
        if not (0 <= sec < nsc):
            ok = fail(f"sidedef {idx//30} bad sector {sec}")
    p1 = sum(1 for i in range(0, len(L["THINGS"]), 10)
             if struct.unpack("<hhhhh", L["THINGS"][i:i+10])[3] == 1)
    if p1 < 1:
        ok = fail("no player-1 start (type 1)")
    print(f"geometry: verts={nv} sidedefs={nsd} sectors={nsc} "
          f"linedefs={len(L['LINEDEFS'])//14} things={len(L['THINGS'])//10} player1starts={p1}")
    return ok


def done(ok):
    print("RESULT:", "PASS" if ok else "FAIL")
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
