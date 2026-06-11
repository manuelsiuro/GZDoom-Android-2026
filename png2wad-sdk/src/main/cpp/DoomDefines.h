#pragma once
#include <cstdint>

enum class LinedefFlags : uint16_t {
    Impassible = 1,
    BlocksMonsters = 2,
    TwoSided = 4,
    UpperUnpegged = 8,
    LowerUnpegged = 16,
    Secret = 32,
    BlocksSound = 64,
    NotOnMap = 128,
    AlreadyOnMap = 126
};
inline LinedefFlags operator|(LinedefFlags a, LinedefFlags b) {
    return static_cast<LinedefFlags>(static_cast<uint16_t>(a) | static_cast<uint16_t>(b));
}

enum class ThingOptions : uint16_t {
    Skill12 = 1,
    Skill3 = 2,
    Skill45 = 4,
    Deaf = 8,
    MultiplayerOnly = 16,
    AllSkills = 1 | 2 | 4
};
inline ThingOptions operator|(ThingOptions a, ThingOptions b) {
    return static_cast<ThingOptions>(static_cast<uint16_t>(a) | static_cast<uint16_t>(b));
}

enum class TileType {
    Wall, Room, RoomExterior, RoomSpecialCeiling, RoomSpecialFloor,
    Door, DoorSide, Secret, Entrance, Exit
};

enum class TileSide { North, East, South, West };

enum class ThingsGeneratorFlags {
    None = 0,
    MoreThingsInEasyMode = 1,
    MoreThingsInHardMode = 2
};
