
LOCAL_PATH := $(call my-dir)/../libraries/bzip2

include $(CLEAR_VARS)

LOCAL_MODULE    := bzip2_gl3

LOCAL_C_INCLUDES :=   $(LOCAL_PATH)/include/

LOCAL_SRC_FILES =  	\
	  blocksort.c \
    bzlib.c \
    compress.c \
    crctable.c \
    decompress.c \
    huffman.c \
    randtable.c \

LOCAL_CFLAGS += -fvisibility=hidden -fdata-sections -ffunction-sections

include $(BUILD_STATIC_LIBRARY)








