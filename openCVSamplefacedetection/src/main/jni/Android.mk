LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

#OPENCV_CAMERA_MODULES:=off
#OPENCV_INSTALL_MODULES:=off
#OPENCV_LIB_TYPE:=SHARED
include D:\OpenCV-2.4.9-android-sdk\sdk\native\jni\OpenCV.mk

LOCAL_MODULE    := example
LOCAL_CFLAGS    := -Werror
LOCAL_C_INCLUDES += $(LOCAL_PATH)
LOCAL_SRC_FILES := findEyeCenter.cpp helpers.cpp
LOCAL_LDLIBS     += -llog -ldl

include $(BUILD_SHARED_LIBRARY)
