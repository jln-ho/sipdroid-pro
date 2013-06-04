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

/*
arm:
sizeof(char)      = 1
sizeof(short)     = 2
sizeof(int)       = 4
sizeof(long)      = 4
sizeof(long long) = 8
sizeof(float)     = 4
sizeof(double)    = 8
*/

#include <jni.h>
#include <math.h>
#include <stdio.h>
#include <arpa/inet.h>
#include <sys/ioctl.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include "mp3.h"
#include "../mpg123/mpg123.h"


mpg123_handle *mh = NULL;


/*
 * Init MPG123, must be done one per process and before all other functions!
 */
JNIEXPORT jboolean JNICALL Java_org_sipdroid_media_file_Mp3File_ninitLib(JNIEnv* env, jobject this, jint sampleRate)
{	 
	__android_log_print(ANDROID_LOG_DEBUG, "MP3", "init mp3...");
	jint err = MPG123_ERR;

	if(mpg123_init() != MPG123_OK || (mh = mpg123_new(NULL, &err)) == NULL)
		return JNI_FALSE;

	/*
	 * Setup needed format options
	 */
	mpg123_format_none(mh);
	if ((err = mpg123_format(mh, sampleRate, MPG123_MONO, MPG123_ENC_SIGNED_16)) != MPG123_OK) 
		return JNI_FALSE;

	mpg123_volume();
	__android_log_print(ANDROID_LOG_DEBUG, "MP3", "mp3 initialized");
	return JNI_TRUE;
}

/*
 * Finish our MPG123 library
 */
JNIEXPORT void JNICALL Java_org_sipdroid_media_file_Mp3File_ncleanupLib(JNIEnv* env, jobject this)
{
	mpg123_delete(mh);
	mpg123_exit();
	mh = NULL;
}

/*
 * Get last error, explaining string
 */
JNIEXPORT jstring JNICALL Java_org_sipdroid_media_file_Mp3File_ngetError(JNIEnv* env, jobject this)
{
	const char *err_string = mpg123_strerror(mh);
	return (*env)->NewStringUTF(env, err_string);
}

/*
 * Init one MP3 file
 */
JNIEXPORT jint JNICALL Java_org_sipdroid_media_file_Mp3File_ninitMP3(JNIEnv* env, jobject this, jstring filename)
{
	jint err = MPG123_ERR;

	const char *mfile = (*env)->GetStringUTFChars(env, filename, NULL); // This is a UTF8 String!
	if(mfile == NULL)
		return -2;

	// Init and access new MP3 file
	if((err = mpg123_open(mh, mfile)) != MPG123_OK) 
		return err;

  	(*env)->ReleaseStringUTFChars(env, filename, mfile);


/* 
	int i = 1;
 
	double in = 1.0;

	if(mpg123_eq(mh, MPG123_LEFT | MPG123_RIGHT, 0, in) != MPG123_OK)
	 	dprintf(0, "eq failed\n");

	in = 1.0;
	
  for(i = 1; i < 32; i++)
  {
	  if(mpg123_eq(mh, MPG123_LEFT | MPG123_RIGHT, i, in) != MPG123_OK)
		  dprintf(0, "eq failed\n");
  }
*/

	err = MPG123_OK;
	return err;
}

/*
 * Close and finish all handles to one MP3 file
 */
void Java_org_sipdroid_media_file_Mp3File_ncleanupMP3(JNIEnv* env, jobject this)
{
	mpg123_close(mh);
}

/*
 *
 */
JNIEXPORT jboolean JNICALL Java_org_sipdroid_media_file_Mp3File_nsetEQ(JNIEnv* env, jobject this, jint ch, jdouble val)
{
	if(mpg123_eq(mh, MPG123_LEFT | MPG123_RIGHT, ch, val) != MPG123_OK)
		return JNI_FALSE;

	return JNI_TRUE;
}

/*
 *
 */
JNIEXPORT void JNICALL Java_org_sipdroid_media_file_Mp3File_nresetEQ(JNIEnv* env, jobject this)
{
	mpg123_reset_eq(mh);
}


/*
 * Get needed informations about the accessed MP3 audio file
 */
JNIEXPORT jobject JNICALL Java_org_sipdroid_media_file_Mp3File_ngetAudioInformations(JNIEnv* env, jobject this, jclass clazz)
{
  	// Get our class defined in java project
	clazz = (*env)->FindClass(env, "org/sipdroid/media/file/AudioFileInformations");
	if (clazz == 0)
		return NULL;

	jobject obj = (*env)->AllocObject(env, clazz);
	jfieldID fidSuccess = (*env)->GetFieldID(env, clazz, "success", "Z");
	jfieldID fidError = (*env)->GetFieldID(env, clazz, "error", "Ljava/lang/String;");
	jfieldID fidRate = (*env)->GetFieldID(env, clazz, "rate", "J");
	jfieldID fidChannels = (*env)->GetFieldID(env, clazz, "channels", "I");
	jfieldID fidEncoding = (*env)->GetFieldID(env, clazz, "encoding", "I");
	jfieldID fidBitrateMode = (*env)->GetFieldID(env, clazz, "bitratemode", "I");
	jfieldID fidBitrate= (*env)->GetFieldID(env, clazz, "bitrate", "I");
	jfieldID fidLength = (*env)->GetFieldID(env, clazz, "length", "J");

	if(obj == 0 || fidSuccess == 0 || fidError == 0 || fidRate == 0 || fidChannels == 0 || fidEncoding == 0 || fidLength == 0)
		return NULL;

	if(mh == NULL)
	{
		// mpg123 isn't initialized
		(*env)->SetBooleanField(env, obj, fidSuccess, JNI_FALSE);
		(*env)->SetObjectField(env, obj, fidError, (*env)->NewStringUTF(env, "mpg123 not initialized"));
		return obj;
	}

	struct mpg123_frameinfo info = {0};
	mpg123_info(mh, &info);

	(*env)->SetLongField(env, obj, fidRate, info.rate);
	(*env)->SetIntField(env, obj, fidChannels, info.mode);
	(*env)->SetIntField(env, obj, fidEncoding, 1);
	(*env)->SetIntField(env, obj, fidBitrateMode, info.vbr);
	(*env)->SetIntField(env, obj, fidBitrate, info.bitrate);
/*
	// Get length in seconds
	long length_in_mseconds = 0;
	off_t length_in_samples = mpg123_length(mh);
	if(length_in_samples != MPG123_ERR)
		length_in_mseconds /= (info.rate / 1000);

	(*env)->SetLongField(env, obj, fidLength, length_in_mseconds);
*/
	(*env)->SetBooleanField(env, obj, fidSuccess, JNI_TRUE);
	return obj;
}

/*
 * Decode our MP3 file
 */
JNIEXPORT jint JNICALL Java_org_sipdroid_media_file_Mp3File_ndecodeMP3(JNIEnv* env, jobject this, jint inlen, jshortArray jpcm)
{
	jint err = MPG123_ERR;
	jint outlen = 0;
	jshort *pcm = NULL;

	// Write PCM Data to byte array
	pcm = (*env)->GetShortArrayElements(env, jpcm, NULL);
	err = mpg123_read(mh, (unsigned char*) pcm, inlen, &outlen);
	(*env)->ReleaseShortArrayElements(env, jpcm, pcm, 0);

	return err;
}

/*
 * Seek to specified offset, starting from 0, in milliseconds
 */
JNIEXPORT void JNICALL Java_org_sipdroid_media_file_Mp3File_nseekTo(JNIEnv* env, jobject this, jint pos)
{
	if(mh != NULL)
		mpg123_seek(mh, pos, SEEK_SET);
}
