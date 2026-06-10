LOCAL_SDL2_PATH := $(call my-dir)

#include $(LOCAL_SDL2_PATH)/SDL2/Android.mk
include $(LOCAL_SDL2_PATH)/SDL2-2.0.12/Android.mk
include $(LOCAL_SDL2_PATH)/SDL2_mixer/Android.mk
include $(LOCAL_SDL2_PATH)/SDL2_net/Android.mk
include $(LOCAL_SDL2_PATH)/SDL2_image/Android.mk
include $(LOCAL_SDL2_PATH)/fluidsynth-lite/src/Android.mk

ifeq ($(SDL3_ENABLED),1)
include $(LOCAL_SDL2_PATH)/SDL-3.x/Android.mk
endif

SDL_INCLUDE_PATHS = $(LOCAL_SDL2_PATH) $(LOCAL_SDL2_PATH)/SDL2-2.0.12/include  $(LOCAL_SDL2_PATH)/SDL2_mixer  $(LOCAL_SDL2_PATH)/SDL2_net  $(LOCAL_SDL2_PATH)/SDL2_image
SDL3_INCLUDE_PATHS = $(LOCAL_SDL2_PATH)/SDL-3.x/include  $(LOCAL_SDL2_PATH)/SDL2_mixer  $(LOCAL_SDL2_PATH)/SDL2_net  $(LOCAL_SDL2_PATH)/SDL2_image
