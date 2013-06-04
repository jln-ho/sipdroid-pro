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
#include <iostream>
#include <stdint.h>
#include <stddef.h>
#include <stdlib.h>
#include <stdio.h>
#include "aacdecoder_lib.h"
#include <pthread.h>
extern "C"{
	#include "libavformat/avformat.h"
}

#define DEBUG_TAG_DECODER "AAC_FILE_DEC"

#define OUTPUT_SIZE 8*2*1024
int output_size = OUTPUT_SIZE;

pthread_mutex_t lock;
bool mutex_initialized = false;

HANDLE_AACDECODER dec_handle;
AAC_DECODER_ERROR dec_err;
UINT dec_out_buf_size;

FILE *in;

TRANSPORT_TYPE file_type = TT_UNKNOWN;

AVFormatContext *avformat_in = NULL;
AVStream *av_stream = NULL;

int get_mp4_asc(const char *file, UCHAR* asc_buf){
	av_register_all();
	avformat_network_init();
	int ret = avformat_open_input(&avformat_in, file, NULL, NULL);
	if (ret < 0) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "av_open_input failed");
		return -1;
	}
	for (int i = 0; i < avformat_in->nb_streams && !av_stream; i++) {
		if (avformat_in->streams[i]->codec->codec_id == CODEC_ID_AAC)
			av_stream = avformat_in->streams[i];
	}
	if (!av_stream) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "no AAC stream found");
		return -1;
	}
	if (!av_stream->codec->extradata_size) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "no ASC found");
		return -1;
	}
	memcpy(asc_buf, av_stream->codec->extradata, av_stream->codec->extradata_size);
	return av_stream->codec->extradata_size;
}

int decode_mp4_packet(FILE *file, HANDLE_AACDECODER &dec_adts_handle, int16_t* dec_buffer){
	UINT valid;
	AVPacket pkt = { 0 };
	int ret = av_read_frame(avformat_in, &pkt);
	if (ret == AVERROR(EAGAIN)){
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "EAGAIN");
		return 0;
	}
	if (pkt.stream_index != av_stream->index) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "packet index != stream index");
		av_free_packet(&pkt);
		return 0;
	}
	valid = pkt.size;
	dec_err = aacDecoder_Fill(dec_handle, &pkt.data, (UINT*) &pkt.size, &valid);
	if (dec_err != AAC_DEC_OK) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "fill failed");
		return 0;
	}
	dec_err = aacDecoder_DecodeFrame(dec_handle, dec_buffer, output_size, 0);
	av_free_packet(&pkt);
	if (dec_err != AAC_DEC_OK) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "decoding failed");
		return 0;
	}

	CStreamInfo *info = aacDecoder_GetStreamInfo(dec_adts_handle);
	return info->frameSize * info->numChannels;
}

int decode_adts_packet(FILE *file, HANDLE_AACDECODER &dec_adts_handle, int16_t* dec_buffer){
	uint8_t packet[10240], *ptr = packet;
	int n, i;
	UINT packet_size;
	n = fread(packet, 1, 7, file);
	if (n != 7){
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "invalid file");
		return 0;
	}
	if (packet[0] != 0xff || (packet[1] & 0xf0) != 0xf0) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "not an ADTS packet");
		return 0;
	}
	packet_size = ((packet[3] & 0x03) << 11) | (packet[4] << 3) | (packet[5] >> 5);
	n = fread(packet + 7, 1, packet_size - 7, file);
	if (n != packet_size - 7) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "partial packet");
		return 0;
	}
	UINT valid = packet_size;
	dec_err = aacDecoder_Fill(dec_adts_handle, &ptr, &packet_size, &valid);
	if (dec_err != AAC_DEC_OK) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "fill failed");
		return 0;
	}
	dec_err = aacDecoder_DecodeFrame(dec_adts_handle, dec_buffer, output_size, 0);
	if (dec_err != AAC_DEC_OK) {
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "Decode failed: %x\n", dec_err);
		return 0;
	}
	CStreamInfo *info = aacDecoder_GetStreamInfo(dec_adts_handle);
	return info->frameSize * info->numChannels;
}

