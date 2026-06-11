#include <jni.h>
#include <string>
#include <vector>
#include <iostream>

#define STB_IMAGE_IMPLEMENTATION
#include "stb_image.h"

// Forward declaration of the internal C++ generate function we will write
bool GenerateWadInternal(const std::vector<std::string>& pngPaths, const std::string& outWadPath, const std::string& configPath);

#include "PNGToWad.h"
#include "Config.h"
#include "Generator.h"
#include "WadFile.h"
#include "Log.h"
#include <iostream>

extern "C" JNIEXPORT jboolean JNICALL
Java_com_doomandroid_png2wad_Png2WadConverter_generateWad(
        JNIEnv* env,
        jobject /* this */,
        jobjectArray pngPaths,
        jstring outWadPath,
        jstring configPath) {
        
    int pathCount = env->GetArrayLength(pngPaths);
    std::vector<std::string> nativePaths;
    for (int i = 0; i < pathCount; i++) {
        jstring jstr = (jstring)env->GetObjectArrayElement(pngPaths, i);
        const char* cstr = env->GetStringUTFChars(jstr, nullptr);
        nativePaths.push_back(std::string(cstr));
        env->ReleaseStringUTFChars(jstr, cstr);
        env->DeleteLocalRef(jstr);
    }
    
    const char* outPathCStr = env->GetStringUTFChars(outWadPath, nullptr);
    std::string nativeOutPath(outPathCStr);
    env->ReleaseStringUTFChars(outWadPath, outPathCStr);

    const char* configPathCStr = env->GetStringUTFChars(configPath, nullptr);
    std::string nativeConfigPath(configPathCStr);
    env->ReleaseStringUTFChars(configPath, configPathCStr);

    bool result = GenerateWadInternal(nativePaths, nativeOutPath, nativeConfigPath);
    return result ? JNI_TRUE : JNI_FALSE;
}

bool GenerateWadInternal(const std::vector<std::string>& pngPaths, const std::string& outWadPath, const std::string& configPath) {
    LOGI("GenerateWadInternal: %zu PNG(s), out='%s', config='%s'",
         pngPaths.size(), outWadPath.c_str(), configPath.c_str());

    if (pngPaths.empty()) {
        LOGE("No PNG paths supplied; aborting.");
        return false;
    }

    Preferences config(configPath);
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
