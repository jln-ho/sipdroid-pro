LOCAL_PATH := $(call my-dir)
SPEEX	:= speex-1.2rc1
SILK     := silk

include $(CLEAR_VARS)
LOCAL_MODULE    := OSNetworkSystem
LOCAL_MODULE_FILENAME    := libOSNetworkSystem
LOCAL_SRC_FILES := OSNetworkSystem.cpp
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := speex_jni
LOCAL_MODULE_FILENAME    := libspeex_jni
LOCAL_SRC_FILES := speex_jni.cpp \
		$(SPEEX)/libspeex/speex.c \
		$(SPEEX)/libspeex/speex_callbacks.c \
		$(SPEEX)/libspeex/bits.c \
		$(SPEEX)/libspeex/modes.c \
		$(SPEEX)/libspeex/nb_celp.c \
		$(SPEEX)/libspeex/exc_20_32_table.c \
		$(SPEEX)/libspeex/exc_5_256_table.c \
		$(SPEEX)/libspeex/exc_5_64_table.c \
		$(SPEEX)/libspeex/exc_8_128_table.c \
		$(SPEEX)/libspeex/exc_10_32_table.c \
		$(SPEEX)/libspeex/exc_10_16_table.c \
		$(SPEEX)/libspeex/filters.c \
		$(SPEEX)/libspeex/quant_lsp.c \
		$(SPEEX)/libspeex/ltp.c \
		$(SPEEX)/libspeex/lpc.c \
		$(SPEEX)/libspeex/lsp.c \
		$(SPEEX)/libspeex/vbr.c \
		$(SPEEX)/libspeex/gain_table.c \
		$(SPEEX)/libspeex/gain_table_lbr.c \
		$(SPEEX)/libspeex/lsp_tables_nb.c \
		$(SPEEX)/libspeex/cb_search.c \
		$(SPEEX)/libspeex/vq.c \
		$(SPEEX)/libspeex/window.c \
		$(SPEEX)/libspeex/high_lsp_tables.c

LOCAL_C_INCLUDES += 
LOCAL_CFLAGS = -DFIXED_POINT -DEXPORT="" -UHAVE_CONFIG_H -I$(LOCAL_PATH)/$(SPEEX)/include
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
BV16     := bx16_fixedp
LOCAL_MODULE    := bv16_jni
LOCAL_MODULE_FILENAME    := libbv16_jni
LOCAL_SRC_FILES := bv16_jni.cpp \
	$(BV16)/bvcommon/a2lsp.c \
	$(BV16)/bvcommon/allpole.c \
	$(BV16)/bvcommon/allzero.c  \
	$(BV16)/bvcommon/autocor.c \
	$(BV16)/bvcommon/basop32.c \
	$(BV16)/bvcommon/cmtables.c \
	$(BV16)/bvcommon/levdur.c \
	$(BV16)/bvcommon/lsp2a.c \
	$(BV16)/bvcommon/mathtables.c \
	$(BV16)/bvcommon/mathutil.c \
	$(BV16)/bvcommon/memutil.c \
	$(BV16)/bvcommon/ptdec.c \
	$(BV16)/bvcommon/stblzlsp.c \
	$(BV16)/bvcommon/utility.c \
	$(BV16)/bvcommon/vqdecode.c \
	$(BV16)/bv16/bitpack.c \
	$(BV16)/bv16/bv.c \
	$(BV16)/bv16/coarptch.c \
	$(BV16)/bv16/decoder.c \
	$(BV16)/bv16/encoder.c \
	$(BV16)/bv16/excdec.c \
	$(BV16)/bv16/excquan.c \
	$(BV16)/bv16/fineptch.c \
	$(BV16)/bv16/g192.c \
	$(BV16)/bv16/gaindec.c \
	$(BV16)/bv16/gainquan.c \
	$(BV16)/bv16/levelest.c \
	$(BV16)/bv16/lspdec.c \
	$(BV16)/bv16/lspquan.c \
	$(BV16)/bv16/plc.c \
	$(BV16)/bv16/postfilt.c \
	$(BV16)/bv16/preproc.c \
	$(BV16)/bv16/ptquan.c \
	$(BV16)/bv16/tables.c 
	
LOCAL_ARM_MODE := arm
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog
LOCAL_C_INCLUDES += $(LOCAL_PATH)/$(BV16)/bvcommon $(LOCAL_PATH)/$(BV16)/bv16 $(LOCAL_PATH)/$(BV16)
#LOCAL_CFLAGS = -O3 -marm -march=armv6 -mtune=arm1136j-s -DWMOPS=0 -DG192BITSTREAM=0
#LOCAL_CFLAGS = -O3 -DWMOPS=0 -DG192BITSTREAM=0
include $(BUILD_SHARED_LIBRARY)


