LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_ARM_MODE  := arm

LOCAL_SRC_FILES := mp3.c

LOCAL_MODULE := mp3

LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog

LOCAL_STATIC_LIBRARIES := libmpg123

include $(BUILD_SHARED_LIBRARY)
