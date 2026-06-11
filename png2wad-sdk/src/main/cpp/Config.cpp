#include "Config.h"
#include <algorithm>

namespace PNG2WAD {
namespace Config {

    namespace ConfigUtils {
        std::optional<Color> GetColorFromString(const std::string& str) {
            Color c = {255, 255, 255, 255};
            if (str.length() >= 7 && str[0] == '#') {
                try {
                    c.r = static_cast<uint8_t>(std::stoul(str.substr(1, 2), nullptr, 16));
                    c.g = static_cast<uint8_t>(std::stoul(str.substr(3, 2), nullptr, 16));
                    c.b = static_cast<uint8_t>(std::stoul(str.substr(5, 2), nullptr, 16));
                    return c;
                } catch(...) {}
            }
            return std::nullopt;
        }
    }

    const int PreferencesTheme::THEME_SECTORS_COUNT = static_cast<int>(ThemeSector::Count);
    const int PreferencesTheme::THEME_TEXTURES_COUNT = static_cast<int>(ThemeTexture::Count);

    PreferencesTheme::PreferencesTheme(INI::INIFile& ini, const std::string& section) {
        Height.resize(THEME_SECTORS_COUNT);
        LightLevel.resize(THEME_SECTORS_COUNT);
        SectorSpecial.resize(THEME_SECTORS_COUNT);

        for (int i = 0; i < THEME_SECTORS_COUNT; i++) {
            ThemeSector ts = static_cast<ThemeSector>(i);
            Height[i] = ini.GetValueArray<int>(section, "Height." + ToString(ts));
            if (Height[i].empty()) {
                Height[i] = { 0, 64 };
            } else if (Height[i].size() == 1) {
                Height[i] = { 0, Height[i][0] };
            }
            Height[i] = { std::min(Height[i][0], Height[i][1]), std::max(Height[i][0], Height[i][1]) };

            LightLevel[i] = ini.GetValue<int>(section, "LightLevel." + ToString(ts), -1);
            LightLevel[i] = ConfigUtils::Clamp(LightLevel[i], 0, 255);

            SectorSpecial[i] = ini.GetValue<int>(section, "SectorSpecial." + ToString(ts), 0);
            SectorSpecial[i] = std::max(0, SectorSpecial[i]);
        }

        Textures.resize(THEME_TEXTURES_COUNT);
        for (int i = 0; i < THEME_TEXTURES_COUNT; i++) {
            ThemeTexture tt = static_cast<ThemeTexture>(i);
            Textures[i] = ini.GetValueArray<std::string>(section, "Textures." + ToString(tt));
        }
    }

    const int Preferences::THINGS_CATEGORY_COUNT = static_cast<int>(ThingCategory::Count);
    const int Preferences::DEFAULT_THEME_COLOR = Color{255, 255, 255, 255}.ToArgb(); // Color.White.ToArgb()

    Preferences::Preferences(const std::string& filePath) {
        INI::INIFile ini(filePath);

        BuildNodes = ini.GetValue<bool>("Options", "BuildNodes", false);
        Doom1Format = ini.GetValue<bool>("Options", "Doom1Format", false);
        Episode = std::max(1, std::min(9, ini.GetValue<int>("Options", "Episode", 1)));
        GenerateEntranceAndExit = ini.GetValue<bool>("Options", "GenerateEntranceAndExit", true);
        GenerateThings = ini.GetValue<bool>("Options", "GenerateThings", true);

        ThingsTypes.resize(THINGS_CATEGORY_COUNT);
        ThingsCount.resize(THINGS_CATEGORY_COUNT);
        for (int i = 0; i < THINGS_CATEGORY_COUNT; i++) {
            ThingCategory tc = static_cast<ThingCategory>(i);
            ThingsTypes[i] = ini.GetValueArray<int>("Things", "Types." + ToString(tc));
            ThingsCount[i] = ini.GetValueArray<int>("Things", "Count." + ToString(tc));
            
            if (ThingsCount[i].size() < 2) {
                ThingsCount[i].resize(2, 0);
            }
            ThingsCount[i] = { std::min(ThingsCount[i][0], ThingsCount[i][1]), std::max(ThingsCount[i][0], ThingsCount[i][1]) };
        }

        Themes[DEFAULT_THEME_COLOR] = PreferencesTheme(ini, "Theme.Default");

        std::vector<std::string> themeKeys = ini.GetAllKeysInSection("Themes");
        for (const std::string& theme : themeKeys) {
            std::string colorStr = ini.GetValue<std::string>("Themes", theme);
            std::optional<Color> c = ConfigUtils::GetColorFromString(colorStr);
            if (!c.has_value()) continue;
            
            int colorArgb = c.value().ToArgb();
            if (Themes.find(colorArgb) != Themes.end()) continue;

            Themes[colorArgb] = PreferencesTheme(ini, "Theme." + theme);
        }
    }

    PreferencesTheme Preferences::GetTheme(Color color) const {
        int colorInt = color.ToArgb();
        auto it = Themes.find(colorInt);
        if (it != Themes.end()) {
            return it->second;
        }
        return Themes.at(DEFAULT_THEME_COLOR);
    }

}
}