SILK     := silk
LOCAL_MODULE    := silkcommon
LOCAL_MODULE_FILENAME    := libsilkcommon
LOCAL_SRC_FILES :=  $(SILK)/src/SKP_Silk_A2NLSF.c \
	$(SILK)/src/SKP_Silk_CNG.c \
	$(SILK)/src/SKP_Silk_HP_variable_cutoff_FIX.c \
	$(SILK)/src/SKP_Silk_LBRR_reset.c \
	$(SILK)/src/SKP_Silk_LPC_inv_pred_gain.c \
	$(SILK)/src/SKP_Silk_LPC_stabilize.c \
	$(SILK)/src/SKP_Silk_LPC_synthesis_filter.c \
	$(SILK)/src/SKP_Silk_LPC_synthesis_order16.c \
	$(SILK)/src/SKP_Silk_LP_variable_cutoff.c \
	$(SILK)/src/SKP_Silk_LSF_cos_table.c \
	$(SILK)/src/SKP_Silk_LTP_analysis_filter_FIX.c \
	$(SILK)/src/SKP_Silk_LTP_scale_ctrl_FIX.c \
	$(SILK)/src/SKP_Silk_MA.c \
	$(SILK)/src/SKP_Silk_NLSF2A.c \
	$(SILK)/src/SKP_Silk_NLSF2A_stable.c \
	$(SILK)/src/SKP_Silk_NLSF_MSVQ_decode.c \
	$(SILK)/src/SKP_Silk_NLSF_MSVQ_encode_FIX.c \
	$(SILK)/src/SKP_Silk_NLSF_VQ_rate_distortion_FIX.c \
	$(SILK)/src/SKP_Silk_NLSF_VQ_sum_error_FIX.c \
	$(SILK)/src/SKP_Silk_NLSF_VQ_weights_laroia.c \
	$(SILK)/src/SKP_Silk_NLSF_stabilize.c \
	$(SILK)/src/SKP_Silk_NSQ.c \
	$(SILK)/src/SKP_Silk_NSQ_del_dec.c \
	$(SILK)/src/SKP_Silk_PLC.c \
	$(SILK)/src/SKP_Silk_VAD.c \
	$(SILK)/src/SKP_Silk_VQ_nearest_neighbor_FIX.c \
	$(SILK)/src/SKP_Silk_allpass_int.c \
	$(SILK)/src/SKP_Silk_ana_filt_bank_1.c \
	$(SILK)/src/SKP_Silk_apply_sine_window.c \
	$(SILK)/src/SKP_Silk_array_maxabs.c \
	$(SILK)/src/SKP_Silk_autocorr.c \
	$(SILK)/src/SKP_Silk_biquad.c \
	$(SILK)/src/SKP_Silk_biquad_alt.c \
	$(SILK)/src/SKP_Silk_burg_modified.c \
	$(SILK)/src/SKP_Silk_bwexpander.c \
	$(SILK)/src/SKP_Silk_bwexpander_32.c \
	$(SILK)/src/SKP_Silk_code_signs.c \
	$(SILK)/src/SKP_Silk_control_codec_FIX.c \
	$(SILK)/src/SKP_Silk_corrMatrix_FIX.c \
	$(SILK)/src/SKP_Silk_create_init_destroy.c \
	$(SILK)/src/SKP_Silk_dec_API.c \
	$(SILK)/src/SKP_Silk_decode_core.c \
	$(SILK)/src/SKP_Silk_decode_frame.c \
	$(SILK)/src/SKP_Silk_decode_indices_v4.c \
	$(SILK)/src/SKP_Silk_decode_parameters.c \
	$(SILK)/src/SKP_Silk_decode_parameters_v4.c \
	$(SILK)/src/SKP_Silk_decode_pulses.c \
	$(SILK)/src/SKP_Silk_decoder_set_fs.c \
	$(SILK)/src/SKP_Silk_detect_SWB_input.c \
	$(SILK)/src/SKP_Silk_enc_API.c \
	$(SILK)/src/SKP_Silk_encode_frame_FIX.c \
	$(SILK)/src/SKP_Silk_encode_parameters.c \
	$(SILK)/src/SKP_Silk_encode_parameters_v4.c \
	$(SILK)/src/SKP_Silk_encode_pulses.c \
	$(SILK)/src/SKP_Silk_find_LPC_FIX.c \
	$(SILK)/src/SKP_Silk_find_LTP_FIX.c \
	$(SILK)/src/SKP_Silk_find_pitch_lags_FIX.c \
	$(SILK)/src/SKP_Silk_find_pred_coefs_FIX.c \
	$(SILK)/src/SKP_Silk_gain_quant.c \
	$(SILK)/src/SKP_Silk_init_encoder_FIX.c \
	$(SILK)/src/SKP_Silk_inner_prod_aligned.c \
	$(SILK)/src/SKP_Silk_interpolate.c \
	$(SILK)/src/SKP_Silk_k2a.c \
	$(SILK)/src/SKP_Silk_k2a_Q16.c \
	$(SILK)/src/SKP_Silk_lin2log.c \
	$(SILK)/src/SKP_Silk_log2lin.c \
	$(SILK)/src/SKP_Silk_lowpass_int.c \
	$(SILK)/src/SKP_Silk_lowpass_short.c \
	$(SILK)/src/SKP_Silk_noise_shape_analysis_FIX.c \
	$(SILK)/src/SKP_Silk_pitch_analysis_core.c \
	$(SILK)/src/SKP_Silk_pitch_est_tables.c \
	$(SILK)/src/SKP_Silk_prefilter_FIX.c \
	$(SILK)/src/SKP_Silk_process_NLSFs_FIX.c \
	$(SILK)/src/SKP_Silk_process_gains_FIX.c \
	$(SILK)/src/SKP_Silk_pulses_to_bytes.c \
	$(SILK)/src/SKP_Silk_quant_LTP_gains_FIX.c \
	$(SILK)/src/SKP_Silk_range_coder.c \
	$(SILK)/src/SKP_Silk_regularize_correlations_FIX.c \
	$(SILK)/src/SKP_Silk_resample_1_2.c \
	$(SILK)/src/SKP_Silk_resample_1_2_coarse.c \
	$(SILK)/src/SKP_Silk_resample_1_2_coarsest.c \
	$(SILK)/src/SKP_Silk_resample_1_3.c \
	$(SILK)/src/SKP_Silk_resample_2_1_coarse.c \
	$(SILK)/src/SKP_Silk_resample_2_3.c \
	$(SILK)/src/SKP_Silk_resample_2_3_coarse.c \
	$(SILK)/src/SKP_Silk_resample_2_3_coarsest.c \
	$(SILK)/src/SKP_Silk_resample_2_3_rom.c \
	$(SILK)/src/SKP_Silk_resample_3_1.c \
	$(SILK)/src/SKP_Silk_resample_3_2.c \
	$(SILK)/src/SKP_Silk_resample_3_2_rom.c \
	$(SILK)/src/SKP_Silk_resample_3_4.c \
	$(SILK)/src/SKP_Silk_resample_4_3.c \
	$(SILK)/src/SKP_Silk_residual_energy16_FIX.c \
	$(SILK)/src/SKP_Silk_residual_energy_FIX.c \
	$(SILK)/src/SKP_Silk_scale_copy_vector16.c \
	$(SILK)/src/SKP_Silk_scale_vector.c \
	$(SILK)/src/SKP_Silk_schur.c \
	$(SILK)/src/SKP_Silk_schur64.c \
	$(SILK)/src/SKP_Silk_shell_coder.c \
	$(SILK)/src/SKP_Silk_sigm_Q15.c \
	$(SILK)/src/SKP_Silk_solve_LS_FIX.c \
	$(SILK)/src/SKP_Silk_sort.c \
	$(SILK)/src/SKP_Silk_sum_sqr_shift.c \
	$(SILK)/src/SKP_Silk_tables_LTP.c \
	$(SILK)/src/SKP_Silk_tables_NLSF_CB0_10.c \
	$(SILK)/src/SKP_Silk_tables_NLSF_CB0_16.c \
	$(SILK)/src/SKP_Silk_tables_NLSF_CB1_10.c \
	$(SILK)/src/SKP_Silk_tables_NLSF_CB1_16.c \
	$(SILK)/src/SKP_Silk_tables_gain.c \
	$(SILK)/src/SKP_Silk_tables_other.c \
	$(SILK)/src/SKP_Silk_tables_pitch_lag.c \
	$(SILK)/src/SKP_Silk_tables_pulses_per_block.c \
	$(SILK)/src/SKP_Silk_tables_sign.c \
	$(SILK)/src/SKP_Silk_tables_type_offset.c
	
