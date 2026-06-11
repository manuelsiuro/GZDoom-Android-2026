#pragma once

#include "INIFile.h"
#include <string>
#include <vector>
#include <map>
#include <cstdint>
#include <optional>

namespace PNG2WAD {
namespace Config {

    struct Color {
        uint8_t r, g, b, a;
        int ToArgb() const { return (a << 24) | (r << 16) | (g << 8) | b; }
    };

    namespace ConfigUtils {
        std::optional<Color> GetColorFromString(const std::string& str);
        
        template<typename T>
        T Clamp(T val, T min, T max) {
            if (val < min) return min;
            if (val > max) return max;
            return val;
        }
    }

    enum class ThemeSector {
        Default = 0,
        DoorSide,
        Entrance,
        Exit,
        Exterior,
        SpecialCeiling,
        SpecialFloor,
        Count
    };

    inline std::string ToString(ThemeSector sec) {
        switch(sec) {
            case ThemeSector::Default: return "Default";
            case ThemeSector::DoorSide: return "DoorSide";
            case ThemeSector::Entrance: return "Entrance";
            case ThemeSector::Exit: return "Exit";
            case ThemeSector::Exterior: return "Exterior";
            case ThemeSector::SpecialCeiling: return "SpecialCeiling";
            case ThemeSector::SpecialFloor: return "SpecialFloor";
            default: return "Unknown";
        }
    }

    enum class ThemeTexture {
        Ceiling = 0,
        CeilingSpecial,
        Door,
        DoorSide,
        Floor,
        FloorEntrance,
        FloorExit,
        FloorExterior,
        FloorSpecial,
        Wall,
        WallExterior,
        Count
    };

    inline std::string ToString(ThemeTexture tex) {
        switch(tex) {
            case ThemeTexture::Ceiling: return "Ceiling";
            case ThemeTexture::CeilingSpecial: return "CeilingSpecial";
            case ThemeTexture::Door: return "Door";
            case ThemeTexture::DoorSide: return "DoorSide";
            case ThemeTexture::Floor: return "Floor";
            case ThemeTexture::FloorEntrance: return "FloorEntrance";
            case ThemeTexture::FloorExit: return "FloorExit";
            case ThemeTexture::FloorExterior: return "FloorExterior";
            case ThemeTexture::FloorSpecial: return "FloorSpecial";
            case ThemeTexture::Wall: return "Wall";
            case ThemeTexture::WallExterior: return "WallExterior";
            default: return "Unknown";
        }
    }

    enum class ThingCategory {
        AmmoLarge = 0,
        AmmoSmall,
        Armor,
        Health,
        MonstersEasy,
        MonstersAverage,
        MonstersHard,
        MonstersVeryHard,
        PowerUps,
        WeaponsHigh,
        WeaponsLow,
        Count
    };

    inline std::string ToString(ThingCategory cat) {
        switch(cat) {
            case ThingCategory::AmmoLarge: return "AmmoLarge";
            case ThingCategory::AmmoSmall: return "AmmoSmall";
            case ThingCategory::Armor: return "Armor";
            case ThingCategory::Health: return "Health";
            case ThingCategory::MonstersEasy: return "MonstersEasy";
            case ThingCategory::MonstersAverage: return "MonstersAverage";
            case ThingCategory::MonstersHard: return "MonstersHard";
            case ThingCategory::MonstersVeryHard: return "MonstersVeryHard";
            case ThingCategory::PowerUps: return "PowerUps";
            case ThingCategory::WeaponsHigh: return "WeaponsHigh";
            case ThingCategory::WeaponsLow: return "WeaponsLow";
            default: return "Unknown";
        }
    }

    struct PreferencesTheme {
        static const int THEME_SECTORS_COUNT;
        static const int THEME_TEXTURES_COUNT;

        std::vector<std::vector<int>> Height;
        std::vector<int> LightLevel;
        std::vector<int> SectorSpecial;
        std::vector<std::vector<std::string>> Textures;

        PreferencesTheme() = default;
        PreferencesTheme(INI::INIFile& ini, const std::string& section);
    };

    struct Preferences {
        static const int THINGS_CATEGORY_COUNT;
        static const int DEFAULT_THEME_COLOR;

        bool BuildNodes = false;
        bool Doom1Format = false;
        int Episode = 1;
        bool GenerateEntranceAndExit = true;
        bool GenerateThings = true;

        std::map<int, PreferencesTheme> Themes;
        std::vector<std::vector<int>> ThingsTypes;
        std::vector<std::vector<int>> ThingsCount;

        Preferences() = default;
        Preferences(const std::string& filePath);

        PreferencesTheme GetTheme(Color color) const;
    };

}
}