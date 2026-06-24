# Master makefile for the Freedoom-for-Android native stack, rebased onto
# emileb's maintained UZDoom 5.0 mobile port (github.com/emileb/gzdoom
# @ uz_5.0_pre, a GZDoom 4.15-derived engine) plus its OpenTouch support libraries.
#
# Layout expectations baked into the vendored makefiles:
#  - TOP_DIR is this directory; support libs are siblings (Clibs_OpenTouch,
#    MobileTouchControls, AudioLibs_OpenTouch, SDL2_OpenTouch, SAFFAL, jpeg8d).
#  - The engine lives one level down (engine/gzdoom) so that the glue sources
#    referenced as ../../../Clibs_OpenTouch/... from engine/gzdoom/src resolve
#    back to TOP_DIR.

TOP_DIR := $(call my-dir)
LOCAL_PATH := $(call my-dir)

SDL_INCLUDE_PATHS := $(TOP_DIR)/SDL2_OpenTouch/SDL2-2.0.12/include \
                     $(TOP_DIR)/SDL2_OpenTouch

# --- support / third-party static libs ---
include $(TOP_DIR)/Clibs_OpenTouch/Android.mk            # logwritter
include $(TOP_DIR)/Clibs_OpenTouch/libvpx/Android.mk     # vpx_player (prebuilt)
include $(TOP_DIR)/jpeg8d/Android.mk                     # libjpeg
include $(TOP_DIR)/Android_webp.mk                       # webpmux (from engine/gzdoom/libraries/webp)

# --- MobileTouchControls and its deps ---
include $(TOP_DIR)/MobileTouchControls/libpng/Android.mk
include $(TOP_DIR)/MobileTouchControls/libzip/Android.mk
include $(TOP_DIR)/MobileTouchControls/sigc++/Android.mk
include $(TOP_DIR)/MobileTouchControls/TinyXML/Android.mk
include $(TOP_DIR)/MobileTouchControls/Android_TouchControls.mk

# --- SAF file-access layer (shared lib, used by engine + touchcontrols + zmusic) ---
include $(TOP_DIR)/SAFFAL/saffal/src/main/jni/Android.mk

# --- SDL2 (emileb fork, OPENTOUCH_SDL_EXTRA) + SDL2_net ---
include $(TOP_DIR)/SDL2_OpenTouch/SDL2-2.0.12/Android.mk
include $(TOP_DIR)/SDL2_OpenTouch/SDL2_net/Android.mk

# --- audio: OpenAL (source build, OpenSL backend) + ZMusic codec deps ---
include $(TOP_DIR)/AudioLibs_OpenTouch/openal/Android.mk
include $(TOP_DIR)/AudioLibs_OpenTouch/android_external_flac/Android.mk
include $(TOP_DIR)/AudioLibs_OpenTouch/libsndfile-android/jni/Android.mk
include $(TOP_DIR)/AudioLibs_OpenTouch/libmpg123/Android.mk
include $(TOP_DIR)/AudioLibs_OpenTouch/fluidsynth-lite/src/Android.mk

# --- the engine: pulls in lzma, bzip2, glslang, zwidget, ZMusic and module "gzdoom" ---
include $(TOP_DIR)/engine/gzdoom/mobile/Android.mk
