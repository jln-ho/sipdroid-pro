/*
 * Copyright (C) 2010 The Sipdroid Open Source Project
 * Copyright (C) 2007 The Android Open Source Project
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

import java.util.Arrays;

import org.sipdroid.media.RingBuffer;
import org.sipdroid.media.file.AudioFileInformations.FileType;

public class AACFile extends AudioFile {
	
	static{
		System.loadLibrary("FraunhoferAAC");
		System.loadLibrary("ffmpeg");
		System.loadLibrary("aac_file_jni");
	}

	private RingBuffer pcmBuf;
	private short[] frameBuf;
	private Thread fileDecoderThread;

	public AACFile(String path){
		fileInfo = ntryOpen(path);
		fileInfo.fileType = FileType.AAC;
		if(fileInfo != null && fileInfo.success){
			pcmBuf = new RingBuffer(fileInfo.framesize * 32);
			frameBuf = new short[fileInfo.framesize];
			for(int i = 0; i < 32; i++){
				if(ndecodeFrame(frameBuf,0,frameBuf.length) > 0){
					pcmBuf.enqueue(frameBuf);
				}
				else{
					fileInfo.success = false;
					break;
				}
			}
			fileDecoderThread = new Thread(){
				public void run(){
					while(true){
						synchronized (pcmBuf) {
							try { pcmBuf.wait(); } catch (InterruptedException e) {break;}
							int frames = 0;
							if(pcmBuf.getFreeSlots() >= fileInfo.framesize){
								do{
									frames = ndecodeFrame(frameBuf,0,frameBuf.length);
									if(frames > 0) pcmBuf.enqueue(frameBuf);
								}
								while(frames != 0 && pcmBuf.getFreeSlots() >= fileInfo.framesize);
							}
						}
					}
				}
			};
			fileDecoderThread.setPriority(Thread.MIN_PRIORITY);
			fileDecoderThread.start();
		}
	}
	
	public boolean isOpen(){
		if(fileInfo != null)
			return fileInfo.success;
		return false;
	}

	@Override
	public boolean isSupported(){
		if(isOpen()){
			return fileInfo.channels <= 2 && fileInfo.framesize % 512 == 0;
		}
		return false;
	}

	@Override
	protected boolean getFrame(short[] buf) {
		if(pcmBuf.numOccupiedSlots() <= 1) return false;
		synchronized (pcmBuf) {
			if(fileInfo.channels == 1){
				if(buf.length > pcmBuf.numOccupiedSlots()){
					int numSamples = pcmBuf.numOccupiedSlots();
					buf = pcmBuf.dequeue(numSamples);
					Arrays.fill(buf, numSamples, buf.length, (short) 0);
				}
				else{
					buf = pcmBuf.dequeue(frameBuf.length);
				}
			}
			else if(fileInfo.channels == 2){
				short[] tmp;
				if(buf.length * 2 > pcmBuf.numOccupiedSlots()){
					short[] tmp2 = pcmBuf.dequeue(pcmBuf.numOccupiedSlots());
					if (tmp2.length % 2 != 0){
						tmp = new short[tmp2.length+1];
						for(int i = 0; i < tmp2.length; i++){
							tmp[i] = tmp2[i];
						}
					}
					else{
						tmp = tmp2;
					}
					tmp = pcmBuf.dequeue(pcmBuf.numOccupiedSlots());
				}
				else{
					tmp = pcmBuf.dequeue(buf.length * 2);
				}
				int pos = 0;
				for(int i = 0; i < tmp.length; i++){
					short downMixedSample = (short) ((tmp[i] + tmp[++i]) / 2);
					if (downMixedSample < -32768){
						downMixedSample = -32768;
					} 
					else if (downMixedSample > 32767){
						downMixedSample = 32767;
					}
					buf[pos++] = downMixedSample;
				}
			}
			pcmBuf.notifyAll();
		}
		return true;
	}
	
	@Override
	public void cleanup() {
		ncleanUp();
		fileDecoderThread.interrupt();
	}
	
	private static native AudioFileInformations ntryOpen(String path);
	private static native int ndecodeFrame(short[] lin, int offset, int maxNumSamples);
	private static native void ncleanUp();
}
