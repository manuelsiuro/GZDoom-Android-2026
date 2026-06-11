#include "DoomMap.h"
#include <cmath>
#include <algorithm>

Sector::Sector(int floorHeight, int ceilingHeight, const std::string& floorTexture, const std::string& ceilingTexture, int lightLevel, int special, int tag) {
    FloorHeight = std::max(-32768, std::min(32767, floorHeight));
    CeilingHeight = std::max(FloorHeight, std::min(32767, ceilingHeight));
    FloorTexture = floorTexture;
    CeilingTexture = ceilingTexture;
    LightLevel = std::max(0, std::min(255, lightLevel));
    Special = std::max(0, std::min(32767, special));
    Tag = std::max(0, std::min(32767, tag));
}

std::vector<uint8_t> Sector::ToBytes() const {
    std::vector<uint8_t> bytes;
    
    auto pushShort = [&bytes](int16_t val) {
        auto ptr = reinterpret_cast<const uint8_t*>(&val);
        bytes.insert(bytes.end(), ptr, ptr + 2);
    };

    pushShort(static_cast<int16_t>(FloorHeight));
    pushShort(static_cast<int16_t>(CeilingHeight));
    
    std::vector<uint8_t> fTex = WadFile::GetBytesFromString(FloorTexture);
    bytes.insert(bytes.end(), fTex.begin(), fTex.end());
    
    std::vector<uint8_t> cTex = WadFile::GetBytesFromString(CeilingTexture);
    bytes.insert(bytes.end(), cTex.begin(), cTex.end());
    
    pushShort(static_cast<int16_t>(LightLevel));
    pushShort(static_cast<int16_t>(Special));
    pushShort(static_cast<int16_t>(Tag));
    
    return bytes;
}

Linedef::Linedef(int vertex1, int vertex2, LinedefFlags flags, int type, int tag, int sidedefLeft, int sidedefRight)
    : Vertex1(vertex1), Vertex2(vertex2), Flags(flags), Type(type), Tag(tag), SidedefRight(sidedefRight), SidedefLeft(sidedefLeft) {}

std::vector<uint8_t> Linedef::ToBytes() const {
    std::vector<uint8_t> bytes;
    
    auto pushShort = [&bytes](int16_t val) {
        auto ptr = reinterpret_cast<const uint8_t*>(&val);
        bytes.insert(bytes.end(), ptr, ptr + 2);
    };

    pushShort(static_cast<int16_t>(Vertex1));
    pushShort(static_cast<int16_t>(Vertex2));
    pushShort(static_cast<int16_t>(Flags));
    pushShort(static_cast<int16_t>(Type));
    pushShort(static_cast<int16_t>(Tag));
    pushShort(static_cast<int16_t>(SidedefRight));
    pushShort(static_cast<int16_t>(SidedefLeft));

    return bytes;
}

Sidedef::Sidedef(int xOffset, int yOffset, const std::string& upperTexture, const std::string& lowerTexture, const std::string& middleTexture, int sector)
    : XOffset(xOffset), YOffset(yOffset), UpperTexture(upperTexture), LowerTexture(lowerTexture), MiddleTexture(middleTexture), Sector(sector) {}

std::vector<uint8_t> Sidedef::ToBytes() const {
    std::vector<uint8_t> bytes;
    
    auto pushShort = [&bytes](int16_t val) {
        auto ptr = reinterpret_cast<const uint8_t*>(&val);
        bytes.insert(bytes.end(), ptr, ptr + 2);
    };

    pushShort(static_cast<int16_t>(XOffset));
    pushShort(static_cast<int16_t>(YOffset));
    
    std::vector<uint8_t> uTex = WadFile::GetBytesFromString(UpperTexture);
    bytes.insert(bytes.end(), uTex.begin(), uTex.end());
    
    std::vector<uint8_t> lTex = WadFile::GetBytesFromString(LowerTexture);
    bytes.insert(bytes.end(), lTex.begin(), lTex.end());
    
    std::vector<uint8_t> mTex = WadFile::GetBytesFromString(MiddleTexture);
    bytes.insert(bytes.end(), mTex.begin(), mTex.end());
    
    pushShort(static_cast<int16_t>(Sector));
    
    return bytes;
}

Vertex::Vertex(const Point& pt) : X(pt.X), Y(pt.Y) {}

