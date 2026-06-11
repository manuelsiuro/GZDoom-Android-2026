#include "Generator.h"
#include "stb_image.h"
#include "Log.h"
#include <stack>
#include <algorithm>

const Point MapGenerator::VERTEX_POSITION_MULTIPLIER = Point(SUBTILE_SIZE, -SUBTILE_SIZE);

SectorInfo::SectorInfo(TileType type, const PreferencesTheme& theme, const std::vector<std::string>& themeTextures) {
    Type = type;
    FloorHeight = theme.Height[static_cast<int>(ThemeSector::Default)][0];
    CeilingHeight = theme.Height[static_cast<int>(ThemeSector::Default)][1];
    LinedefSpecial = 0;
    SectorSpecial = 0;

    LightLevel = theme.LightLevel[static_cast<int>(ThemeSector::Default)];

    CeilingTexture = themeTextures[static_cast<int>(ThemeTexture::Ceiling)];
    FloorTexture = themeTextures[static_cast<int>(ThemeTexture::Floor)];
    WallTexture = Toolbox::RandomFromArray(theme.Textures[static_cast<int>(ThemeTexture::Wall)]);
    WallTextureUpper = "";
    WallTextureLower = "";

    switch (type) {
        case TileType::Door:
            CeilingHeight = FloorHeight;
            LinedefSpecial = 1; // DR Door Open Wait Close
            CeilingTexture = "CRATOP1";
            WallTexture = "DOORTRAK";
            WallTextureUpper = themeTextures[static_cast<int>(ThemeTexture::Door)];
            break;

        case TileType::DoorSide:
            WallTexture = themeTextures[static_cast<int>(ThemeTexture::DoorSide)];
            break;

        case TileType::Entrance:
            CeilingHeight = theme.Height[static_cast<int>(ThemeSector::Entrance)][1];
            FloorHeight = theme.Height[static_cast<int>(ThemeSector::Entrance)][0];
            LightLevel = theme.LightLevel[static_cast<int>(ThemeSector::Entrance)];
            SectorSpecial = theme.SectorSpecial[static_cast<int>(ThemeSector::Entrance)];
            FloorTexture = themeTextures[static_cast<int>(ThemeTexture::FloorEntrance)];
            break;

        case TileType::Exit:
            CeilingHeight = theme.Height[static_cast<int>(ThemeSector::Exit)][1];
            FloorHeight = theme.Height[static_cast<int>(ThemeSector::Exit)][0];
            LightLevel = theme.LightLevel[static_cast<int>(ThemeSector::Exit)];
            SectorSpecial = theme.SectorSpecial[static_cast<int>(ThemeSector::Exit)];
            LinedefSpecial = 52; // W1 Exit Level
            FloorTexture = themeTextures[static_cast<int>(ThemeTexture::FloorExit)];
            break;

        case TileType::RoomExterior:
            CeilingHeight = theme.Height[static_cast<int>(ThemeSector::Exterior)][1];
            FloorHeight = theme.Height[static_cast<int>(ThemeSector::Exterior)][0];
            LightLevel = theme.LightLevel[static_cast<int>(ThemeSector::Exterior)];
            SectorSpecial = theme.SectorSpecial[static_cast<int>(ThemeSector::Exterior)];
            CeilingTexture = "F_SKY1";
            if (!theme.Textures[static_cast<int>(ThemeTexture::FloorExterior)].empty())
                FloorTexture = themeTextures[static_cast<int>(ThemeTexture::FloorExterior)];
            if (!theme.Textures[static_cast<int>(ThemeTexture::WallExterior)].empty())
                WallTexture = Toolbox::RandomFromArray(theme.Textures[static_cast<int>(ThemeTexture::WallExterior)]);
            break;

        case TileType::RoomSpecialCeiling:
            CeilingHeight = theme.Height[static_cast<int>(ThemeSector::SpecialCeiling)][1];
            FloorHeight = theme.Height[static_cast<int>(ThemeSector::SpecialCeiling)][0];
            LightLevel = theme.LightLevel[static_cast<int>(ThemeSector::SpecialCeiling)];
            SectorSpecial = theme.SectorSpecial[static_cast<int>(ThemeSector::SpecialCeiling)];
            CeilingTexture = themeTextures[static_cast<int>(ThemeTexture::CeilingSpecial)];
            break;

        case TileType::RoomSpecialFloor:
            CeilingHeight = theme.Height[static_cast<int>(ThemeSector::SpecialFloor)][1];
            FloorHeight = theme.Height[static_cast<int>(ThemeSector::SpecialFloor)][0];
            LightLevel = theme.LightLevel[static_cast<int>(ThemeSector::SpecialFloor)];
            SectorSpecial = theme.SectorSpecial[static_cast<int>(ThemeSector::SpecialFloor)];
            FloorTexture = themeTextures[static_cast<int>(ThemeTexture::FloorSpecial)];
            break;

        case TileType::Secret:
            CeilingHeight = FloorHeight;
            LinedefSpecial = 31; // D1 Door Open Stay
            SectorSpecial = 9; // Secret room
            WallTexture = "DOORTRAK";
            break;
            
        default:
            break;
    }

    CeilingHeight = std::max(FloorHeight, CeilingHeight);

    if (WallTextureUpper.empty()) WallTextureUpper = WallTexture;
    if (WallTextureLower.empty()) WallTextureLower = WallTexture;
}

