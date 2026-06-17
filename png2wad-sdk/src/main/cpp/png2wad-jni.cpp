#include <jni.h>
#include <string>
#include <vector>
#include <iostream>
#include <sstream>

#define STB_IMAGE_IMPLEMENTATION
#include "stb_image.h"

// Forward declaration of the internal C++ generate function we will write.
// perMapThings is parallel to pngPaths; each entry is a compact descriptor of
// hand-placed things "type,cellX,cellY,angle,flags;..." (empty = none).
// suppressAutoThings disables the procedural monster/item scatter so the user
// gets exactly what they placed (the grid's Start tile still yields a player
// start via GenerateEntranceAndExit).
bool GenerateWadInternal(const std::vector<std::string>& pngPaths,
                         const std::vector<std::string>& perMapThings,
                         bool suppressAutoThings,
                         const std::string& outWadPath,
                         const std::string& configPath);

#include "PNGToWad.h"
#include "Config.h"
#include "Generator.h"
#include "DoomMap.h"
#include "DoomDefines.h"
#include "WadFile.h"
#include "Log.h"
#include <iostream>

namespace {

std::vector<std::string> JStringArray(JNIEnv* env, jobjectArray arr) {
    std::vector<std::string> out;
    if (!arr) return out;
    int n = env->GetArrayLength(arr);
    out.reserve(n);
    for (int i = 0; i < n; i++) {
        jstring js = (jstring) env->GetObjectArrayElement(arr, i);
        if (js) {
            const char* c = env->GetStringUTFChars(js, nullptr);
            out.emplace_back(c ? c : "");
            if (c) env->ReleaseStringUTFChars(js, c);
            env->DeleteLocalRef(js);
        } else {
            out.emplace_back();
        }
    }
    return out;
}

std::string JString(JNIEnv* env, jstring s) {
    if (!s) return std::string();
    const char* c = env->GetStringUTFChars(s, nullptr);
    std::string out(c ? c : "");
    if (c) env->ReleaseStringUTFChars(s, c);
    return out;
}

// Parse "type,cellX,cellY,angle,flags;type,cellX,cellY,angle,flags;..."
struct PlacedThing { int type; int cellX; int cellY; int angle; int flags; };
std::vector<PlacedThing> ParsePlacedThings(const std::string& desc) {
    std::vector<PlacedThing> things;
    std::stringstream ss(desc);
    std::string record;
    while (std::getline(ss, record, ';')) {
        if (record.empty()) continue;
        std::stringstream rs(record);
        std::string field;
        int vals[5] = {0, 0, 0, 0, 7}; // angle default 0, flags default 7 (all skills)
        int idx = 0;
        while (idx < 5 && std::getline(rs, field, ',')) {
            try { vals[idx] = std::stoi(field); } catch (...) { vals[idx] = 0; }
            idx++;
        }
        if (idx < 3) continue; // need at least type,cellX,cellY
        things.push_back({vals[0], vals[1], vals[2], vals[3], vals[4]});
    }
    return things;
}

// Append hand-placed things to a generated map, converting editor grid cells to
// Doom world units with the exact transform the generator uses for player
// starts (Generator.cpp AddThing): worldX=(cell+0.5)*TILE, worldY negated.
void InjectPlacedThings(DoomMap* map, const std::vector<PlacedThing>& placed) {
    const int tile = MapGenerator::TILE_SIZE;
    for (const auto& t : placed) {
        int worldX = static_cast<int>((t.cellX + 0.5f) * tile);
        int worldY = static_cast<int>((t.cellY + 0.5f) * -tile);
        ThingOptions opts = static_cast<ThingOptions>(t.flags == 0 ? 7 : t.flags);
        map->Things.push_back(Thing(worldX, worldY, t.type, t.angle, opts));
    }
}

} // namespace

