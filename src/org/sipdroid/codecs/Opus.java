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
package org.sipdroid.codecs;

import java.util.HashMap;

import org.sipdroid.media.RtpStreamSender;
import org.sipdroid.sipua.ui.Sipdroid;

public class Opus extends CodecBase implements Codec {
	
	public static final int SAMPLING_RATE_48 = 48000;
	public static final int SAMPLING_RATE_24 = 24000;
	public static final int SAMPLING_RATE_16 = 16000;
	public static final int SAMPLING_RATE_12 = 12000;
	public static final int SAMPLING_RATE_8 = 8000;
	
	public static final int FRAME_SIZE_20 = 20;
	public static final int FRAME_SIZE_40 = 40;
	public static final int FRAME_SIZE_60 = 60;
	
	public static final int MODE_AUDIO = 2049;
	public static final int MODE_VOIP = 2048;
	public static final int MODE_LOW_DELAY = 2051;
	
	public enum Mode {
		AUDIO,
		VOIP,
		LOW_DELAY
	}
	
	private static final HashMap <Mode, Integer> OPUS_MODES = new HashMap<Mode,Integer>();
	static{
		OPUS_MODES.put(Mode.AUDIO, MODE_AUDIO);
		OPUS_MODES.put(Mode.VOIP, MODE_VOIP);
		OPUS_MODES.put(Mode.LOW_DELAY, MODE_LOW_DELAY);
	}
	
	public enum FrameSizeMs{
		TWENTY_MS,
		FOURTY_MS,
		SIXTY_MS
	}
	private static final HashMap <FrameSizeMs, Float> OPUS_FRAME_SIZES_MS = new HashMap<FrameSizeMs, Float>();
	static{
		OPUS_FRAME_SIZES_MS.put(FrameSizeMs.TWENTY_MS, (float) 0.02);
		OPUS_FRAME_SIZES_MS.put(FrameSizeMs.FOURTY_MS, (float) 0.04);
		OPUS_FRAME_SIZES_MS.put(FrameSizeMs.SIXTY_MS, (float) 0.06);
	}

	private Mode currMode = Mode.AUDIO;
	private FrameSizeMs currFrameSizeMs = FrameSizeMs.TWENTY_MS;
	private int maxBitRate = 0;
	private boolean fec = true;
	private boolean dtx = false;
	private boolean cbr = false;
	
	private int CODEC_NUMBER_OVERRIDE = 0;
	
	Opus() {
		CODEC_NAME = "Opus";
		CODEC_USER_NAME = "opus";
		CODEC_NUMBER = 107;
		CODEC_DEFAULT_SETTING = "wlanor3g";
		if(RtpStreamSender.isSupportedSampRate(SAMPLING_RATE_48)){
			setSampleRate(SAMPLING_RATE_48);
		}
		else{
			setSampleRate(SAMPLING_RATE_16);
		}
		super.update();
	}
	
	Opus(Mode mode){
		this();
		setMode(mode);
	}
	
	Opus(Mode mode, FrameSizeMs frameSize){
		this(mode);
		setFrameSize(frameSize);
	}
	
	public Opus(Mode mode, FrameSizeMs frameSize, int sampleRate){
		this(mode, frameSize);
		setSampleRate(sampleRate);
	}
	
	public Opus(Opus other){
		this(other.currMode, other.currFrameSizeMs, other.CODEC_SAMPLE_RATE);
	}
	
	private void updateDescription(){
		CODEC_DESCRIPTION = CODEC_SAMPLE_RATE / 1000 + " kHz | fsize: "+getFrameSizeSamples()+" | mode: " + currMode.toString();
	}
	
	public void setMode(Mode mode){
		this.currMode = mode;
		updateDescription();
	}
	
	public void setFrameSize(FrameSizeMs frameSize){
		this.currFrameSizeMs = frameSize;
		this.CODEC_FRAME_SIZE = getFrameSizeSamples();
		updateDescription();
	}
	
	public boolean setFrameSize(int frameSize){
		if(isSupportedFrameSize(frameSize)){
			for(FrameSizeMs currFs : OPUS_FRAME_SIZES_MS.keySet()){
				if(Math.ceil(OPUS_FRAME_SIZES_MS.get(currFs) * 1000) == frameSize){
					setFrameSize(currFs);
					return true;
				}
			}
		}
		return false;
	}
	