ThingsGenerator::ThingsGenerator(const Preferences& preferences) : PreferencesRef(preferences) {}

void ThingsGenerator::CreateThings(DoomMap& map, const std::vector<std::vector<TileType>>& subTiles) {
    int i, x, y;

    FreeTiles.clear();
    for (x = 0; x < subTiles.size(); x += MapGenerator::SUBTILE_DIVISIONS) {
        for (y = 0; y < subTiles[x].size(); y += MapGenerator::SUBTILE_DIVISIONS) {
            switch (subTiles[x][y]) {
                case TileType::Door:
                case TileType::DoorSide:
                case TileType::Entrance:
                case TileType::Exit:
                case TileType::Secret:
                case TileType::Wall:
                    continue;
                default:
                    break;
            }

            FreeTiles.push_back(Point(x / MapGenerator::SUBTILE_DIVISIONS, y / MapGenerator::SUBTILE_DIVISIONS));
        }
    }

    if (PreferencesRef.GenerateEntranceAndExit) { 
        AddPlayerStarts(map, subTiles);
        AddThings(map, 8, ThingsGeneratorFlags::None, {11});
    }

    float thingsCountMultiplier = FreeTiles.size() / 1000.0f;

    if (PreferencesRef.GenerateThings) {
        for (i = 0; i < Preferences::THINGS_CATEGORY_COUNT; i++) {
            AddThings(map, static_cast<ThingCategory>(i),
                static_cast<int>(PreferencesRef.ThingsCount[i][0] * thingsCountMultiplier),
                static_cast<int>(PreferencesRef.ThingsCount[i][1] * thingsCountMultiplier));
        }
    }
}

void ThingsGenerator::AddThings(DoomMap& map, ThingCategory thingCategory, int minCount, int maxCount, ThingsGeneratorFlags generationFlags) {
    int count = Toolbox::RandomInt(minCount, maxCount + 1);
    AddThings(map, count, generationFlags, PreferencesRef.ThingsTypes[static_cast<int>(thingCategory)]);
}

void ThingsGenerator::AddThings(DoomMap& map, int count, ThingsGeneratorFlags generationFlags, const std::vector<int>& thingTypes) {
    if (count < 1 || thingTypes.empty()) return;

    for (int i = 0; i < count; i++) {
        if (FreeTiles.empty()) return;

        ThingOptions options = ThingOptions::AllSkills;

        if (static_cast<int>(generationFlags) & static_cast<int>(ThingsGeneratorFlags::MoreThingsInEasyMode)) {
            if (Toolbox::RandomInt(4) == 0) options = static_cast<ThingOptions>(static_cast<int>(ThingOptions::Skill12) | static_cast<int>(ThingOptions::Skill3));
            else if (Toolbox::RandomInt(3) == 0) options = ThingOptions::Skill12;
        } else if (static_cast<int>(generationFlags) & static_cast<int>(ThingsGeneratorFlags::MoreThingsInHardMode)) {
            if (Toolbox::RandomInt(3) == 0) options = static_cast<ThingOptions>(static_cast<int>(ThingOptions::Skill3) | static_cast<int>(ThingOptions::Skill45));
            else if (Toolbox::RandomInt(2) == 0) options = ThingOptions::Skill45;
        }

        int thingType = Toolbox::RandomFromArray(thingTypes);
        Point pt = Toolbox::RandomFromList(FreeTiles);
        AddThing(map, pt.X, pt.Y, thingType, Toolbox::RandomInt(360), options);
        FreeTiles.erase(std::remove(FreeTiles.begin(), FreeTiles.end(), pt), FreeTiles.end());
    }
}

