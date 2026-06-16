
LOCAL_PATH := $(call my-dir)/../thirdparty/zlib

include $(CLEAR_VARS)

LOCAL_MODULE  := zlib_zmd

LOCAL_CFLAGS = -Wall

LOCAL_C_INCLUDES :=

LOCAL_SRC_FILES =  \
    adler32.c \
    compress.c \
    crc32.c \
    deflate.c \
    inflate.c \
    infback.c \
    inftrees.c \
    inffast.c \
    trees.c \
    uncompr.c \
    zutil.c \

LOCAL_CFLAGS += -fvisibility=hidden -fdata-sections -ffunction-sections

include $(BUILD_STATIC_LIBRARY)








