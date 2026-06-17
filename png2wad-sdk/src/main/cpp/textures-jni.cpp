#include <jni.h>
#include <string>
#include <vector>
#include "TextureReader.h"
#include "Log.h"

using png2wad::TextureReader;
using png2wad::DecodedImage;

namespace {
std::string JStr(JNIEnv* env, jstring s) {
    if (!s) return std::string();
    const char* c = env->GetStringUTFChars(s, nullptr);
    std::string out(c ? c : "");
    if (c) env->ReleaseStringUTFChars(s, c);
    return out;
}

jobjectArray ToJStringArray(JNIEnv* env, const std::vector<std::string>& names) {
    jclass strCls = env->FindClass("java/lang/String");
    jobjectArray arr = env->NewObjectArray(static_cast<jsize>(names.size()), strCls, nullptr);
    for (size_t i = 0; i < names.size(); i++) {
        jstring s = env->NewStringUTF(names[i].c_str());
        env->SetObjectArrayElement(arr, static_cast<jsize>(i), s);
        env->DeleteLocalRef(s);
    }
    env->DeleteLocalRef(strCls);
    return arr;
}
} // namespace

extern "C" {

JNIEXPORT jlong JNICALL
Java_com_doomandroid_png2wad_WadTextures_nativeOpen(
        JNIEnv* env, jobject, jstring iwadPath, jobjectArray extraWadPaths) {
    std::string iwad = JStr(env, iwadPath);
    std::vector<std::string> extras;
    if (extraWadPaths) {
        jsize n = env->GetArrayLength(extraWadPaths);
        for (jsize i = 0; i < n; i++) {
            jstring js = (jstring) env->GetObjectArrayElement(extraWadPaths, i);
            extras.push_back(JStr(env, js));
            env->DeleteLocalRef(js);
        }
    }
    auto* reader = new TextureReader();
    if (!reader->Open(iwad, extras)) {
        delete reader;
        return 0;
    }
    return reinterpret_cast<jlong>(reader);
}

JNIEXPORT jobjectArray JNICALL
Java_com_doomandroid_png2wad_WadTextures_nativeListWalls(
        JNIEnv* env, jobject, jlong handle) {
    if (!handle) return ToJStringArray(env, {});
    auto* reader = reinterpret_cast<TextureReader*>(handle);
    return ToJStringArray(env, reader->WallNames());
}

JNIEXPORT jobjectArray JNICALL
Java_com_doomandroid_png2wad_WadTextures_nativeListFlats(
        JNIEnv* env, jobject, jlong handle) {
    if (!handle) return ToJStringArray(env, {});
    auto* reader = reinterpret_cast<TextureReader*>(handle);
    return ToJStringArray(env, reader->FlatNames());
}

// Returns int[] = { width, height, argb[0], argb[1], ... } or null.
JNIEXPORT jintArray JNICALL
Java_com_doomandroid_png2wad_WadTextures_nativeGetRgba(
        JNIEnv* env, jobject, jlong handle, jstring name) {
    if (!handle) return nullptr;
    auto* reader = reinterpret_cast<TextureReader*>(handle);
    DecodedImage img = reader->GetImage(JStr(env, name));
    if (!img.valid()) return nullptr;

    jsize total = 2 + static_cast<jsize>(img.argb.size());
    jintArray arr = env->NewIntArray(total);
    if (!arr) return nullptr;
    std::vector<jint> buf(total);
    buf[0] = img.width;
    buf[1] = img.height;
    for (size_t i = 0; i < img.argb.size(); i++) {
        buf[2 + i] = static_cast<jint>(img.argb[i]);
    }
    env->SetIntArrayRegion(arr, 0, total, buf.data());
    return arr;
}

JNIEXPORT void JNICALL
Java_com_doomandroid_png2wad_WadTextures_nativeClose(
        JNIEnv* env, jobject, jlong handle) {
    if (!handle) return;
    delete reinterpret_cast<TextureReader*>(handle);
}

} // extern "C"
