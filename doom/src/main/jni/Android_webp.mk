# libwebp (decode + mux, used by the engine's webptexture.cpp) built from the
# copy vendored inside the gzdoom repo. emileb's app project has its own
# makefile for this; upstream only ships CMake, so this one is ours.

LOCAL_PATH := $(TOP_DIR)/engine/gzdoom/libraries/webp

include $(CLEAR_VARS)

LOCAL_MODULE := webpmux

LOCAL_C_INCLUDES := $(LOCAL_PATH) $(LOCAL_PATH)/include $(LOCAL_PATH)/src

LOCAL_CFLAGS := -O2 -DWEBP_USE_THREAD

LOCAL_SRC_FILES := \
    $(subst $(LOCAL_PATH)/,, \
    $(wildcard $(LOCAL_PATH)/sharpyuv/*.c) \
    $(wildcard $(LOCAL_PATH)/src/dec/*.c) \
    $(wildcard $(LOCAL_PATH)/src/demux/*.c) \
    $(wildcard $(LOCAL_PATH)/src/dsp/*.c) \
    $(wildcard $(LOCAL_PATH)/src/enc/*.c) \
    $(wildcard $(LOCAL_PATH)/src/mux/*.c) \
    $(wildcard $(LOCAL_PATH)/src/utils/*.c))

include $(BUILD_STATIC_LIBRARY)
