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
#include "samplerate.h"
#include <pthread.h>

#define DEBUG_TAG "RESAMPLE_JNI"

SRC_STATE* converter = NULL;
pthread_mutex_t lock;
bool ready = false;

extern "C"
JNIEXPORT jint JNICALL Java_org_sipdroid_media_file_AudioFile_ninitResample(JNIEnv* env, jobject obj){
	if(!ready){
		ready = true;
		return pthread_mutex_init(&lock, NULL);
	}
	return 0;
}

extern "C"
JNIEXPORT jint JNICALL Java_org_sipdroid_media_file_AudioFile_nresample(JNIEnv* env, jobject obj, jdouble ratio, jshortArray inBuffer, jshortArray outBuffer){
	if(ready){
		pthread_mutex_lock(&lock);
		// initialize converter
		if(converter == NULL){
			int error;
			converter = src_new(SRC_SINC_MEDIUM_QUALITY, 1, &error);
			if(converter == NULL){
				__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "unable to initialize sample rate converter: %s", src_strerror(error));
				pthread_mutex_unlock(&lock);
				return -1;
			}
		}
		// prepare buffers
		jint input_len = env->GetArrayLength(inBuffer);
		float* fl_inBuffer = (float*) malloc(input_len * sizeof(float));
		short* sh_inBuffer = env->GetShortArrayElements(inBuffer, NULL);
		src_short_to_float_array(sh_inBuffer, fl_inBuffer, input_len);
		env->ReleaseShortArrayElements(inBuffer, sh_inBuffer, 0);

		jint output_len = env->GetArrayLength(outBuffer);
		float* fl_outBuffer = (float*) malloc(sizeof(float) * output_len);

		SRC_DATA src_data;
		src_data.data_in = fl_inBuffer;
		src_data.input_frames = (long) input_len;
		src_data.data_out = fl_outBuffer;
		src_data.output_frames = (long) output_len;
		src_data.src_ratio = (double) 1/ratio;
		src_data.end_of_input = 0;

		// resample
		int error;
		if ((error = src_process(converter, &src_data)) >= 0){
			// convert output to float and write to outBuffer
			short* sh_outBuffer = env->GetShortArrayElements(outBuffer, NULL);
			src_float_to_short_array(src_data.data_out, sh_outBuffer, src_data.output_frames_gen);
			env->ReleaseShortArrayElements(outBuffer, sh_outBuffer, 0);
		}
		else{
			__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG, "resampling error: %s", src_strerror(error));
		}

		free(fl_outBuffer);
		free(fl_inBuffer);

		pthread_mutex_unlock(&lock);
		return src_data.output_frames_gen;
	}
	return 0;
}

extern "C"
JNIEXPORT jint JNICALL Java_org_sipdroid_media_file_AudioFile_ncleanupResample(JNIEnv* env, jobject obj){
	if(ready){
		ready = false;
		pthread_mutex_lock(&lock);
		converter = src_delete(converter);
		pthread_mutex_unlock(&lock);
		return pthread_mutex_destroy(&lock);
	}
	return -1;
}