std::vector<uint8_t> Vertex::ToBytes() const {
    std::vector<uint8_t> bytes;
    
    auto pushShort = [&bytes](int16_t val) {
        auto ptr = reinterpret_cast<const uint8_t*>(&val);
        bytes.insert(bytes.end(), ptr, ptr + 2);
    };

    pushShort(static_cast<int16_t>(X));
    pushShort(static_cast<int16_t>(Y));
    
    return bytes;
}

Thing::Thing(int x, int y, int type, int angle, ThingOptions options)
    : X(x), Y(y), Angle(angle), Type(type), Options(options) {}

std::vector<uint8_t> Thing::ToBytes() const {
    std::vector<uint8_t> bytes;
    
    auto pushShort = [&bytes](int16_t val) {
        auto ptr = reinterpret_cast<const uint8_t*>(&val);
        bytes.insert(bytes.end(), ptr, ptr + 2);
    };

    pushShort(static_cast<int16_t>(X));
    pushShort(static_cast<int16_t>(Y));
    pushShort(static_cast<int16_t>(Angle));
    pushShort(static_cast<int16_t>(Type));
    pushShort(static_cast<int16_t>(Options));

    return bytes;
}

DoomMap::DoomMap(const std::string& name) : Name(name) {}

void DoomMap::AddToWad(WadFile& wad) {
    // Lumps MUST be written in canonical Doom order. Older/ordered map loaders
    // (e.g. the GZDoom 1.9.0 engine used by GZDoom-for-Android) walk the lumps
    // following the map marker against a fixed table — see GetMapIndex() in
    // gzdoom p_setup.cpp: {THINGS, LINEDEFS, SIDEDEFS, VERTEXES, SEGS, SSECTORS,
    // NODES, SECTORS, REJECT, BLOCKMAP, BEHAVIOR}. The first lump after the
    // marker must be THINGS or the load aborts with
    // I_Error("'THINGS' not found in <map>"). We omit the optional node lumps
    // (SEGS/SSECTORS/NODES/REJECT/BLOCKMAP); the engine builds them on load.
    // (The C# original used a different order because it targets modern desktop
    // GZDoom, which collects map lumps order-independently.)
    wad.AddLump(Name, std::vector<uint8_t>());

    std::vector<uint8_t> thingsBytes;
    for (const auto& t : Things) { auto b = t.ToBytes(); thingsBytes.insert(thingsBytes.end(), b.begin(), b.end()); }
    wad.AddLump("THINGS", thingsBytes);

    std::vector<uint8_t> linedefsBytes;
    for (const auto& l : Linedefs) { auto b = l.ToBytes(); linedefsBytes.insert(linedefsBytes.end(), b.begin(), b.end()); }
    wad.AddLump("LINEDEFS", linedefsBytes);

    std::vector<uint8_t> sidedefsBytes;
    for (const auto& s : Sidedefs) { auto b = s.ToBytes(); sidedefsBytes.insert(sidedefsBytes.end(), b.begin(), b.end()); }
    wad.AddLump("SIDEDEFS", sidedefsBytes);

    std::vector<uint8_t> vertexesBytes;
    for (const auto& v : Vertices) { auto b = v.ToBytes(); vertexesBytes.insert(vertexesBytes.end(), b.begin(), b.end()); }
    wad.AddLump("VERTEXES", vertexesBytes);

    std::vector<uint8_t> sectorsBytes;
    for (const auto& s : Sectors) { auto b = s.ToBytes(); sectorsBytes.insert(sectorsBytes.end(), b.begin(), b.end()); }
    wad.AddLump("SECTORS", sectorsBytes);
}

int DoomMap::AddVertex(const Point& coordinates) {
    for (size_t i = 0; i < Vertices.size(); i++) {
        if (Vertices[i].X == coordinates.X && Vertices[i].Y == coordinates.Y) {
            return i;
        }
    }
    Vertices.emplace_back(coordinates);
    return Vertices.size() - 1;
}

void DoomMap::GetMapBoundaries(int& minX, int& minY, int& maxX, int& maxY) const {
    minX = 0; minY = 0; maxX = 0; maxY = 0;
    if (Vertices.empty()) return;

    minX = Vertices[0].X; maxX = Vertices[0].X;
    minY = Vertices[0].Y; maxY = Vertices[0].Y;

    for (const auto& v : Vertices) {
        if (v.X < minX) minX = v.X;
        if (v.X > maxX) maxX = v.X;
        if (v.Y < minY) minY = v.Y;
        if (v.Y > maxY) maxY = v.Y;
    }
}

void DoomMap::Dispose() {
}
