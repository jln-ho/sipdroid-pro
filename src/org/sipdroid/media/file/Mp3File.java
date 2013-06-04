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

package org.sipdroid.media.file;

import org.sipdroid.media.file.AudioFileInformations.FileType;


/**
 * Abstract class for native implementations
 * 
 * All native functions will be called through this class.
 * 
 * Controlling the MPG123 library need to be done through the service 
 * AIDL interface!
 * 
 *
 */
public class Mp3File extends AudioFile {
	
	private static final int MPG123_OK = 0;
	private static final int MPG123_NEW_FORMAT = -11;

	static{
		System.loadLibrary("mp3");
	}
	
	public Mp3File(String path, int sampleRate){
		Mp3File.initLib(sampleRate);
		Mp3File.initMP3(path);
		this.fileInfo = getAudioInformations();
		this.fileInfo.fileType = FileType.MP3;
	}
	
	public static boolean initLib(int sampleRate) {
		return ninitLib(sampleRate);
	}
	
	public static void cleanupLib() {
		ncleanupLib();
	}
	
	public static String getError() {
		return ngetError();
	}
	
	public static int initMP3(String filename) {
		return ninitMP3(filename);
	}
	
	public static void cleanupMP3() {
		ncleanupMP3();
	}
	
	public static boolean setEQ(int channel, double vol) {
		return nsetEQ(channel, vol);
	}
	
	public static void resetEQ() {
		nresetEQ();
	}
	
	public static AudioFileInformations getAudioInformations() {
		return ngetAudioInformations();
	}
	
	public static int decodeMP3(int bufferLen, short[] buffer) {
		return ndecodeMP3(bufferLen, buffer);
	}
	
	public static void seekTo(int frames) {
		nseekTo(frames);
	}

	/**
	 * 
	 * @return
	 */
	private static native boolean ninitLib(int sampleRate);
	
	/**
	 * 
	 */
	private static native void ncleanupLib();
	
	/**
	 * 
	 * @return String explaining what went wrong
	 */
	private static native String ngetError();
	
	/**
	 * Initialize one MP3 file
	 * @param filename
	 * @return MPG123_OK
	 */
	private static native int ninitMP3(String filename);
	
	/**
	 * Cleanup all native needed resources for one MP3 file
	 */
	private static native void ncleanupMP3();
	
	/**
	 * 
	 * @param channel
	 * @param vol
	 * @return
	 */
	private static native boolean nsetEQ(int channel, double vol);
	
	/**
	 * 
	 */
	private static native void nresetEQ();
	
	/**
	 * 
	 * @return
	 */
	private static native AudioFileInformations ngetAudioInformations();
	
    /**
	 * Read, decode and write PCM data to our java application
	 * 
	 * @param bufferLen
	 * @param buffer
	 * @return 
	 */
    private static native int ndecodeMP3(int bufferLen, short[] buffer);
    
    /**
     * 
     * @param frames
     */
    private static native void nseekTo(int frames);
    
    /**
     * Our native MPEG (1,2 and 3) decoder library
     */

	@Override
	public boolean isSupported() {
		return true;
	}

	@Override
	protected boolean getFrame(short[] frameBuf) {
		int code = ndecodeMP3(frameBuf.length * 2, frameBuf);
		return code == MPG123_OK || code == MPG123_NEW_FORMAT;
	}

	@Override
	public void cleanup() {
		ncleanupMP3();
		ncleanupLib();
	}
}
