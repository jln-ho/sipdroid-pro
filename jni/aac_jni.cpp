/*
 * Copyright (C) 2009 The Sipdroid Open Source Project
 *
 * This file is part of Sipdroid (http://www.sipdroid.org)
 *
 * Sipdroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

/** AU header format from RFC 3640
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+- .. -+-+-+-+-+-+-+-+-+-+
 |AU-headers-length|AU-header|AU-header|      |AU-header|padding|
 |                 |   (1)   |   (2)   |      |   (n)   | bits  |
 +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+- .. -+-+-+-+-+-+-+-+-+-+
 *
 *
 *+---------------------------------------+
 |     AU-size     13 bit                |
 +---------------------------------------+
 |     AU-Index / AU-Index-delta   3bit  |
 +---------------------------------------+
 */

#include <jni.h>
#include <android/log.h>
#include <string.h>
#include <stdint.h>
#include <stddef.h>
#include <stdlib.h>
#include "aacenc_lib.h"
#include "aacdecoder_lib.h"
#include <pthread.h>

#define DEBUG_TAG_ENCODER "AAC_ENC"
#define DEBUG_TAG_DECODER "AAC_DEC"

pthread_mutex_t enc_lock;
pthread_mutex_t dec_lock;
bool mutexes_initialized = false;

#define	 RTP_HDR_SIZE	12
#define AU_HDR_SIZE 	4
#define TOTAL_HDR_SIZE 	RTP_HDR_SIZE + AU_HDR_SIZE

bool open_error = true;
int codec_samplerate = -1;
int codec_bitrate = -1;
int channels = 1;

/* ID of used Audio Object Type (see FDK_audio.h)
 possible profiles:
 AOT_AAC_LC (2) for standard AAC-LC
 AOT_SBR (5) for HE-AAC
 AOT_PS (29) for HE-AACv2
 AOT_ER_AAC_LD (23) for AAC-LD
 AOT_ER_AAC_ELD (39) for AAC-ELD (sbr must be enabled manually for this one)*/
int codec_aot = -1;

// flag for increased audio quality (also increases cpu load)
int afterburner = 1;

// flag for manually enabling SBR for aot = 39 (AAC-ELD)
bool codec_eld_sbr = false;

HANDLE_AACENCODER enc_handle;
AACENC_InfoStruct enc_info = { 0 };

AACENC_InArgs enc_in_args = { 0 };
AACENC_OutArgs enc_out_args = { 0 };

AACENC_ERROR enc_err;

// pointers to encoder's in/out buffers for buffer descriptors
void* enc_in_ptr = 0;
void* enc_out_ptr = 0;

// encoder buffer descriptors
AACENC_BufDesc enc_in_bufdesc = { 0 };
int enc_in_buf_identifier = IN_AUDIO_DATA;
int enc_in_buf_size;
int enc_in_buf_elem_size = 2;

AACENC_BufDesc enc_out_bufdesc = { 0 };
int enc_out_buf_identifier = OUT_BITSTREAM_DATA;
int enc_out_buf_size;
int enc_out_buf_elem_size = 1;

// AU header template for encoded frames
jbyte enc_au_header[AU_HDR_SIZE] = { 0, 16, 0, 0 };

HANDLE_AACDECODER dec_handle;
AAC_DECODER_ERROR dec_err;
UINT dec_out_buf_size;
UCHAR dec_au_header[AU_HDR_SIZE];

