LOCAL_PATH := $(call my-dir)

# --- 1. Define OpenAL as a prebuilt module ---
include $(CLEAR_VARS)

LOCAL_MODULE := fluidsynth
# Point this to where CMake outputs the .so file
# $(if...) helps handle multiple ABIs automatically
LOCAL_SRC_FILES := build/intermediates/merged_native_libs/release/mergeReleaseNativeLibs/out/lib/$(TARGET_ARCH_ABI)/libfluidsynth.so

include $(PREBUILT_SHARED_LIBRARY)

