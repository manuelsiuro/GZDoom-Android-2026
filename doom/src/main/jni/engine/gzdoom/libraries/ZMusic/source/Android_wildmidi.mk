
LOCAL_PATH := $(call my-dir)/../thirdparty/wildmidi

include $(CLEAR_VARS)

LOCAL_MODULE    := wildmidi_zmd

LOCAL_CFLAGS := -Dstricmp=strcasecmp -Dstrnicmp=strncasecmp -Wall -Wextra -Wno-unused-parameter -fomit-frame-pointer -fsigned-char

LOCAL_C_INCLUDES := $(LOCAL_PATH)/wildmidi

LOCAL_SRC_FILES =  	\
		file_io.cpp \
    	gus_pat.cpp \
    	reverb.cpp \
    	wildmidi_lib.cpp \
    	wm_error.cpp

LOCAL_CFLAGS += -fvisibility=hidden -fdata-sections -ffunction-sections

include $(BUILD_STATIC_LIBRARY)








