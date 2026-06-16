
LOCAL_PATH := $(call my-dir)/../thirdparty/timidity


include $(CLEAR_VARS)

LOCAL_MODULE    := timidity_zmd

LOCAL_CFLAGS := -fexceptions -std=c++11 -Wno-unused-function -Wno-unused-variable -fsigned-char

LOCAL_C_INCLUDES := $(LOCAL_PATH)/timidity

LOCAL_SRC_FILES =  	\
			common.cpp \
        	instrum.cpp \
        	instrum_dls.cpp \
        	instrum_font.cpp \
        	instrum_sf2.cpp \
        	mix.cpp \
        	playmidi.cpp \
        	resample.cpp \
        	timidity.cpp \

LOCAL_CFLAGS += -fvisibility=hidden -fdata-sections -ffunction-sections

include $(BUILD_STATIC_LIBRARY)








