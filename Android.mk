ifneq ($(BUILD_ID), GINGERBREAD)

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(call all-subdir-java-files) \
	src/com/quicinc/fmradio/IFMRadioServiceCallbacks.aidl \
	src/com/quicinc/fmradio/IFMRadioService.aidl \
	src/com/quicinc/fmradio/IFMTransmitterServiceCallbacks.aidl \
	src/com/quicinc/fmradio/IFMTransmitterService.aidl \

LOCAL_PACKAGE_NAME := FM
LOCAL_CERTIFICATE := platform

include $(BUILD_PACKAGE)

endif