LOCAL_ARM_MODE := arm
LOCAL_CFLAGS = -O3 
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog
LOCAL_C_INCLUDES += $(LOCAL_PATH)/$(SILK)/src $(LOCAL_PATH)/$(SILK)/interface
include $(BUILD_STATIC_LIBRARY)


include $(CLEAR_VARS)
LOCAL_MODULE    := silk8_jni
LOCAL_MODULE_FILENAME    := libsilk8_jni
LOCAL_SRC_FILES := silk8_jni.cpp 
LOCAL_C_INCLUDES += $(LOCAL_PATH)/$(SILK)/src $(LOCAL_PATH)/$(SILK)/interface
LOCAL_CFLAGS = -O3 
LOCAL_STATIC_LIBRARIES :=  silkcommon
LOCAL_ARM_MODE := arm
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := silk16_jni
LOCAL_MODULE_FILENAME    := libsilk16_jni
LOCAL_SRC_FILES := silk16_jni.cpp 
LOCAL_C_INCLUDES += $(LOCAL_PATH)/$(SILK)/src $(LOCAL_PATH)/$(SILK)/interface
LOCAL_CFLAGS = -O3 
LOCAL_STATIC_LIBRARIES :=  silkcommon
LOCAL_ARM_MODE := arm
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE    := silk24_jni
LOCAL_MODULE_FILENAME    := libsilk24_jni
LOCAL_C_INCLUDES += $(LOCAL_PATH)/$(SILK)/src $(LOCAL_PATH)/$(SILK)/interface
LOCAL_CFLAGS = -O3 
LOCAL_STATIC_LIBRARIES :=  silkcommon
LOCAL_ARM_MODE := arm
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
SPANDSP     := spandsp
LOCAL_MODULE    := g722_jni
LOCAL_MODULE_FILENAME    := libg722_jni
LOCAL_SRC_FILES := g722_jni.cpp \
	$(SPANDSP)/g722.c \
	$(SPANDSP)/vector_int.c
