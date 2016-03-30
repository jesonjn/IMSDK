LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := IMSDK
LOCAL_SRC_FILES := IMSDK.cpp

include $(BUILD_SHARED_LIBRARY)