void ThingsGenerator::AddPlayerStarts(DoomMap& map, const std::vector<std::vector<TileType>>& subTiles) {
    int x, y;
    std::vector<Point> entrances;

    for (int player = 1; player <= 4; player++) {
        bool foundAnEntrance = false;

        for (x = 0; x < subTiles.size(); x += MapGenerator::SUBTILE_DIVISIONS) {
            for (y = 0; y < subTiles[x].size(); y += MapGenerator::SUBTILE_DIVISIONS) {
                if (!foundAnEntrance && (subTiles[x][y] == TileType::Entrance)) {
                    Point entranceTile(x / MapGenerator::SUBTILE_DIVISIONS, y / MapGenerator::SUBTILE_DIVISIONS);
                    if (std::find(entrances.begin(), entrances.end(), entranceTile) != entrances.end()) continue;
                    AddThing(map, entranceTile.X, entranceTile.Y, player);
                    entrances.push_back(entranceTile);
                    foundAnEntrance = true;
                }
            }
        }

        if (foundAnEntrance) continue;

        if (!FreeTiles.empty()) {
            Point tile;

            if (entrances.empty()) {
                tile = Toolbox::RandomFromList(FreeTiles);
            } else {
                tile = FreeTiles[0];
                float minDistance = tile.Distance(entrances[0]);
                for (const auto& t : FreeTiles) {
                    float dist = t.Distance(entrances[0]);
                    if (dist < minDistance) {
                        minDistance = dist;
                        tile = t;
                    }
                }
            }

            FreeTiles.erase(std::remove(FreeTiles.begin(), FreeTiles.end(), tile), FreeTiles.end());
            AddThing(map, tile.X, tile.Y, player);
            entrances.push_back(tile);
            foundAnEntrance = true;
        }

        if (foundAnEntrance) continue;

        AddThing(map, player - 1, 0, player);
        entrances.push_back(Point(player - 1, 0));
    }
}

void ThingsGenerator::AddThing(DoomMap& map, int x, int y, int thingType, int angle, ThingOptions options) {
    map.Things.push_back(
        Thing(
            static_cast<int>((x + 0.5f) * MapGenerator::TILE_SIZE),
            static_cast<int>((y + 0.5f) * -MapGenerator::TILE_SIZE),
            thingType, angle, options));
}

void ThingsGenerator::Dispose() {}

MapGenerator::MapGenerator(const Preferences& preferences) : PreferencesRef(preferences) {}

DoomMap* MapGenerator::Generate(const std::string& name, const std::string& bitmapFile) {
    int width, height, channels;
    unsigned char* data = stbi_load(bitmapFile.c_str(), &width, &height, &channels, 4);
    if (!data) {
        LOGE("stbi_load failed for '%s': %s", bitmapFile.c_str(), stbi_failure_reason());
        return nullptr;
    }
    LOGI("Loaded PNG '%s': %dx%d, %d channel(s) (forced RGBA)", bitmapFile.c_str(), width, height, channels);

    DoomMap* map = new DoomMap(name);

    CreateTheme(data, width, height);
    CreateArrays(data, width, height);
    LOGI("Sub-grid: %d x %d subtiles (MapSubWidth x MapSubHeight)", MapSubWidth(), MapSubHeight());

    CreateSectors(*map);
    CreateLines(*map);

    ThingsGenerator thingsGenerator(PreferencesRef);
    thingsGenerator.CreateThings(*map, SubTiles);
    thingsGenerator.Dispose();

    int minX, minY, maxX, maxY;
    map->GetMapBoundaries(minX, minY, maxX, maxY);
    LOGI("Map '%s': sectors=%zu vertices=%zu linedefs=%zu sidedefs=%zu things=%zu bounds=[%d,%d..%d,%d]",
         name.c_str(), map->Sectors.size(), map->Vertices.size(), map->Linedefs.size(),
         map->Sidedefs.size(), map->Things.size(), minX, minY, maxX, maxY);

    if (map->Sectors.empty() || map->Linedefs.empty() || map->Vertices.empty()) {
        LOGW("Map '%s' has degenerate geometry (empty sectors/linedefs/vertices) - GZDoom may reject it.",
             name.c_str());
    }

    stbi_image_free(data);
    return map;
}

