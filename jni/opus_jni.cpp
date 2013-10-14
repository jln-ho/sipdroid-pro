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

#include <jni.h>
#include <android/log.h>
#include <string.h>
#include <stdint.h>
#include <stddef.h>
#include <stdlib.h>
#include <pthread.h>
#include "opus.h"

#define DEBUG_TAG_ENCODER "OPUS_ENC"
#define DEBUG_TAG_DECODER "OPUS_DEC"

#define MIN_FRAME_SIZE_MS 0.0025
#define MAX_FRAME_SIZE_MS 0.06
#define MAX_OUT_SAMPLES 5760

#define	 RTP_HDR_SIZE	12

OpusEncoder *enc = 0;
OpusDecoder *dec = 0;

int samp_rate = 0;
int enc_mode = OPUS_AUTO;
int frame_size = 0;
int max_bitrate = 0;
int fec_flag = 1;
int dtx_flag = 1;
int vbr_flag = 1;
int seq_prev = -1;

pthread_mutex_t enc_lock;
pthread_mutex_t dec_lock;
bool mutexes_initialized = false;

// buffer for the previously received packet (used for FEC)
unsigned char* dec_in_buf_prev = 0;
int size_prev;
bool lost_prev = false;

extern "C"
JNIEXPORT jint JNICALL Java_org_sipdroid_codecs_Opus_open
	(JNIEnv *env, jobject obj, jint samplerate, jint mode, jint framesize, jint maxbrate, jboolean fec, jboolean dtx, jboolean cbr) {
	if(!mutexes_initialized){
		pthread_mutex_init(&enc_lock,NULL);
		pthread_mutex_init(&dec_lock,NULL);
		mutexes_initialized = true;
	}
	if (samp_rate == samplerate
		&& enc_mode == mode
		&& frame_size == framesize
		&& max_bitrate == maxbrate
		&& ((fec && fec_flag == 1) || (!fec && fec_flag == 0))
		&& ((dtx && dtx_flag == 1) || (!dtx && dtx_flag == 0))
		&& ((cbr && vbr_flag == 0) || (!cbr && vbr_flag == 1))
		&& enc != 0 && dec != 0 ){
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_ENCODER, "opus already open. returning.");
		return 0;
	}
	if(samplerate != 8000 && samplerate != 12000 && samplerate != 16000 && samplerate != 24000 && samplerate != 48000){
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_ENCODER, "could not initialize encoder: unsupported sampling rate: %d", samplerate);
		return -1;
	}
	if(mode != OPUS_AUTO && mode != OPUS_APPLICATION_VOIP && mode != OPUS_APPLICATION_AUDIO && mode != OPUS_APPLICATION_RESTRICTED_LOWDELAY){
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_ENCODER, "could not initialize encoder: invalid mode: %d", mode);
		return -1;
	}
	float ms = 1000 * (float) framesize*(1.0/samplerate);
	if(ms != 2.5 && ms != 5 && ms != 10 && ms != 20 && ms != 40 && ms != 60){
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_ENCODER, "could not initialize encoder: unsupported frame size: %d (%f ms)", framesize, ms);
		return -1;
	}
	int error;

	// apply configuration values
	samp_rate = samplerate;
	enc_mode = mode;
	frame_size = framesize;
	max_bitrate = maxbrate;
	if(cbr) vbr_flag = 0; else vbr_flag = 1;
	if(dtx) dtx_flag = 1; else dtx_flag = 0;
	if(fec) fec_flag = 1; else fec_flag = 0;

	// initialize encoder
	enc = opus_encoder_create(samp_rate, 1, enc_mode, &error);
	if(error != OPUS_OK){
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_ENCODER, "failed to initialize encoder. error code : %d", error);
		return -1;
	}
	if(max_bitrate != 0){
		opus_encoder_ctl(enc, OPUS_SET_BITRATE(max_bitrate));
	}

	opus_encoder_ctl(enc, OPUS_SET_INBAND_FEC(fec_flag));
	if(fec_flag){
		opus_encoder_ctl(enc, OPUS_SET_PACKET_LOSS_PERC(5));
	}

	opus_encoder_ctl(enc, OPUS_SET_VBR(vbr_flag));

	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_ENCODER, "encoder opened successfully. fec: %d", fec_flag);

	// initialize decoder
	dec = opus_decoder_create(samp_rate, 1, &error);
	if(error != OPUS_OK){
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "failed to initialize decoder. error code : %d", error);
		return -1;
	}
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "decoder opened successfully");

	return 0;
}

