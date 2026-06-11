#pragma once
#include <string>
#include <vector>
#include <cstdint>
#include "DoomDefines.h"
#include "Toolbox.h"
#include "WadFile.h"

struct Sector {
    int FloorHeight;
    int CeilingHeight;
    std::string FloorTexture;
    std::string CeilingTexture;
    int LightLevel;
    int Special;
    int Tag;

    Sector(int floorHeight, int ceilingHeight, const std::string& floorTexture, const std::string& ceilingTexture, int lightLevel, int special = 0, int tag = 0);
    std::vector<uint8_t> ToBytes() const;
};

struct Linedef {
    int Vertex1;
    int Vertex2;
    LinedefFlags Flags;
    int Type;
    int Tag;
    int SidedefRight;
    int SidedefLeft;

    Linedef(int vertex1, int vertex2, LinedefFlags flags, int type, int tag, int sidedefLeft, int sidedefRight);
    std::vector<uint8_t> ToBytes() const;
};

struct Sidedef {
    int XOffset;
    int YOffset;
    std::string UpperTexture;
    std::string LowerTexture;
    std::string MiddleTexture;
    int Sector;

    Sidedef(int xOffset, int yOffset, const std::string& upperTexture, const std::string& lowerTexture, const std::string& middleTexture, int sector);
    std::vector<uint8_t> ToBytes() const;
};

struct Vertex {
    int X;
    int Y;
    
    Vertex(const Point& pt);
    std::vector<uint8_t> ToBytes() const;
};

struct Thing {
    int X;
    int Y;
    int Angle;
    int Type;
    ThingOptions Options;

    Thing(int x, int y, int type, int angle = 0, ThingOptions options = ThingOptions::AllSkills);
    std::vector<uint8_t> ToBytes() const;
};

class DoomMap {
public:
    std::string Name;
    std::vector<Linedef> Linedefs;
    std::vector<Sector> Sectors;
    std::vector<Sidedef> Sidedefs;
    std::vector<Thing> Things;
    std::vector<Vertex> Vertices;

    DoomMap(const std::string& name);
    void AddToWad(WadFile& wad);
    int AddVertex(const Point& coordinates);
    void GetMapBoundaries(int& minX, int& minY, int& maxX, int& maxY) const;
    void Dispose();
};
