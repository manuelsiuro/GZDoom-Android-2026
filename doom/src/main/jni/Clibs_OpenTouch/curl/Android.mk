LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := curl
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/lib/libcurl.a

include $(PREBUILT_STATIC_LIBRARY)