extern "C"
JNIEXPORT jint JNICALL Java_org_sipdroid_codecs_AAC_open (JNIEnv *env, jobject obj, jint brate, jint samplerate, jint aot, jbyteArray codec_config, jint codec_config_length, jboolean eld_sbr) {
	if(mutexes_initialized && !open_error && brate == codec_bitrate && samplerate == codec_samplerate && aot == codec_aot && ((codec_aot == 39 && codec_eld_sbr == eld_sbr) || codec_aot != 39)) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_ENCODER, "codec already opened. returning");
		return 0;
	}

	if(!mutexes_initialized) {
		pthread_mutex_init(&enc_lock,NULL);
		pthread_mutex_init(&dec_lock,NULL);
		mutexes_initialized = true;
	}

	codec_bitrate = brate;
	codec_samplerate = samplerate;
	codec_eld_sbr = eld_sbr;

	switch(aot) {
		case AOT_AAC_LC: codec_aot = AOT_AAC_LC; break;
		case AOT_SBR: codec_aot = AOT_SBR; break;
		case AOT_ER_AAC_LD: codec_aot = AOT_ER_AAC_LD; break;
		case AOT_ER_AAC_ELD: codec_aot = AOT_ER_AAC_ELD; break;
		default: __android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_ENCODER, "unsupported AOT: %d", aot);
		return -1;
	}
	// open encoder
	if (aacEncOpen(&enc_handle, 0, 1) != AACENC_OK) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_ENCODER, "unable to open encoder");
		return -1;
	}
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_ENCODER, "encoder opened");
	// set SBR separately for AAC-ELD
	if (codec_aot == 39 && codec_eld_sbr) {
		if (aacEncoder_SetParam(enc_handle, AACENC_SBR_MODE, 1) != AACENC_OK) {
			__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_ENCODER, "unable to set SBR for ELD", aot);
			return -1;
		}
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_ENCODER, "AOT is 39, SBR is enabled", aot);
	}
	// set AOT (HE-AACv1, HE-AACv2, AAC-LC, AAC-LD or AAC-ELD)
	if (aacEncoder_SetParam(enc_handle, AACENC_AOT, codec_aot) != AACENC_OK) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_ENCODER, "unable to set the AOT to %d", codec_aot);
		return -1;
	}
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_ENCODER, "AOT: %d", aot);
	// set the samplerate
	if (aacEncoder_SetParam(enc_handle, AACENC_SAMPLERATE, codec_samplerate) != AACENC_OK) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_ENCODER, "unable to set the samplerate to %d", codec_samplerate);
		return -1;
	}
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_ENCODER, "samplerate: %d", codec_samplerate);
	// set channel mode
	if (aacEncoder_SetParam(enc_handle, AACENC_CHANNELMODE, channels) != AACENC_OK) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_ENCODER, "unable to set the channelmode to %d", channels);
		return -1;
	}
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_ENCODER, "channelmode: %d", channels);
	// set bitrate
	if (aacEncoder_SetParam(enc_handle, AACENC_BITRATE, codec_bitrate) != AACENC_OK) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_ENCODER, "unable to set the bitrate to %d", codec_bitrate);
		return -1;
	}
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_ENCODER, "bitrate: %d", brate);
	// set transport type
	if (aacEncoder_SetParam(enc_handle, AACENC_TRANSMUX, TT_MP4_RAW) != AACENC_OK) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_ENCODER, "unable to set AACENC_TRANSMUX to raw");
		return -1;
	}
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_ENCODER, "transport type: raw access units");
	// enable afterburner for better sound quality
	if (aacEncoder_SetParam(enc_handle, AACENC_AFTERBURNER, afterburner) != AACENC_OK) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_ENCODER, "unable to enable afterburner");
		return -1;
	}
	if(afterburner) __android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_ENCODER, "afterburner enabled");
	// enable encoder
	if (aacEncEncode(enc_handle, NULL, NULL, NULL, NULL) != AACENC_OK) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_ENCODER, "unable to initialize the encoder");
		return -1;
	}
	// get encoder info
	if (aacEncInfo(enc_handle, &enc_info) != AACENC_OK) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_ENCODER, "unable to get encoder info");
		return -1;
	}
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_ENCODER, "frameLength: %d", enc_info.frameLength);
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_ENCODER, "encoder enabled successfully !");

	// prepare encoder buffers
	enc_in_buf_size = sizeof(uint16_t) * enc_info.frameLength;
	enc_out_buf_size = enc_in_buf_size;

	enc_in_bufdesc.numBufs = 1;
	enc_in_bufdesc.bufs = &enc_in_ptr;
	enc_in_bufdesc.bufferIdentifiers = &enc_in_buf_identifier;
	enc_in_bufdesc.bufSizes = &enc_in_buf_size;
	enc_in_bufdesc.bufElSizes = &enc_in_buf_elem_size;

	enc_out_bufdesc.numBufs = 1;
	enc_out_bufdesc.bufs = &enc_out_ptr;
	enc_out_bufdesc.bufferIdentifiers = &enc_out_buf_identifier;
	enc_out_bufdesc.bufSizes = &enc_out_buf_size;
	enc_out_bufdesc.bufElSizes = &enc_out_buf_elem_size;

	enc_in_args.numInSamples = enc_info.frameLength;

	// open decoder
	dec_handle = aacDecoder_Open(TT_MP4_RAW, 1);
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "decoder opened");

	// ASC data for decoder
	UINT asc_buf_size = codec_config_length;
	UCHAR asc_buf[asc_buf_size];
	UCHAR* asc_buf_pt = asc_buf;
	env->GetByteArrayRegion(codec_config, 0 , asc_buf_size, (jbyte*) asc_buf);
	dec_err = aacDecoder_ConfigRaw(dec_handle, &asc_buf_pt, &asc_buf_size);
	if(dec_err != AAC_DEC_OK) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "error writing ASC. AAC_DECODER_ERROR %x", dec_err);
		return -1;
	}
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "decoder sampleRate: %d", aacDecoder_GetStreamInfo(dec_handle)->aacSampleRate);
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "decoder extSamplingRate: %d", aacDecoder_GetStreamInfo(dec_handle)->extSamplingRate);
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "decoder samples per frame: %d", aacDecoder_GetStreamInfo(dec_handle)->aacSamplesPerFrame);
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "decoder framesize: %d", aacDecoder_GetStreamInfo(dec_handle)->frameSize);
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "decoder aot: %d", aacDecoder_GetStreamInfo(dec_handle)->aot);
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "decoder extAot: %d", aacDecoder_GetStreamInfo(dec_handle)->extAot);
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "decoder channelConfig: %d", aacDecoder_GetStreamInfo(dec_handle)->channelConfig);
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "decoder numchannels: %d", aacDecoder_GetStreamInfo(dec_handle)->numChannels);

	dec_out_buf_size = enc_info.frameLength;

	if (aacDecoder_SetParam(dec_handle, AAC_PCM_OUTPUT_CHANNELS, 1) != AAC_DEC_OK) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "unable to set number of pcm output channels");
		return -1;
	}

	if (aacDecoder_SetParam(dec_handle, AAC_PCM_OUTPUT_INTERLEAVED, 0) != AAC_DEC_OK) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "unable to set number of interleaving to 0");
		return -1;
	}

	open_error = false;
	return 0;
}

