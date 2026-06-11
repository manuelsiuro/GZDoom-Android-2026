#include "PNGToWad.h"
#include "Config.h"
#include "Generator.h"
#include "DoomMap.h"
#include "WadFile.h"
#include <iostream>

void PNGToWad::Run(const std::vector<std::string>& args) {
    if (args.empty()) {
        std::cout << "Missing parameters or no valid PNG file in the parameters.\n";
        return;
    }

    std::string firstFile = args[0];
    std::string wadFile = firstFile.substr(0, firstFile.find_last_of('.')) + ".wad";

    Preferences config("Preferences.ini");
    MapGenerator generator(config);
    WadFile wad;

    for (size_t i = 0; i < args.size(); i++) {
        int mapNumber = i + 1;
        if ((config.Doom1Format && mapNumber > 9) || (!config.Doom1Format && mapNumber > 99)) break;

        char mapName[16];
        if (config.Doom1Format) {
            snprintf(mapName, sizeof(mapName), "E%dM%d", config.Episode, mapNumber);
        } else {
            snprintf(mapName, sizeof(mapName), "MAP%02d", mapNumber);
        }

        DoomMap* map = generator.Generate(mapName, args[i]);
        if (map) {
            map->AddToWad(wad);
            delete map;
        }
    }

    wad.SaveToFile(wadFile);
}