void MapGenerator::CreateTheme(unsigned char* data, int width, int height) {
    ::Color px(data[3], data[0], data[1], data[2]);
    PNG2WAD::Config::Color cfgPixel = {px.r, px.g, px.b, px.a};
    Theme = PreferencesRef.GetTheme(cfgPixel);
    LOGI("Theme selected from top-left pixel RGB=(%d,%d,%d)", px.r, px.g, px.b);

    data[0] = 255; data[1] = 255; data[2] = 255; data[3] = 255;

    ThemeTextures.resize(PreferencesTheme::THEME_TEXTURES_COUNT);
    for (int i = 0; i < PreferencesTheme::THEME_TEXTURES_COUNT; i++) {
        ThemeTextures[i] = Toolbox::RandomFromArray(Theme.Textures[i]);
    }
}

auto GetPixelColor = [](unsigned char* data, int width, int x, int y) {
    int idx = (y * width + x) * 4;
    return ::Color(data[idx+3], data[idx], data[idx+1], data[idx+2]);
};

TileType MapGenerator::GetTileTypeFromPixel(const ::Color& pixel) {
    if (pixel.IsSameRGB(::Color(255, 255, 255))) return TileType::Wall;
    if (pixel.IsSameRGB(::Color(0, 0, 255))) return TileType::RoomExterior;
    if (pixel.IsSameRGB(::Color(0, 128, 0))) return TileType::RoomSpecialCeiling;
    if (pixel.IsSameRGB(::Color(255, 0, 0))) return TileType::RoomSpecialFloor;
    if (pixel.IsSameRGB(::Color(128, 128, 0))) return TileType::Door;
    if (pixel.IsSameRGB(::Color(255, 0, 255))) return TileType::Secret;
    if (pixel.IsSameRGB(::Color(255, 255, 0))) return TileType::Entrance;
    if (pixel.IsSameRGB(::Color(0, 255, 0))) return TileType::Exit;
    return TileType::Room;
}

void MapGenerator::CreateArrays(unsigned char* data, int width, int height) {
    int x, y, sX, sY;
    TileType tileType, subTileType;

    SectorsInfo.clear();

    SubTiles.assign(width * SUBTILE_DIVISIONS, std::vector<TileType>(height * SUBTILE_DIVISIONS, TileType::Room));

    for (x = 0; x < width; x++) {
        for (y = 0; y < height; y++) {
            ::Color pixel = GetPixelColor(data, width, x, y);
            tileType = GetTileTypeFromPixel(pixel);

            if (!PreferencesRef.GenerateEntranceAndExit) {
                if (tileType == TileType::Entrance || tileType == TileType::Exit)
                    tileType = TileType::Room;
            }

            for (sX = 0; sX < SUBTILE_DIVISIONS; sX++) {
                for (sY = 0; sY < SUBTILE_DIVISIONS; sY++) {
                    subTileType = tileType;

                    if (tileType == TileType::Door) {
                        subTileType = TileType::DoorSide;

                        if (x > 0 && x < width - 1 &&
                            GetTileTypeFromPixel(GetPixelColor(data, width, x - 1, y)) != TileType::Room &&
                            GetTileTypeFromPixel(GetPixelColor(data, width, x + 1, y)) != TileType::Room) 
                        {
                            if (sY == 3 || sY == 4)
                                subTileType = TileType::Door;
                        } else {
                            if (sX == 3 || sX == 4)
                                subTileType = TileType::Door;
                        }
                    }

                    SubTiles[x * SUBTILE_DIVISIONS + sX][y * SUBTILE_DIVISIONS + sY] = subTileType;
                }
            }
        }
    }

    Sectors.assign(width * SUBTILE_DIVISIONS, std::vector<int>(height * SUBTILE_DIVISIONS, -2));
}

