APP_PROJECT_PATH := $(call my-dir)

APP_MODULES  := OSNetworkSystem
#APP_MODULES	 += speex_jni
#APP_MODULES	 += bv16_jni

#APP_MODULES	 += silkcommon
#APP_MODULES	 += silk8_jni
#APP_MODULES	 += silk16_jni
#APP_MODULES	 += silk24_jni

APP_MODULES	 += g722_jni
#APP_MODULES	 += gsm_jni
APP_MODULES	 += libFraunhoferAAC
APP_MODULES  += aac_jni
APP_MODULES += aac_file_jni
APP_MODULES += opus_jni
APP_MODULES += resample_jni
APP_MODULES += mp3

APP_STL      := stlport_static
APP_ABI 	 := armeabi-v7a
APP_OPTIM    := release 
APP_CFLAGS   += -O3
