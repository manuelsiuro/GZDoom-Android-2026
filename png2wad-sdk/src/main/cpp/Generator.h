#pragma once

#include "DoomDefines.h"
#include "DoomMap.h"
#include "Config.h"
#include "Toolbox.h"
#include <string>
#include <vector>

using namespace PNG2WAD::Config;

struct SectorInfo {
    TileType Type;
    int CeilingHeight;
    std::string CeilingTexture;
    int FloorHeight;
    std::string FloorTexture;
    int LightLevel;
    int LinedefSpecial;
    int SectorSpecial;
    std::string WallTexture;
    std::string WallTextureUpper;
    std::string WallTextureLower;

    SectorInfo(TileType type, const PreferencesTheme& theme, const std::vector<std::string>& themeTextures);
};

class ThingsGenerator {
private:
    const Preferences& PreferencesRef;
    std::vector<Point> FreeTiles;

    void AddThings(DoomMap& map, ThingCategory thingCategory, int minCount, int maxCount, ThingsGeneratorFlags generationFlags = ThingsGeneratorFlags::None);
    void AddThings(DoomMap& map, int count, ThingsGeneratorFlags generationFlags, const std::vector<int>& thingTypes);
    void AddPlayerStarts(DoomMap& map, const std::vector<std::vector<TileType>>& subTiles);
    static void AddThing(DoomMap& map, int x, int y, int thingType, int angle = 90, ThingOptions options = ThingOptions::AllSkills);

public:
    ThingsGenerator(const Preferences& preferences);
    void CreateThings(DoomMap& map, const std::vector<std::vector<TileType>>& subTiles);
    void Dispose();
};

class MapGenerator {
public:
    static const int TILE_SIZE = 64;
    static const int SUBTILE_DIVISIONS = 8;
    static const int SUBTILE_SIZE = TILE_SIZE / SUBTILE_DIVISIONS;

private:
    static const Point VERTEX_POSITION_MULTIPLIER;

    // SubTiles is indexed [x][y] and sized [width*8][height*8], so the outer
    // dimension is the X (width) axis and the inner dimension is the Y (height)
    // axis. This matches the C# original where MapSubWidth = SubTiles.GetLength(0)
    // and MapSubHeight = SubTiles.GetLength(1). (These were previously swapped,
    // which caused out-of-bounds indexing for non-square images.)
    int MapSubWidth() const { return SubTiles.size(); }
    int MapSubHeight() const { return SubTiles.empty() ? 0 : SubTiles[0].size(); }

    PreferencesTheme Theme;
    std::vector<std::string> ThemeTextures;
    std::vector<std::vector<TileType>> SubTiles;
    std::vector<std::vector<int>> Sectors;
    std::vector<SectorInfo> SectorsInfo;
    const Preferences& PreferencesRef;

    void CreateTheme(unsigned char* data, int width, int height);
    static TileType GetTileTypeFromPixel(const ::Color& pixel);
    void CreateArrays(unsigned char* data, int width, int height);
    void CreateLines(DoomMap& map);
    static Point GetTileSideOffset(TileSide side);
    int AddLine(DoomMap& map, Point position, TileSide neighborDirection, bool vertical, int sector, int neighborSector);
    static Sidedef CreateTwoSidedSidedef(int sectorID, const SectorInfo& sector, const SectorInfo& neighborSector);
    int GetSector(Point position) const;
    int GetSector(int x, int y) const;
    bool IsSubPointOnMap(Point position) const;
    void CreateSectors(DoomMap& map);

public:
    MapGenerator(const Preferences& preferences);
    DoomMap* Generate(const std::string& name, const std::string& bitmapFile);
    void Dispose();
};
