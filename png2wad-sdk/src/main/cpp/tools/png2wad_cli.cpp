// Host-side diagnostic harness for the png2wad C++ port.
//
// This is NOT part of the Android build. It links the exact same generation
// sources (Generator/DoomMap/WadFile/Config/INIFile/Toolbox + stb_image) so we
// can reproduce, log and structurally validate output.wad on a desktop without
// an Android device round-trip. ZDBSP is intentionally excluded: like the
// shipped path, we emit a clean nodeless PWAD and let the engine build nodes.
//
// Build (see build_cli.sh):
//   clang++ -std=c++17 -I.. tools/png2wad_cli.cpp \
//       ../Generator.cpp ../DoomMap.cpp ../WadFile.cpp \
//       ../Config.cpp ../INIFile.cpp ../Toolbox.cpp -o /tmp/png2wad_cli
//
// Usage:
//   png2wad_cli <Preferences.ini> <out.wad> <map1.png> [map2.png ...]

#define STB_IMAGE_IMPLEMENTATION
#include "stb_image.h"

#include "Config.h"
#include "Generator.h"
#include "WadFile.h"
#include "DoomMap.h"
#include "Log.h"

#include <cstdio>
#include <string>
#include <vector>

using namespace PNG2WAD::Config;

int main(int argc, char** argv) {
    if (argc < 4) {
        fprintf(stderr,
                "Usage: %s <Preferences.ini> <out.wad> <map1.png> [map2.png ...]\n",
                argv[0]);
        return 2;
    }

    std::string configPath = argv[1];
    std::string outWadPath = argv[2];
    std::vector<std::string> pngPaths;
    for (int i = 3; i < argc; i++) pngPaths.push_back(argv[i]);

    LOGI("png2wad_cli: %zu PNG(s), out='%s', config='%s'",
         pngPaths.size(), outWadPath.c_str(), configPath.c_str());

    Preferences config(configPath);
    MapGenerator generator(config);
    WadFile wad;

    int mapsAdded = 0;
    for (size_t i = 0; i < pngPaths.size(); i++) {
        int mapNumber = static_cast<int>(i) + 1;
        if ((config.Doom1Format && mapNumber > 9) ||
            (!config.Doom1Format && mapNumber > 99)) break;

        char mapName[16];
        if (config.Doom1Format)
            snprintf(mapName, sizeof(mapName), "E%dM%d", config.Episode, mapNumber);
        else
            snprintf(mapName, sizeof(mapName), "MAP%02d", mapNumber);

        LOGI("Generating map '%s' from '%s'", mapName, pngPaths[i].c_str());
        DoomMap* map = generator.Generate(mapName, pngPaths[i]);
        if (map) {
            map->AddToWad(wad);
            mapsAdded++;
            delete map;
        } else {
            LOGE("Generator returned null for '%s'", pngPaths[i].c_str());
        }
    }

    if (mapsAdded == 0) {
        LOGE("No maps generated; not writing WAD.");
        return 1;
    }

    wad.SaveToFile(outWadPath);
    LOGI("Done: %d map(s), %d lump(s) -> '%s'", mapsAdded, wad.GetLumpCount(), outWadPath.c_str());
    return 0;
}