extern "C"
JNIEXPORT jint JNICALL Java_org_sipdroid_codecs_AAC_encode (JNIEnv *env, jobject obj, jshortArray lin, jint offset, jbyteArray encoded, jint size) {
	int out_bytes = 0;
	pthread_mutex_lock(&enc_lock);
	if(enc_handle != NULL) {
		// pointers to encoder's in/out buffers
		jbyte enc_out_buf[enc_out_buf_size];
		jshort enc_in_buf[enc_in_buf_size];

		enc_in_ptr = (uint16_t*) enc_in_buf;
		enc_out_ptr = (uint8_t*) enc_out_buf;

		// fill input buffer
		env->GetShortArrayRegion(lin, offset , size, enc_in_buf);
		// encode frame
		if ((enc_err = aacEncEncode(enc_handle, &enc_in_bufdesc, &enc_out_bufdesc, &enc_in_args, &enc_out_args)) != AACENC_OK) {
			__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_ENCODER, "encoding failed AAC_ENCODER_ERROR %x", enc_err);
		}
		else {
			// set AU header
			uint16_t au_size = (uint16_t) enc_out_args.numOutBytes;
			// first 8 of 13 bits for AU-size
			enc_au_header[2] = (jbyte) ((au_size << 3) >> 8);
			// last 5 of 13 bits for AU-size + 3 bits for AU-Index (always 0, see RFC 3640)
			enc_au_header[3] = (jbyte) ((au_size << 11) >> 8);
			//set marker bit
			jbyte hdr_byte;
			env->GetByteArrayRegion(encoded, 1, 1, &hdr_byte);
			hdr_byte |= 0x80;
			env->SetByteArrayRegion(encoded, 1, 1, &hdr_byte);
			// write AU header
			env->SetByteArrayRegion(encoded, RTP_HDR_SIZE, AU_HDR_SIZE, enc_au_header);
			// write AU (frame)
			env->SetByteArrayRegion(encoded, TOTAL_HDR_SIZE, au_size, enc_out_buf);

			out_bytes = AU_HDR_SIZE + au_size;
		}
	}
	pthread_mutex_unlock(&enc_lock);
	return out_bytes;
}