extern "C" JNIEXPORT jboolean JNICALL
Java_com_doomandroid_png2wad_Png2WadConverter_generateWad(
        JNIEnv* env,
        jobject /* this */,
        jobjectArray pngPaths,
        jstring outWadPath,
        jstring configPath) {

    std::vector<std::string> nativePaths = JStringArray(env, pngPaths);
    std::string nativeOutPath = JString(env, outWadPath);
    std::string nativeConfigPath = JString(env, configPath);

    bool result = GenerateWadInternal(nativePaths, {}, false, nativeOutPath, nativeConfigPath);
    return result ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_doomandroid_png2wad_Png2WadConverter_generateWadWithThings(
        JNIEnv* env,
        jobject /* this */,
        jobjectArray pngPaths,
        jobjectArray perMapThings,
        jboolean suppressAutoThings,
        jstring outWadPath,
        jstring configPath) {

    std::vector<std::string> nativePaths = JStringArray(env, pngPaths);
    std::vector<std::string> nativeThings = JStringArray(env, perMapThings);
    std::string nativeOutPath = JString(env, outWadPath);
    std::string nativeConfigPath = JString(env, configPath);

    bool result = GenerateWadInternal(nativePaths, nativeThings,
                                      suppressAutoThings == JNI_TRUE,
                                      nativeOutPath, nativeConfigPath);
    return result ? JNI_TRUE : JNI_FALSE;
}

bool GenerateWadInternal(const std::vector<std::string>& pngPaths,
                         const std::vector<std::string>& perMapThings,
                         bool suppressAutoThings,
                         const std::string& outWadPath,
                         const std::string& configPath) {
    LOGI("GenerateWadInternal: %zu PNG(s), %zu things-desc, suppressAuto=%d, out='%s', config='%s'",
         pngPaths.size(), perMapThings.size(), suppressAutoThings ? 1 : 0,
         outWadPath.c_str(), configPath.c_str());

    if (pngPaths.empty()) {
        LOGE("No PNG paths supplied; aborting.");
        return false;
    }

    Preferences config(configPath);
    // When the user controls things manually, stop the procedural scatter but
    // keep entrance/exit so the Start tile still spawns a player start.
    if (suppressAutoThings) {
        config.GenerateThings = false;
    }

    MapGenerator generator(config);
    WadFile wad;

    int mapsAdded = 0;
    for (size_t i = 0; i < pngPaths.size(); i++) {
        int mapNumber = i + 1;
        if ((config.Doom1Format && mapNumber > 9) || (!config.Doom1Format && mapNumber > 99)) break;

        char mapName[16];
        if (config.Doom1Format) {
            snprintf(mapName, sizeof(mapName), "E%dM%d", config.Episode, mapNumber);
        } else {
            snprintf(mapName, sizeof(mapName), "MAP%02d", mapNumber);
        }

        LOGI("Generating map '%s' from '%s'", mapName, pngPaths[i].c_str());
        DoomMap* map = generator.Generate(mapName, pngPaths[i]);
        if (map) {
            if (i < perMapThings.size() && !perMapThings[i].empty()) {
                std::vector<PlacedThing> placed = ParsePlacedThings(perMapThings[i]);
                InjectPlacedThings(map, placed);
                LOGI("Map '%s': injected %zu hand-placed thing(s)", mapName, placed.size());
            }
            map->AddToWad(wad);
            mapsAdded++;
            delete map;
        } else {
            LOGE("Generator returned null for '%s' (failed PNG load?)", pngPaths[i].c_str());
        }
    }

    if (mapsAdded == 0) {
        LOGE("No maps were generated; not writing WAD.");
        return false;
    }

    // Write a clean nodeless PWAD. GZDoom builds NODES/SEGS/SSECTORS/BLOCKMAP/
    // REJECT internally on load, so we do NOT run an embedded node builder here.
    // (The C# original also leaves node-building to an optional external tool.)
    wad.SaveToFile(outWadPath);
    LOGI("Wrote WAD with %d map(s) and %d lump(s) to '%s'",
         mapsAdded, wad.GetLumpCount(), outWadPath.c_str());

    return true;
}