LOCAL_ARM_MODE := arm
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog
LOCAL_C_INCLUDES += $(LOCAL_PATH)/$(SPANDSP)/spandsp $(LOCAL_PATH)/$(SPANDSP)
LOCAL_CFLAGS = -O3
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
SPANDSP     := spandsp
LOCAL_MODULE    := gsm_jni
LOCAL_MODULE_FILENAME    := libgsm_jni
LOCAL_SRC_FILES := gsm_jni.cpp \
	$(SPANDSP)/gsm0610_decode.c \
	$(SPANDSP)/gsm0610_encode.c \
	$(SPANDSP)/gsm0610_lpc.c \
	$(SPANDSP)/gsm0610_preprocess.c \
	$(SPANDSP)/gsm0610_rpe.c \
	$(SPANDSP)/gsm0610_short_term.c \
	$(SPANDSP)/gsm0610_long_term.c
LOCAL_ARM_MODE := arm
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog
LOCAL_C_INCLUDES += $(LOCAL_PATH)/$(SPANDSP)/spandsp $(LOCAL_PATH)/$(SPANDSP)
LOCAL_CFLAGS = -O3
include $(BUILD_SHARED_LIBRARY)

### Fraunhofer AAC Library ###
include $(CLEAR_VARS)

aacdec_sources := $(wildcard $(LOCAL_PATH)/aac/libAACdec/src/*.cpp)
aacdec_sources := $(aacdec_sources:$(LOCAL_PATH)/aac/libAACdec/src/%=%)

aacenc_sources := $(wildcard $(LOCAL_PATH)/aac/libAACenc/src/*.cpp)
aacenc_sources := $(aacenc_sources:$(LOCAL_PATH)/aac/libAACenc/src/%=%)

pcmutils_sources := $(wildcard $(LOCAL_PATH)/aac/libPCMutils/src/*.cpp)
pcmutils_sources := $(pcmutils_sources:$(LOCAL_PATH)/aac/libPCMutils/src/%=%)

fdk_sources := $(wildcard $(LOCAL_PATH)/aac/libFDK/src/*.cpp)
fdk_sources := $(fdk_sources:$(LOCAL_PATH)/aac/libFDK/src/%=%)

sys_sources := $(wildcard $(LOCAL_PATH)/aac/libSYS/src/*.cpp)
sys_sources := $(sys_sources:$(LOCAL_PATH)/aac/libSYS/src/%=%)

mpegtpdec_sources := $(wildcard $(LOCAL_PATH)/aac/libMpegTPDec/src/*.cpp)
mpegtpdec_sources := $(mpegtpdec_sources:$(LOCAL_PATH)/aac/libMpegTPDec/src/%=%)

mpegtpenc_sources := $(wildcard $(LOCAL_PATH)/aac/libMpegTPEnc/src/*.cpp)
mpegtpenc_sources := $(mpegtpenc_sources:$(LOCAL_PATH)/aac/libMpegTPEnc/src/%=%)

sbrdec_sources := $(wildcard $(LOCAL_PATH)/aac/libSBRdec/src/*.cpp)
sbrdec_sources := $(sbrdec_sources:$(LOCAL_PATH)/aac/libSBRdec/src/%=%)

sbrenc_sources := $(wildcard $(LOCAL_PATH)/aac/libSBRenc/src/*.cpp)
sbrenc_sources := $(sbrenc_sources:$(LOCAL_PATH)/aac/libSBRenc/src/%=%)

LOCAL_SRC_FILES := \
        $(aacdec_sources:%=aac/libAACdec/src/%) \
        $(aacenc_sources:%=aac/libAACenc/src/%) \
        $(pcmutils_sources:%=aac/libPCMutils/src/%) \
        $(fdk_sources:%=aac/libFDK/src/%) \
        $(sys_sources:%=aac/libSYS/src/%) \
        $(mpegtpdec_sources:%=aac/libMpegTPDec/src/%) \
        $(mpegtpenc_sources:%=aac/libMpegTPEnc/src/%) \
        $(sbrdec_sources:%=aac/libSBRdec/src/%) \
        $(sbrenc_sources:%=aac/libSBRenc/src/%) 

LOCAL_CFLAGS := -DANDROID
LOCAL_CFLAGS += -Wno-sequence-point -Wno-extra

LOCAL_C_INCLUDES := \
        $(LOCAL_PATH)/aac/libAACdec/include \
        $(LOCAL_PATH)/aac/libAACenc/include \
        $(LOCAL_PATH)/aac/libPCMutils/include \
        $(LOCAL_PATH)/aac/libFDK/include \
        $(LOCAL_PATH)/aac/libSYS/include \
        $(LOCAL_PATH)/aac/libMpegTPDec/include \
        $(LOCAL_PATH)/aac/libMpegTPEnc/include \
        $(LOCAL_PATH)/aac/libSBRdec/include \
        $(LOCAL_PATH)/aac/libSBRenc/include 

LOCAL_MODULE := libFraunhoferAAC
include $(BUILD_SHARED_LIBRARY)

### Fraunhofer AAC Library pre-compiled###
#include $(CLEAR_VARS)
#LOCAL_MODULE    := libFraunhoferAAC
#LOCAL_SRC_FILES  :=  $(LOCAL_PATH)/aac/libFraunhoferAAC.so
#LOCAL_EXPORT_C_INCLUDES := \
#        $(LOCAL_PATH)/aac/libAACdec/include \
#        $(LOCAL_PATH)/aac/libAACenc/include \
#        $(LOCAL_PATH)/aac/libPCMutils/include \
#        $(LOCAL_PATH)/aac/libFDK/include \
#        $(LOCAL_PATH)/aac/libSYS/include \
#        $(LOCAL_PATH)/aac/libMpegTPDec/include \
#        $(LOCAL_PATH)/aac/libMpegTPEnc/include \
#        $(LOCAL_PATH)/aac/libSBRdec/include \
#        $(LOCAL_PATH)/aac/libSBRenc/include 
#include $(PREBUILT_SHARED_LIBRARY)

## AAC JNI ###
include $(CLEAR_VARS)
LOCAL_SRC_FILES := aac_jni.cpp
LOCAL_C_INCLUDES := \
        $(LOCAL_PATH)/aac/libAACdec/include \
        $(LOCAL_PATH)/aac/libAACenc/include \
        $(LOCAL_PATH)/aac/libPCMutils/include \
        $(LOCAL_PATH)/aac/libFDK/include \
        $(LOCAL_PATH)/aac/libSYS/include \
        $(LOCAL_PATH)/aac/libMpegTPDec/include \
        $(LOCAL_PATH)/aac/libMpegTPEnc/include \
        $(LOCAL_PATH)/aac/libSBRdec/include \
        $(LOCAL_PATH)/aac/libSBRenc/include 

LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog
LOCAL_SHARED_LIBRARIES := libFraunhoferAAC
LOCAL_MODULE := aac_jni
include $(BUILD_SHARED_LIBRARY)


## FFMPEG pre-compiled. For initial compilation, run ffmpeg_android.sh##
include $(CLEAR_VARS)
LOCAL_MODULE    := ffmpeg
LOCAL_SRC_FILES  := $(LOCAL_PATH)/ffmpeg/lib/libffmpeg.so
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/ffmpeg/include/
include $(PREBUILT_SHARED_LIBRARY)

## AAC FILE JNI ###
include $(CLEAR_VARS)
LOCAL_SRC_FILES := aac_file_jni.cpp
LOCAL_C_INCLUDES := \
        $(LOCAL_PATH)/aac/libAACdec/include \
        $(LOCAL_PATH)/aac/libAACenc/include \
        $(LOCAL_PATH)/aac/libPCMutils/include \
        $(LOCAL_PATH)/aac/libFDK/include \
        $(LOCAL_PATH)/aac/libSYS/include \
        $(LOCAL_PATH)/aac/libMpegTPDec/include \
        $(LOCAL_PATH)/aac/libMpegTPEnc/include \
        $(LOCAL_PATH)/aac/libSBRdec/include \
        $(LOCAL_PATH)/aac/libSBRenc/include \
        $(LOCAL_PATH)/ffmpeg/include

LOCAL_CFLAGES := -D__STDC_CONSTANT_MACROS
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog 
LOCAL_SHARED_LIBRARIES := libFraunhoferAAC ffmpeg
LOCAL_MODULE := aac_file_jni
include $(BUILD_SHARED_LIBRARY)

#### Opus #### 
include $(CLEAR_VARS)
LOCAL_LDLIBS := -llog  

#################### COMPILE OPTIONS #######################

# Uncomment this for fixed-point build
#FIXED_POINT=1

# It is strongly recommended to uncomment one of these
#	VAR_ARRAYS: Use C99 variable-length arrays for stack allocation
# 	USE_ALLOCA: Use alloca() for stack allocation
# 	If none is defined, then the fallback is a non-threadsafe global array
LOCAL_CFLAGS := -DUSE_ALLOCA
#CFLAGS := -DVAR_ARRAYS $(CFLAGS)

# These options affect performance
#	HAVE_LRINTF: Use C99 intrinsics to speed up float-to-int conversion
#   inline: Don't use the 'inline' keyword (for ANSI C compilers)
#   restrict: Don't use the 'restrict' keyword (for pre-C99 compilers)
LOCAL_CFLAGS += -DHAVE_LRINTF
#CFLAGS := -Dinline= $(CFLAGS)
#LOCAL_CFLAGS := -Drestrict= $(CFLAGS)

OPUS_VERSION := "1.0.2"
LOCAL_CFLAGS += -DOPUS_VERSION='$(OPUS_VERSION)'
WARNINGS := -Wall -W -Wstrict-prototypes -Wextra -Wcast-align -Wnested-externs -Wshadow
LOCAL_CFLAGS += -O2 -g $(WARNINGS) -DOPUS_BUILD
ifdef FIXED_POINT
LOCAL_CFLAGS += -DFIXED_POINT=1 -DDISABLE_FLOAT_API
endif


SILK_SOURCES := \
$(LOCAL_PATH)/opus/silk/CNG.c \
$(LOCAL_PATH)/opus/silk/code_signs.c \
$(LOCAL_PATH)/opus/silk/init_decoder.c \
$(LOCAL_PATH)/opus/silk/decode_core.c \
$(LOCAL_PATH)/opus/silk/decode_frame.c \
$(LOCAL_PATH)/opus/silk/decode_parameters.c \
$(LOCAL_PATH)/opus/silk/decode_indices.c \
$(LOCAL_PATH)/opus/silk/decode_pulses.c \
$(LOCAL_PATH)/opus/silk/decoder_set_fs.c \
$(LOCAL_PATH)/opus/silk/dec_API.c \
$(LOCAL_PATH)/opus/silk/enc_API.c \
$(LOCAL_PATH)/opus/silk/encode_indices.c \
$(LOCAL_PATH)/opus/silk/encode_pulses.c \
$(LOCAL_PATH)/opus/silk/gain_quant.c \
$(LOCAL_PATH)/opus/silk/interpolate.c \
$(LOCAL_PATH)/opus/silk/LP_variable_cutoff.c \
$(LOCAL_PATH)/opus/silk/NLSF_decode.c \
$(LOCAL_PATH)/opus/silk/NSQ.c \
$(LOCAL_PATH)/opus/silk/NSQ_del_dec.c \
$(LOCAL_PATH)/opus/silk/PLC.c \
$(LOCAL_PATH)/opus/silk/shell_coder.c \
$(LOCAL_PATH)/opus/silk/tables_gain.c \
$(LOCAL_PATH)/opus/silk/tables_LTP.c \
$(LOCAL_PATH)/opus/silk/tables_NLSF_CB_NB_MB.c \
$(LOCAL_PATH)/opus/silk/tables_NLSF_CB_WB.c \
$(LOCAL_PATH)/opus/silk/tables_other.c \
$(LOCAL_PATH)/opus/silk/tables_pitch_lag.c \
$(LOCAL_PATH)/opus/silk/tables_pulses_per_block.c \
$(LOCAL_PATH)/opus/silk/VAD.c \
$(LOCAL_PATH)/opus/silk/control_audio_bandwidth.c \
$(LOCAL_PATH)/opus/silk/quant_LTP_gains.c \
$(LOCAL_PATH)/opus/silk/VQ_WMat_EC.c \
$(LOCAL_PATH)/opus/silk/HP_variable_cutoff.c \
$(LOCAL_PATH)/opus/silk/NLSF_encode.c \
$(LOCAL_PATH)/opus/silk/NLSF_VQ.c \
$(LOCAL_PATH)/opus/silk/NLSF_unpack.c \
$(LOCAL_PATH)/opus/silk/NLSF_del_dec_quant.c \
$(LOCAL_PATH)/opus/silk/process_NLSFs.c \
$(LOCAL_PATH)/opus/silk/stereo_LR_to_MS.c \
$(LOCAL_PATH)/opus/silk/stereo_MS_to_LR.c \
$(LOCAL_PATH)/opus/silk/check_control_input.c \
$(LOCAL_PATH)/opus/silk/control_SNR.c \
$(LOCAL_PATH)/opus/silk/init_encoder.c \
$(LOCAL_PATH)/opus/silk/control_codec.c \
$(LOCAL_PATH)/opus/silk/A2NLSF.c \
$(LOCAL_PATH)/opus/silk/ana_filt_bank_1.c \
$(LOCAL_PATH)/opus/silk/biquad_alt.c \
$(LOCAL_PATH)/opus/silk/bwexpander_32.c \
$(LOCAL_PATH)/opus/silk/bwexpander.c \
$(LOCAL_PATH)/opus/silk/debug.c \
$(LOCAL_PATH)/opus/silk/decode_pitch.c \
$(LOCAL_PATH)/opus/silk/inner_prod_aligned.c \
$(LOCAL_PATH)/opus/silk/lin2log.c \
$(LOCAL_PATH)/opus/silk/log2lin.c \
$(LOCAL_PATH)/opus/silk/LPC_analysis_filter.c \
$(LOCAL_PATH)/opus/silk/LPC_inv_pred_gain.c \
$(LOCAL_PATH)/opus/silk/table_LSF_cos.c \
$(LOCAL_PATH)/opus/silk/NLSF2A.c \
$(LOCAL_PATH)/opus/silk/NLSF_stabilize.c \
$(LOCAL_PATH)/opus/silk/NLSF_VQ_weights_laroia.c \
$(LOCAL_PATH)/opus/silk/pitch_est_tables.c \
$(LOCAL_PATH)/opus/silk/resampler.c \
$(LOCAL_PATH)/opus/silk/resampler_down2_3.c \
$(LOCAL_PATH)/opus/silk/resampler_down2.c \
$(LOCAL_PATH)/opus/silk/resampler_private_AR2.c \
$(LOCAL_PATH)/opus/silk/resampler_private_down_FIR.c \
$(LOCAL_PATH)/opus/silk/resampler_private_IIR_FIR.c \
$(LOCAL_PATH)/opus/silk/resampler_private_up2_HQ.c \
$(LOCAL_PATH)/opus/silk/resampler_rom.c \
$(LOCAL_PATH)/opus/silk/sigm_Q15.c \
$(LOCAL_PATH)/opus/silk/sort.c \
$(LOCAL_PATH)/opus/silk/sum_sqr_shift.c \
$(LOCAL_PATH)/opus/silk/stereo_decode_pred.c \
$(LOCAL_PATH)/opus/silk/stereo_encode_pred.c \
$(LOCAL_PATH)/opus/silk/stereo_find_predictor.c \
$(LOCAL_PATH)/opus/silk/stereo_quant_pred.c


SILK_SOURCES_FIXED = \
$(LOCAL_PATH)/opus/silk/fixed/LTP_analysis_filter_FIX.c \
$(LOCAL_PATH)/opus/silk/fixed/LTP_scale_ctrl_FIX.c \
$(LOCAL_PATH)/opus/silk/fixed/corrMatrix_FIX.c \
$(LOCAL_PATH)/opus/silk/fixed/encode_frame_FIX.c \
$(LOCAL_PATH)/opus/silk/fixed/find_LPC_FIX.c \
$(LOCAL_PATH)/opus/silk/fixed/find_LTP_FIX.c \
$(LOCAL_PATH)/opus/silk/fixed/find_pitch_lags_FIX.c \
$(LOCAL_PATH)/opus/silk/fixed/find_pred_coefs_FIX.c \
$(LOCAL_PATH)/opus/silk/fixed/noise_shape_analysis_FIX.c \
$(LOCAL_PATH)/opus/silk/fixed/prefilter_FIX.c \
$(LOCAL_PATH)/opus/silk/fixed/process_gains_FIX.c \
$(LOCAL_PATH)/opus/silk/fixed/regularize_correlations_FIX.c \
$(LOCAL_PATH)/opus/silk/fixed/residual_energy16_FIX.c \
$(LOCAL_PATH)/opus/silk/fixed/residual_energy_FIX.c \
$(LOCAL_PATH)/opus/silk/fixed/solve_LS_FIX.c \
$(LOCAL_PATH)/opus/silk/fixed/warped_autocorrelation_FIX.c \
$(LOCAL_PATH)/opus/silk/fixed/apply_sine_window_FIX.c \
$(LOCAL_PATH)/opus/silk/fixed/autocorr_FIX.c \
$(LOCAL_PATH)/opus/silk/fixed/burg_modified_FIX.c \
$(LOCAL_PATH)/opus/silk/fixed/k2a_FIX.c \
$(LOCAL_PATH)/opus/silk/fixed/k2a_Q16_FIX.c \
$(LOCAL_PATH)/opus/silk/fixed/pitch_analysis_core_FIX.c \
$(LOCAL_PATH)/opus/silk/fixed/vector_ops_FIX.c \
$(LOCAL_PATH)/opus/silk/fixed/schur64_FIX.c \
$(LOCAL_PATH)/opus/silk/fixed/schur_FIX.c

SILK_SOURCES_FLOAT = \
$(LOCAL_PATH)/opus/silk/float/apply_sine_window_FLP.c \
$(LOCAL_PATH)/opus/silk/float/corrMatrix_FLP.c \
$(LOCAL_PATH)/opus/silk/float/encode_frame_FLP.c \
$(LOCAL_PATH)/opus/silk/float/find_LPC_FLP.c \
$(LOCAL_PATH)/opus/silk/float/find_LTP_FLP.c \
$(LOCAL_PATH)/opus/silk/float/find_pitch_lags_FLP.c \
$(LOCAL_PATH)/opus/silk/float/find_pred_coefs_FLP.c \
$(LOCAL_PATH)/opus/silk/float/LPC_analysis_filter_FLP.c \
$(LOCAL_PATH)/opus/silk/float/LTP_analysis_filter_FLP.c \
$(LOCAL_PATH)/opus/silk/float/LTP_scale_ctrl_FLP.c \
$(LOCAL_PATH)/opus/silk/float/noise_shape_analysis_FLP.c \
$(LOCAL_PATH)/opus/silk/float/prefilter_FLP.c \
$(LOCAL_PATH)/opus/silk/float/process_gains_FLP.c \
$(LOCAL_PATH)/opus/silk/float/regularize_correlations_FLP.c \
$(LOCAL_PATH)/opus/silk/float/residual_energy_FLP.c \
$(LOCAL_PATH)/opus/silk/float/solve_LS_FLP.c \
$(LOCAL_PATH)/opus/silk/float/warped_autocorrelation_FLP.c \
$(LOCAL_PATH)/opus/silk/float/wrappers_FLP.c \
$(LOCAL_PATH)/opus/silk/float/autocorrelation_FLP.c \
$(LOCAL_PATH)/opus/silk/float/burg_modified_FLP.c \
$(LOCAL_PATH)/opus/silk/float/bwexpander_FLP.c \
$(LOCAL_PATH)/opus/silk/float/energy_FLP.c \
$(LOCAL_PATH)/opus/silk/float/inner_product_FLP.c \
$(LOCAL_PATH)/opus/silk/float/k2a_FLP.c \
$(LOCAL_PATH)/opus/silk/float/levinsondurbin_FLP.c \
$(LOCAL_PATH)/opus/silk/float/LPC_inv_pred_gain_FLP.c \
$(LOCAL_PATH)/opus/silk/float/pitch_analysis_core_FLP.c \
$(LOCAL_PATH)/opus/silk/float/scale_copy_vector_FLP.c \
$(LOCAL_PATH)/opus/silk/float/scale_vector_FLP.c \
$(LOCAL_PATH)/opus/silk/float/schur_FLP.c \
$(LOCAL_PATH)/opus/silk/float/sort_FLP.c

CELT_SOURCES = $(LOCAL_PATH)/opus/celt/bands.c \
$(LOCAL_PATH)/opus/celt/celt.c \
$(LOCAL_PATH)/opus/celt/cwrs.c \
$(LOCAL_PATH)/opus/celt/entcode.c \
$(LOCAL_PATH)/opus/celt/entdec.c \
$(LOCAL_PATH)/opus/celt/entenc.c \
$(LOCAL_PATH)/opus/celt/kiss_fft.c \
$(LOCAL_PATH)/opus/celt/laplace.c \
$(LOCAL_PATH)/opus/celt/mathops.c \
$(LOCAL_PATH)/opus/celt/mdct.c \
$(LOCAL_PATH)/opus/celt/modes.c \
$(LOCAL_PATH)/opus/celt/pitch.c \
$(LOCAL_PATH)/opus/celt/celt_lpc.c \
$(LOCAL_PATH)/opus/celt/quant_bands.c \
$(LOCAL_PATH)/opus/celt/rate.c \
$(LOCAL_PATH)/opus/celt/vq.c

OPUS_SOURCES = $(LOCAL_PATH)/opus/src/opus.c \
$(LOCAL_PATH)/opus/src/opus_decoder.c \
$(LOCAL_PATH)/opus/src/opus_encoder.c \
$(LOCAL_PATH)/opus/src/opus_multistream.c \
$(LOCAL_PATH)/opus/src/repacketizer.c

ifdef FIXED_POINT
SILK_SOURCES += $(SILK_SOURCES_FIXED)
else
SILK_SOURCES += $(SILK_SOURCES_FLOAT)
endif

LOCAL_SRC_FILES := $(SILK_SOURCES) $(CELT_SOURCES) $(OPUS_SOURCES) opus_jni.cpp

LOCAL_C_INCLUDES += $(LOCAL_PATH)/opus/include/ \
	$(LOCAL_PATH)/opus/silk/ \
	$(LOCAL_PATH)/opus/silk/float/ \
	$(LOCAL_PATH)/opus/silk/fixed/ \
	$(LOCAL_PATH)/opus/celt/ \
	$(LOCAL_PATH)/opus/src/
	
LOCAL_MODULE:= opus_jni
LOCAL_MODULE_FILENAME := libopus_jni

include $(BUILD_SHARED_LIBRARY)

### mp3 ###
include $(CLEAR_VARS)
MPG123 := mpg123
LOCAL_ARM_MODE  := arm
LOCAL_MODULE    := libmpg123
LOCAL_SRC_FILES := $(MPG123)/equalizer.c \
                   $(MPG123)/index.c \
                   $(MPG123)/layer2.c \
                   $(MPG123)/synth.c \
                   $(MPG123)/dct64.c \
                   $(MPG123)/format.c \
                   $(MPG123)/layer3.c \
                   $(MPG123)/ntom.c \
                   $(MPG123)/parse.c \
                   $(MPG123)/readers.c \
                   $(MPG123)/frame.c \
                   $(MPG123)/layer1.c \
                   $(MPG123)/libmpg123.c \
                   $(MPG123)/optimize.c \
                   $(MPG123)/synth_arm.S \
                   $(MPG123)/tabinit.c \
                   $(MPG123)/id3.c
include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_ARM_MODE  := arm
LOCAL_SRC_FILES := mp3wrapper/mp3.c
LOCAL_MODULE := mp3
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog
LOCAL_STATIC_LIBRARIES := libmpg123
include $(BUILD_SHARED_LIBRARY)

### libresample / libsamplerate ###
include $(CLEAR_VARS)
RESAMPLE_DIR := $(LOCAL_PATH)/resample
LOCAL_MODULE    := libresample
LOCAL_SRC_FILES := $(RESAMPLE_DIR)/samplerate.c \
				   $(RESAMPLE_DIR)/src_linear.c \
				   $(RESAMPLE_DIR)/src_sinc.c \
				   $(RESAMPLE_DIR)/src_zoh.c \
				   
LOCAL_C_INCLUDES += $(RESAMPLE_DIR)
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_SRC_FILES := resample_jni.cpp
LOCAL_C_INCLUDES +=  $(LOCAL_PATH)/resample
LOCAL_MODULE := resample_jni
LOCAL_LDLIBS := -L$(SYSROOT)/usr/lib -llog
LOCAL_SHARED_LIBRARIES := libresample
include $(BUILD_SHARED_LIBRARY)
