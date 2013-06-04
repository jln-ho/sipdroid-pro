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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.sipdroid.media.file.AudioFileInformations.FileType;

import android.util.Log;

public abstract class AudioFile {
	
    static {
		  System.loadLibrary("resample");
		  System.loadLibrary("resample_jni"); 
    }
	
	protected AudioFileInformations fileInfo = new AudioFileInformations();
	private double resampleRatio = -1;
	
	public static AudioFile open(String path, int sampleRate){
		AudioFile openedFile = null;
		// MP3
		if(path.endsWith(".mp3")){
			openedFile = new Mp3File(path, sampleRate);
		}
		// AAC / M4A
		else if(path.endsWith(".aac") || path.endsWith(".m4a")){
			AACFile aacFile = new AACFile(path);
			if(aacFile.isOpen() && aacFile.isSupported()){
				openedFile = aacFile;
			}
		}
		// WAV
		else if(path.endsWith(".wav")){
			try {
				WavFile wavFile = WavFile.openWavFile(new FileInputStream(path));
				if(wavFile.isSupported()){
					wavFile.fileInfo.success = true;
					wavFile.fileInfo.rate = wavFile.getSampleRate();
					openedFile = wavFile;
				}
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		return openedFile;
	}
	
	public AudioFileInformations getFileInfo(){
		return fileInfo;
	}
	
	public FileType getFileType(){
		return fileInfo.fileType;
	}

	public boolean getFrame(short[] frameBuf, int sampRate){
		if(fileInfo.fileType == FileType.UNKNOWN) return false;
		if(fileInfo.fileType != FileType.MP3 && fileInfo.rate != sampRate){
			if(resampleRatio < 0){
				ninitResample();
				resampleRatio = ((1.0 * fileInfo.rate) / (1.0* sampRate));
			}
			short[] pcmReadBuffer = new short[(int) (resampleRatio * frameBuf.length) + 1]; 
			return getFrame(pcmReadBuffer) && (nresample(resampleRatio, pcmReadBuffer, frameBuf) > 0);
		}
		else{
			return getFrame(frameBuf);
		}
	}
    
	public void close(){
		resampleRatio = -1;
		ncleanupResample();
		cleanup();
	}
	
	public abstract boolean isSupported();
	protected abstract boolean getFrame(short[] frameBuf);
	protected abstract void cleanup();

	private static native int ninitResample();
    private static native int nresample(double ratio, short[] in, short[] out);
    private static native void ncleanupResample();
}
