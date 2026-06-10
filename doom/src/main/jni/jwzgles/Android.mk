
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := jwzgles

LOCAL_CFLAGS :=   -O2  -DHAVE_ANDROID -DHAVE_JWZGLES

LOCAL_SRC_FILES =  jwzgles.c

include $(BUILD_STATIC_LIBRARY)





include $(CLEAR_VARS)

LOCAL_MODULE    := jwzgles_shared

LOCAL_CFLAGS :=   -O2  -DHAVE_ANDROID -DHAVE_JWZGLES

LOCAL_SRC_FILES =  jwzgles.c

LOCAL_LDLIBS :=  -ldl -llog -lGLESv1_CM -lEGL

include $(BUILD_SHARED_LIBRARY)