extern "C"
JNIEXPORT jint JNICALL Java_org_sipdroid_codecs_AAC_decode (JNIEnv *env, jobject obj, jbyteArray encoded, jshortArray lin, jint size) {
	int out_samples = 0;
	pthread_mutex_lock(&dec_lock);
	if(dec_handle != NULL) {
		// get AU size
		env->GetByteArrayRegion(encoded, RTP_HDR_SIZE , AU_HDR_SIZE, (jbyte*) dec_au_header);
		UINT au_size = 0;
		au_size = (UINT) (dec_au_header[2] << 8);
		au_size |= (UINT) dec_au_header[3];
		au_size >>= 3;

		// fill external input buffer
		UCHAR dec_in_buf[au_size];
		UCHAR* dec_in_buf_pt = dec_in_buf;
		env->GetByteArrayRegion(encoded, TOTAL_HDR_SIZE , au_size, (jbyte*) dec_in_buf);

		// fill internal input buffer
		UINT bytes_valid = au_size;
		while(bytes_valid != 0) {
			if ((dec_err = aacDecoder_Fill(dec_handle, &dec_in_buf_pt, &au_size, &bytes_valid)) != AAC_DEC_OK) {
				__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "error filling decoder buffer. AAC_DECODER_ERROR %x", dec_err);
				pthread_mutex_unlock(&dec_lock);
				return 0;
			}
		}

		// decode frame
		INT_PCM dec_out_buf[sizeof(INT_PCM)*dec_out_buf_size];
		if((dec_err = aacDecoder_DecodeFrame(dec_handle, dec_out_buf, dec_out_buf_size, 0)) != AAC_DEC_OK) {
			__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "error decoding frame. AAC_DECODER_ERROR %x", dec_err);
		}
		else {
			// write PCM
			out_samples = aacDecoder_GetStreamInfo(dec_handle)->frameSize;
			env->SetShortArrayRegion(lin, 0, out_samples, (jshort*) dec_out_buf);
			//aacDecoder_SetParam(dec_handle, AAC_TPDEC_CLEAR_BUFFER, 1);
		}
	}
	pthread_mutex_unlock(&dec_lock);
	return out_samples;
}

extern "C"
JNIEXPORT void JNICALL Java_org_sipdroid_codecs_AAC_close
(JNIEnv *env, jobject obj) {
	if(mutexes_initialized) {
		if(enc_handle) {
			pthread_mutex_lock(&enc_lock);
			aacEncClose(&enc_handle);
			enc_handle = NULL;
			pthread_mutex_unlock(&enc_lock);
		}
		if(dec_handle) {
			pthread_mutex_lock(&dec_lock);
			aacDecoder_Close(dec_handle);
			dec_handle = NULL;
			pthread_mutex_unlock(&dec_lock);
		}
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "cleanup complete");
	}
}

