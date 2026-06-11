#pragma once

// Lightweight logging shim.
//
// On Android we route everything through __android_log_print with the "png2wad"
// tag (view it with: adb logcat -s png2wad). When the same sources are compiled
// for a host CLI (no __ANDROID__), we fall back to fprintf(stderr, ...) so the
// diagnostic harness prints to the console.

#include <cstdio>

#ifdef __ANDROID__
#include <android/log.h>
#define LOG_TAG "png2wad"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#else
#define LOGI(...) do { fprintf(stderr, "[png2wad][I] "); fprintf(stderr, __VA_ARGS__); fprintf(stderr, "\n"); } while (0)
#define LOGW(...) do { fprintf(stderr, "[png2wad][W] "); fprintf(stderr, __VA_ARGS__); fprintf(stderr, "\n"); } while (0)
#define LOGE(...) do { fprintf(stderr, "[png2wad][E] "); fprintf(stderr, __VA_ARGS__); fprintf(stderr, "\n"); } while (0)
#endif
