
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := saffal

LOCAL_CFLAGS :=   -O2

LOCAL_C_INCLUDES := .

LOCAL_SRC_FILES =  FileSAF.cpp FileJNI.cpp FileCache.cpp Utils.cpp

LOCAL_LDLIBS :=  -ldl -llog

include $(BUILD_SHARED_LIBRARY)