	public void setSampleRate(int sampleRate){
		CODEC_SAMPLE_RATE = sampleRate;
		CODEC_FRAME_SIZE = getFrameSizeSamples();
		updateDescription();
	}
	
	private int getFrameSizeSamples(){
		return 1 + (int) (OPUS_FRAME_SIZES_MS.get(currFrameSizeMs) / (1.0/CODEC_SAMPLE_RATE));
	}
	
	public FrameSizeMs getFrameSizeMs(){
		return this.currFrameSizeMs;
	}
	
	public int getFrameSizeMsInt(){
		return (int) (Math.ceil(OPUS_FRAME_SIZES_MS.get(currFrameSizeMs)*1000));
	}
	
	public static boolean isSupportedFrameSize(int frameSize){
		for(float currFs : OPUS_FRAME_SIZES_MS.values()){
			if(Math.ceil(currFs * 1000) == frameSize){
				return true;
			}
		}
		return false;
	}
	
	public static int getMaxFrameSizeMs(){
		int max = 0;
		for(float currFs : OPUS_FRAME_SIZES_MS.values()){
			if(Math.ceil(currFs * 1000) > max){
				max = (int) Math.ceil(currFs * 1000);
			}
		}
		return max;
	}
	
	public static int getMinFrameSizeMs(){
		int min = getMaxFrameSizeMs();
		for(float currFs : OPUS_FRAME_SIZES_MS.values()){
			if(Math.ceil(currFs * 1000) < min){
				min = (int) Math.ceil(currFs * 1000);
			}
		}
		return min;
	}

	public void setMaxBitRate(int maxBitRate) {
		this.maxBitRate = maxBitRate;
	}
	
	public void setFEC(boolean fec){
		this.fec = fec;
	}
	
	public int getFEC(){
		if(this.fec) return 1; return 0;
	}
	
	public Mode getMode(){
		return this.currMode;
	}
	
	public int getModeInt(){
		return OPUS_MODES.get(currMode);
	}
	
	
	public int getDTX(){
		if(this.dtx) return 1; return 0;
	}
	
	public void setDTX(boolean dtx){
		this.dtx = dtx;
	}
	
	public int getCBR(){
		if(this.cbr) return 1; return 0;
	}
	
	public void setCBR(boolean cbr){
		this.cbr = cbr;
	}

	void load() {
		try {
			System.loadLibrary("opus_jni");
			super.load();
		} catch (Throwable e) {
			if (!Sipdroid.release) e.printStackTrace();
		}
    
	}  
	
	public void init() {
		load();
		if (isLoaded())
			if(open(CODEC_SAMPLE_RATE, OPUS_MODES.get(currMode), getFrameSizeSamples(), maxBitRate, fec, dtx, cbr) != 0){
				fail();
			}
	}
	
	public void overrideNumber(int newNumber){
		CODEC_NUMBER_OVERRIDE = newNumber;
	}
	
	public void resetNumber(){
		CODEC_NUMBER_OVERRIDE = 0;
	}
	
	@Override
	public int number(){
		if(CODEC_NUMBER_OVERRIDE != 0){
			return CODEC_NUMBER_OVERRIDE;
		}
		return super.number();
	}
	
	public void close(){
		CODEC_NUMBER_OVERRIDE = 0;
		cleanup();
	}
	
	public static FrameSizeMs getFrameSize(int size){
		switch(size){
			case FRAME_SIZE_40 : return FrameSizeMs.FOURTY_MS;
			case FRAME_SIZE_60 : return FrameSizeMs.SIXTY_MS;
			default : return FrameSizeMs.TWENTY_MS;
		}
	}
	
	public static Mode getMode(int mode){
		switch(mode){
			case MODE_VOIP : return Mode.VOIP;
			case MODE_LOW_DELAY : return Mode.LOW_DELAY;
			default : return Mode.AUDIO;
		}	
	}
	
	public native int open(int samplerate, int mode, int framesize, int maxBitRate, boolean fec, boolean dtx, boolean cbr);
	public native int decode(byte encoded[], short lin[], int size);
	public native int encode(short lin[], int offset, byte encoded[], int size);
	public native void cleanup();
}