void MapGenerator::CreateLines(DoomMap& map) {
    int x, y;
    int subWidth = MapSubWidth();
    int subHeight = MapSubHeight();

    std::vector<bool> linesSet(subWidth * subHeight * 4, false);
    auto setLineSet = [&](int px, int py, int i, bool val) {
        linesSet[(px * subHeight + py) * 4 + i] = val;
    };
    auto getLineSet = [&](int px, int py, int i) {
        return linesSet[(px * subHeight + py) * 4 + i];
    };

    for (x = 0; x < subWidth; x++) {
        for (y = 0; y < subHeight; y++) {
            int sector = GetSector(x, y);
            if (sector < 0) continue;

            for (int i = 0; i < 4; i++) {
                if (getLineSet(x, y, i)) continue;

                Point neighborDirection = GetTileSideOffset(static_cast<TileSide>(i));
                int neighborSector = GetSector(x + neighborDirection.X, y + neighborDirection.Y);
                
                if (sector == neighborSector) continue;

                if (neighborSector >= 0 && (i == static_cast<int>(TileSide::South) || i == static_cast<int>(TileSide::East)))
                    continue;

                bool vertical = (neighborDirection.X != 0);

                int length = AddLine(map, Point(x, y), static_cast<TileSide>(i), vertical, sector, neighborSector);

                for (int j = 0; j < length; j++) {
                    Point segmentPosition = Point(x, y).Add(vertical ? Point(0, j) : Point(j, 0));
                    if (!IsSubPointOnMap(segmentPosition)) continue;
                    setLineSet(segmentPosition.X, segmentPosition.Y, i, true);
                }
            }
        }
    }
}

Point MapGenerator::GetTileSideOffset(TileSide side) {
    switch (side) {
        case TileSide::East: return Point(1, 0);
        case TileSide::South: return Point(0, 1);
        case TileSide::West: return Point(-1, 0);
        default: return Point(0, -1);
    }
}

int MapGenerator::AddLine(DoomMap& map, Point position, TileSide neighborDirection, bool vertical, int sector, int neighborSector) {
    bool flipVectors = false;
    Point vertexOffset = Point::Empty();
    Point neighborOffset = GetTileSideOffset(neighborDirection);
    Point neighborPosition;

    bool needsFlipping = SectorsInfo[sector].LinedefSpecial > 0;

    switch (neighborDirection) {
        case TileSide::West:
            flipVectors = true;
            break;
        case TileSide::East:
            vertexOffset = Point(1, 0);
            break;
        case TileSide::South:
            flipVectors = true;
            vertexOffset = Point(0, 1);
            break;
        default:
            break;
    }

    int v1 = map.AddVertex(position.Add(vertexOffset).Mult(VERTEX_POSITION_MULTIPLIER));
    int length = 0;

    Point direction = vertical ? Point(0, 1) : Point(1, 0);

    do {
        position = position.Add(direction);
        neighborPosition = position.Add(neighborOffset);
        length++;

        if (GetSector(position) != sector || GetSector(neighborPosition) != neighborSector) break;
    } while (true);

    int v2 = map.AddVertex(position.Add(vertexOffset).Mult(VERTEX_POSITION_MULTIPLIER));

    if (flipVectors) {
        std::swap(v1, v2);
    }

    if (neighborSector < 0) {
        map.Sidedefs.push_back(Sidedef(0, 0, "-", "-", SectorsInfo[sector].WallTexture, sector));
        map.Linedefs.push_back(Linedef(v1, v2, static_cast<LinedefFlags>(static_cast<int>(LinedefFlags::Impassible) | static_cast<int>(LinedefFlags::LowerUnpegged)), 0, 0, -1, map.Sidedefs.size() - 1));
    } else {
        int lineSpecial = std::max(SectorsInfo[sector].LinedefSpecial, SectorsInfo[neighborSector].LinedefSpecial);

        map.Sidedefs.push_back(CreateTwoSidedSidedef(neighborSector, SectorsInfo[neighborSector], SectorsInfo[sector]));
        map.Sidedefs.push_back(CreateTwoSidedSidedef(sector, SectorsInfo[sector], SectorsInfo[neighborSector]));

        if (needsFlipping) {
            map.Linedefs.push_back(Linedef(v2, v1, LinedefFlags::TwoSided, lineSpecial, 0, map.Sidedefs.size() - 1, map.Sidedefs.size() - 2));
        } else {
            map.Linedefs.push_back(Linedef(v1, v2, LinedefFlags::TwoSided, lineSpecial, 0, map.Sidedefs.size() - 2, map.Sidedefs.size() - 1));
        }
    }

    return length;
}