extern "C"
JNIEXPORT jobject JNICALL Java_org_sipdroid_media_file_AACFile_ntryOpen
	(JNIEnv *env, jobject obj, jstring path){

	file_type = TT_UNKNOWN;

	if(!mutex_initialized){
		pthread_mutex_init(&lock,NULL);
		mutex_initialized = true;
	}
	// convert given path to cstr
	const char *strPath = env->GetStringUTFChars(path, 0);
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "trying to open %s", strPath);

	// open file
	in = fopen(strPath, "rb");

	if(!in){
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "error opening file: ", strPath);
		env->ReleaseStringUTFChars(path, strPath);
		return NULL;
	}
	__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "aac file opened sucessfully: %s", strPath);

	// Get audiofile information class from java project
	jclass clazz = env->FindClass("org/sipdroid/media/file/AudioFileInformations");
	if (clazz == 0){
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "error referencing AudioFileInformations object in JNI");
		env->ReleaseStringUTFChars(path, strPath);
		fclose(in);
		return NULL;
	}

	// reference fields
	jobject infoObj = env->AllocObject(clazz);
	jfieldID fidSuccess = env->GetFieldID(clazz, "success", "Z");
	jfieldID fidError = env->GetFieldID(clazz, "error", "Ljava/lang/String;");
	jfieldID fidRate = env->GetFieldID(clazz, "rate", "J");
	jfieldID fidChannels = env->GetFieldID(clazz, "channels", "I");
	jfieldID fidEncoding = env->GetFieldID(clazz, "encoding", "I");
	jfieldID fidBitrate= env->GetFieldID(clazz, "bitrate", "I");
	jfieldID fidFrameSize= env->GetFieldID(clazz, "framesize", "I");

	if(infoObj == 0 || fidSuccess == 0 || fidError == 0 || fidRate == 0 || fidChannels == 0 || fidEncoding == 0 || fidBitrate == 0 || fidFrameSize == 0){
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "error initializing AudioFileInformations object in JNI");
		env->ReleaseStringUTFChars(path, strPath);
		fclose(in);
		return NULL;
	}

	CStreamInfo* info;
	std::string stdPath(strPath);
	TRANSPORT_TYPE assumed_transport_type = TT_UNKNOWN;
	int16_t decode_buf[OUTPUT_SIZE];

	const char* err = NULL;

	//check for .mp4 or .aac file format
	if (stdPath.find(".m4a") != std::string::npos) {
		assumed_transport_type = TT_MP4_RAW;
	}
	else if (stdPath.find(".aac") != std::string::npos) {
		assumed_transport_type = TT_MP4_ADTS;
	}
	if(assumed_transport_type != TT_UNKNOWN){
		dec_handle = aacDecoder_Open(assumed_transport_type, 1);
		int ret = 0;
		if(assumed_transport_type == TT_MP4_RAW){
			avformat_in = NULL;
			av_stream = NULL;
			UCHAR asc_buf[4096];
			UCHAR* asc_buf_pt = asc_buf;
			fclose(in);
			ret = get_mp4_asc(stdPath.c_str(), asc_buf);
			in = fopen(strPath, "rb");
			UINT asc_buf_size = ret;
			if (ret <= 0 || aacDecoder_ConfigRaw(dec_handle, &asc_buf_pt, &asc_buf_size) != AAC_DEC_OK) {
				err = "unable to configure decoder";
			}
		}
		else if(assumed_transport_type == TT_MP4_ADTS){
			ret = decode_adts_packet(in, dec_handle, decode_buf);
		}
		if(ret > 0){
			file_type = assumed_transport_type;
			info = aacDecoder_GetStreamInfo(dec_handle);
		}
	}
	if(file_type == TT_UNKNOWN){
		err = "unsupported file format";
	}

	if(err != NULL){
		env->SetBooleanField(infoObj, fidSuccess, JNI_FALSE);
		env->SetObjectField(infoObj, fidError, env->NewStringUTF(err));
		env->ReleaseStringUTFChars(path, strPath);
		return infoObj;
	}

	// set class attributes according to audio information
	int numChannels, frameSize;
	if(file_type == TT_MP4_RAW){
		numChannels =info->channelConfig;
		frameSize = info->aacSamplesPerFrame;
	}
	else{
		numChannels = info->numChannels;
		frameSize = info->frameSize;
	}
	env->SetLongField(infoObj, fidRate, info->aacSampleRate);
	env->SetIntField(infoObj, fidChannels, numChannels);
	env->SetIntField(infoObj, fidEncoding, info->aot);
	env->SetIntField(infoObj, fidBitrate, info->bitRate);
	env->SetIntField(infoObj, fidFrameSize,  frameSize * numChannels);
	env->SetBooleanField(infoObj, fidSuccess, JNI_TRUE);

	// seek to beginning of file for future decoding
	rewind(in);
	env->ReleaseStringUTFChars(path, strPath);

	return infoObj;
}

extern "C"
JNIEXPORT jint JNICALL Java_org_sipdroid_media_file_AACFile_ndecodeFrame
	(JNIEnv *env, jobject obj, jshortArray lin, jint offset, jint max_samples){
	pthread_mutex_lock(&lock);
	if(dec_handle != NULL){
		int16_t decode_buf[OUTPUT_SIZE];
		int num_decoded_samples = 0;
		if(file_type == TT_MP4_ADTS){
			num_decoded_samples = decode_adts_packet(in, dec_handle, decode_buf);
		}
		else if(file_type == TT_MP4_RAW){
			num_decoded_samples = decode_mp4_packet(in, dec_handle, decode_buf);
		}
		if(max_samples < num_decoded_samples){
			__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "not enough space in provided output buffer");
			pthread_mutex_unlock(&lock);
			return 0;
		}
		env->SetShortArrayRegion(lin, offset, num_decoded_samples, (jshort*) decode_buf);

		pthread_mutex_unlock(&lock);
		return num_decoded_samples;
	}
	pthread_mutex_unlock(&lock);
	return 0;
}

extern "C"
JNIEXPORT void JNICALL Java_org_sipdroid_media_file_AACFile_ncleanUp (JNIEnv *env, jobject obj){
	if(mutex_initialized){
		pthread_mutex_lock(&lock);
		aacDecoder_Close(dec_handle);
		dec_handle = NULL;
		fclose(in);
		if(avformat_in) avformat_close_input(&avformat_in);
		pthread_mutex_unlock(&lock);
		__android_log_print(ANDROID_LOG_DEBUG, DEBUG_TAG_DECODER, "cleanup complete");
	}
}