extern "C"
JNIEXPORT jint JNICALL Java_org_sipdroid_codecs_Opus_encode
    (JNIEnv *env, jobject obj, jshortArray lin, jint offset, jbyteArray encoded, jint size) {
	int out_bytes = 0;
	pthread_mutex_lock(&enc_lock);
	if(enc != NULL){
		opus_int16* enc_in_buf = (opus_int16*) malloc(sizeof(opus_int16*)*size);
		env->GetShortArrayRegion(lin, offset, size, enc_in_buf);
		unsigned char* enc_out_buf = (unsigned char*) malloc(4000);
		out_bytes = opus_encode(enc, enc_in_buf, frame_size, enc_out_buf, 4000);
		if(out_bytes <= 1){
			__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_ENCODER, "error encoding frame. code %d", out_bytes);
			out_bytes = 0;
		}
		else{
			env->SetByteArrayRegion(encoded, RTP_HDR_SIZE, out_bytes, (jbyte*) enc_out_buf);
		}
		free(enc_in_buf);
		free(enc_out_buf);
	}
	pthread_mutex_unlock(&enc_lock);
	return out_bytes;
}

extern "C"
JNIEXPORT jint JNICALL Java_org_sipdroid_codecs_Opus_decode
    (JNIEnv *env, jobject obj, jbyteArray encoded, jshortArray lin, jint size) {
	int out_samples = 0;
	pthread_mutex_lock(&dec_lock);
	if(dec != NULL){
		unsigned char* dec_in_buf = (unsigned char*) malloc(size);
		env->GetByteArrayRegion(encoded, RTP_HDR_SIZE, size, (jbyte*) dec_in_buf);
		opus_int16* dec_out_buf = (opus_int16*) malloc(sizeof(opus_int16*)*MAX_OUT_SAMPLES);

		if(fec_flag){
			bool lost = false;
			// get sequence number from packet (3rd and 4th byte in RTP header)
			unsigned char* seq_nr_buf = (unsigned char*) malloc(2);
			env->GetByteArrayRegion(encoded, 2, 2, (jbyte*) seq_nr_buf);
			short seq_nr = (short) seq_nr_buf[0] << 8 | seq_nr_buf[1];
			free(seq_nr_buf);
			// detect packet loss
			if(seq_prev >= 0 && seq_nr - seq_prev > 1){
				lost = true;
				__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "PACKET LOSS!! %d", seq_nr);
			}
			if(lost_prev){
				int output_size_prev;
				opus_decoder_ctl(dec, OPUS_GET_LAST_PACKET_DURATION(&output_size_prev));
				out_samples = opus_decode(dec, dec_in_buf, size, dec_out_buf, output_size_prev, 1);
				__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "FEC DECODE %d ", out_samples);
			}
			else if(dec_in_buf_prev){
				out_samples = opus_decode(dec, dec_in_buf_prev, size_prev, dec_out_buf, MAX_OUT_SAMPLES, 0);
			}
			lost_prev = lost;
			seq_prev = seq_nr;
		}
		else{
			out_samples = opus_decode(dec, dec_in_buf, size, dec_out_buf, MAX_OUT_SAMPLES, 0);
		}
		if(out_samples <= 0){
			__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "error decoding frame. code %d", out_samples);
			out_samples = 0;
		}
		else{
			env->SetShortArrayRegion(lin, 0, out_samples, dec_out_buf);
		}
		if(dec_in_buf_prev){
			free(dec_in_buf_prev);
		}
		dec_in_buf_prev = dec_in_buf;
		size_prev = size;
		free(dec_out_buf);
	}
	pthread_mutex_unlock(&dec_lock);
	return out_samples;
}

extern "C"
JNIEXPORT void JNICALL Java_org_sipdroid_codecs_Opus_cleanup
	(JNIEnv *env, jobject obj) {
	if(mutexes_initialized){
		if(dec){
			pthread_mutex_lock(&dec_lock);
			opus_decoder_destroy(dec);
			dec = NULL;
			pthread_mutex_unlock(&dec_lock);
		}
		if(enc){
			pthread_mutex_lock(&enc_lock);
			opus_encoder_destroy(enc);
			enc = NULL;
			pthread_mutex_unlock(&enc_lock);
		}
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "cleanup complete");
	}
}