Sidedef MapGenerator::CreateTwoSidedSidedef(int sectorID, const SectorInfo& sector, const SectorInfo& neighborSector) {
    std::string lowerTexture, upperTexture;

    if (neighborSector.Type == TileType::Door) {
        upperTexture = (neighborSector.CeilingHeight < sector.CeilingHeight) ? neighborSector.WallTextureUpper : "-";
        lowerTexture = (neighborSector.FloorHeight > sector.FloorHeight) ? neighborSector.WallTextureLower : "-";
    } else {
        upperTexture = (neighborSector.CeilingHeight < sector.CeilingHeight) ? sector.WallTexture : "-";
        lowerTexture = (neighborSector.FloorHeight > sector.FloorHeight) ? sector.WallTexture : "-";
    }

    return Sidedef(0, 0, upperTexture, lowerTexture, "-", sectorID);
}

int MapGenerator::GetSector(Point position) const {
    return GetSector(position.X, position.Y);
}

int MapGenerator::GetSector(int x, int y) const {
    if (x < 0 || y < 0 || x >= MapSubWidth() || y >= MapSubHeight() || Sectors[x][y] < 0)
        return -1;
    return Sectors[x][y];
}

bool MapGenerator::IsSubPointOnMap(Point position) const {
    return !(position.X < 0 || position.Y < 0 || position.X >= MapSubWidth() || position.Y >= MapSubHeight());
}

void MapGenerator::CreateSectors(DoomMap& map) {
    int x, y;
    map.Sectors.clear();

    for (x = 0; x < MapSubWidth(); x++) {
        for (y = 0; y < MapSubHeight(); y++) {
            if (Sectors[x][y] != -2) continue;

            if (SubTiles[x][y] == TileType::Wall) {
                Sectors[x][y] = -1;
                continue;
            }

            std::stack<Point> pixels;
            pixels.push(Point(x, y));

            while (!pixels.empty()) {
                Point a = pixels.top();
                pixels.pop();

                // Bounds must include the map border (>= 0). The C# original used
                // "> 0", which never floods row/column 0 (and the last row/col);
                // for PNGs that lack a solid wall border (e.g. maps drawn in the
                // editor), every border subtile then stays unvisited and spawns its
                // own degenerate orphan sector (hundreds of them). Including the
                // border floods the whole region into one sector and walls are made
                // at the true map edge (out-of-bounds neighbor -> GetSector == -1).
                if (a.X >= 0 && a.X < MapSubWidth() && a.Y >= 0 && a.Y < MapSubHeight()) {
                    if (Sectors[a.X][a.Y] == -2 && SubTiles[a.X][a.Y] == SubTiles[x][y]) {
                        Sectors[a.X][a.Y] = SectorsInfo.size();
                        pixels.push(Point(a.X - 1, a.Y));
                        pixels.push(Point(a.X + 1, a.Y));
                        pixels.push(Point(a.X, a.Y - 1));
                        pixels.push(Point(a.X, a.Y + 1));
                    }
                }
            }

            SectorInfo sectorInfo(SubTiles[x][y], Theme, ThemeTextures);
            SectorsInfo.push_back(sectorInfo);

            map.Sectors.push_back(
                Sector(
                    sectorInfo.FloorHeight, sectorInfo.CeilingHeight,
                    sectorInfo.FloorTexture, sectorInfo.CeilingTexture,
                    sectorInfo.LightLevel, sectorInfo.SectorSpecial, 0));
        }
    }
}

void MapGenerator::Dispose() {}
