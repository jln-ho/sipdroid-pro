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
import java.security.InvalidParameterException;
import java.text.DecimalFormat;
import java.util.HashMap;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
/**
 * Class representing the AAC audio codec family
 * @author Julian Howes
 */
public class AAC extends CodecBase implements Codec {
	
	public static final int AOT_AAC_LC = 2;
	public static final int AOT_HE_AAC = 5;
	public static final int AOT_AAC_LD = 23;
	public static final int AOT_AAC_ELD = 39;
	
	public static final int SAMPLING_RATE_48 = 48000;
	public static final int SAMPLING_RATE_44 = 44100;
	public static final int SAMPLING_RATE_32 = 32000;
	
	public static final int BITRATE_64 = 64000;
	public static final int BITRATE_48 = 48000;
	public static final int BITRATE_32 = 32000;
	public static final int BITRATE_24 = 24000;
	
	/**
	 * Nested class describing a configuration for a single AAC profile  
	 * @author Julian Howes
	 */
	private static class Profile{
		private int sampleRate;
		private int aot;
		private int frameSize;
		private boolean eldSBR = false;
		private String name;
		
		public Profile(int sampleRate, int aot){
			this.sampleRate = sampleRate;
			this.aot = aot;
			switch(this.aot){
				case AOT_AAC_LD: this.frameSize = 512;  this.name = "AAC-LD";  break;
				case AOT_AAC_ELD: this.frameSize = 512;  this.name = "AAC-ELD"; break;
				case AOT_HE_AAC : this.frameSize = 2048; this.name = "HE-AAC"; break;
				default: this.frameSize = 1024; this.name = "AAC-LC";
			}
		}
		
		public Profile(int sampleRate, int aot, boolean eldSBR){
			this(sampleRate, aot);
			this.eldSBR = eldSBR;
			if(this.aot == AOT_AAC_ELD && eldSBR){
				this.frameSize = 1024;
			}
		}
	}
	
	/**
	 * Associates all supported profile configurations to their corresponding ASC
	 */
	private static final HashMap<String, Profile> supportedProfiles = new HashMap<String, Profile>();
	static{
		// AAC-LC
		supportedProfiles.put("1188", new AAC.Profile(48000, AOT_AAC_LC));
		supportedProfiles.put("1208", new AAC.Profile(44100, AOT_AAC_LC));
		supportedProfiles.put("1288", new AAC.Profile(32000, AOT_AAC_LC));
		
		// HE-AAC
		supportedProfiles.put("2B098800", new AAC.Profile(48000, AOT_HE_AAC));
		supportedProfiles.put("2B8A0800", new AAC.Profile(44100, AOT_HE_AAC));
		supportedProfiles.put("2C0A8800", new AAC.Profile(32000, AOT_HE_AAC));
		
		// AAC-LD
		supportedProfiles.put("B98900", new AAC.Profile(48000, AOT_AAC_LD));
		supportedProfiles.put("BA0900", new AAC.Profile(44100, AOT_AAC_LD));
		supportedProfiles.put("BA8900", new AAC.Profile(32000, AOT_AAC_LD));
		
		// AAC-ELD (with SBR)
		supportedProfiles.put("F8EC21ACE000", new AAC.Profile(48000, AOT_AAC_ELD, true));
		supportedProfiles.put("F8EE21AF0000", new AAC.Profile(44100, AOT_AAC_ELD, true));
		
		// AAC-ELD (without SBR)
		supportedProfiles.put("F8E62000", new AAC.Profile(48000, AOT_AAC_ELD));
		supportedProfiles.put("F8E82000", new AAC.Profile(44100, AOT_AAC_ELD));
		supportedProfiles.put("F8EA2000", new AAC.Profile(32000, AOT_AAC_ELD));
	}

	private Profile currentProfile;
	private int CODEC_BITRATE = 64000; //default bitrate for all profiles
	private String CODEC_CONFIG;

	/**
	 * A private constructur that sets default values for CODEC_USER_NAME, CODEC_NUMBER and CODEC_DEFAULT_SETTING
	 * These values are the same for each configuration
	 * @param dummy a dummy parameter to distinguish this constructor from the default constructor
	 */
	private AAC(int dummy){
		this.CODEC_USER_NAME = "mpeg4-generic";
		this.CODEC_NUMBER = 96;
		this.CODEC_DEFAULT_SETTING = "always";
		super.update();
	}
	
	/**
	 * Creates an AAC profile with default values for AAC-LC @ 64 kbit/s, 48 kHz
	 * or the values set in shared preferences
	 */
	public AAC(){
		this(0);
		setProfile("1188");
	}
	
	/**
	 * Creates an AAC profile based on the given hexadecimal ASC string with a default bitrate of 64 kbit/s
	 * @param config the hexadecimal ASC, e.g. "1188" for AAC-LC @ 48 kHz
	 * @throws InvalidParameterException if the provided ASC is invalid or not supported
	 */
	public AAC(String config) throws InvalidParameterException {
		this(0);
		setProfile(config);
	}
	
	/**
	 * Creates an AAC profile based on the given hexadecimal ASC string with the specified bitrate
	 * @param config the hexadecimal ASC, e.g. "1188" for AAC-LC @ 48 kHz
	 * @param bitRate the bitrate in bit/s
	 * @throws InvalidParameterException if the provided ASC is invalid or not supported
	 */
	public AAC(String config, int bitRate) throws InvalidParameterException {
		this(0);
		setProfile(config, bitRate);
	} 
	
	/**
	 * Creates an AAC profile based on the given Audio Object Type, sampling rate and bitrate
	 * @param aot The Audio Object Type number for the profile
	 * @param sampleRate the sampling rate in Hz
	 * @param bitRate the bitrate in bit/s
	 * @param eldSBR flag for enabling or disabling Spectral Band Replication (SBR) for aot 39 (AAC-ELD)
	 * @throws InvalidParameterException if the combination of given parameters describes an unsupported configuration
	 */
	public AAC(int aot, int sampleRate, int bitRate, boolean eldSBR) throws InvalidParameterException {
		this(0);
		String matchingConfig = null;
		for(String currKey : supportedProfiles.keySet()){
			Profile currProfile = supportedProfiles.get(currKey);
			if(currProfile.aot == aot && currProfile.sampleRate == sampleRate && currProfile.eldSBR == eldSBR){
				matchingConfig = currKey;
				break;
			}
		}
		if(matchingConfig == null) throw new InvalidParameterException("Configuration not supported");
		setProfile(matchingConfig);
		setBitrate(bitRate);
	}
	
	/**
	 * Checks if the provided ASC is supported
	 * @param config a hexadecimal ASC
	 * @return true if supported, else false
	 */
	public static boolean isSupportedProfile(String config){
		return supportedProfiles.get(config.toUpperCase()) != null;
	}

	/**
	 * Sets the AAC profile according to the provided ASC using the default bitrate
	 * @param config a hexadecimal ASC
	 * @throws InvalidParameterException if the ASC is not supported
	 */
	public void setProfile(String config) throws InvalidParameterException{
		if(!isSupportedProfile(config)) throw new InvalidParameterException("Configuration not supported");
		this.CODEC_CONFIG = config.toUpperCase();
		this.currentProfile = supportedProfiles.get(CODEC_CONFIG);
		this.CODEC_NAME = currentProfile.name;
		this.CODEC_SAMPLE_RATE = currentProfile.sampleRate;
		this.CODEC_FRAME_SIZE = currentProfile.frameSize;
		updateDescription();
	}
	
	/**
	 * Sets the AAC profile according to the provided ASC and applied the provided bitrate
	 * @param config a hexadecimal ASC
	 * @throws InvalidParameterException if the ASC is not supported
	 */
	public void setProfile(String config, int bitRate) throws InvalidParameterException{
		CODEC_BITRATE = bitRate;
		setProfile(config);
	}
	
	/**
	 * Initializes the AAC encoder and decoder according to the specified parameters. <br>
	 * Native method, implemented in "aac_jni.cpp"
	 * @param brate the bitrate for the encoder
	 * @param sample_rate the sample rate for the encoder
	 * @param aot the audio object type for the encoder
	 * @param codec_config the ASC for the decoder
	 * @param codec_config_length the length in bytes of the ASC
	 * @param eldSBR flag for enabling or disabling Spectral Band Replication (SBR) for aot 39 (AAC-ELD)
	 * @return 0 in case of success, else -1
	 */
	private native int open(int brate, int sample_rate, int aot, byte[] codec_config, int codec_config_length, boolean eldSBR);
	
	//Native methods, implemented in "aac_jni.cpp"
	@Override
	public native int decode(byte[] encoded, short[] lin, int size);
	@Override
	public native int encode(short[] lin, int offset, byte[] encoded, int size);
	@Override
	public native void close();
	
	@Override
	public void load() {
		try {
			System.loadLibrary("FraunhoferAAC");
			System.loadLibrary("aac_jni");
			super.load();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}  
	
	@Override
	public void init() {
		load();
		if (isLoaded()){
			byte[] configBytes = getConfigBytes();
			if(open(CODEC_BITRATE, CODEC_SAMPLE_RATE, currentProfile.aot, configBytes, configBytes.length, currentProfile.eldSBR) != 0){
				fail();
			}
		}
	}
	
	public String getConfig(){
		return CODEC_CONFIG;
	}
	
	/**
	 * @return a byte array containing the current ASC
	 */
	private byte[] getConfigBytes(){
		int len = CODEC_CONFIG.length();
	    byte[] bytes = new byte[len/2];
	    for (int i = 0; i < len; i += 2) {
	    	bytes[i/2] = (byte) ((Character.digit(CODEC_CONFIG.charAt(i), 16) << 4)
	                             + Character.digit(CODEC_CONFIG.charAt(i+1), 16));
	    }
	    return bytes;
	}
	
	public int getBitrate(){
		return CODEC_BITRATE;
	}
	
	public void setBitrate(int brate){
		CODEC_BITRATE = brate;
		updateDescription();
	}
	
	public int getAOT(){
		return currentProfile.aot;
	}
	
	public boolean isEldSbr(){
		return currentProfile.eldSBR;
	}
	
	private void updateDescription(){
		DecimalFormat f;
		if(CODEC_SAMPLE_RATE % 1000 != 0) f = new DecimalFormat("#0.0");
		else f = new DecimalFormat("#0");
		CODEC_DESCRIPTION = CODEC_BITRATE / 1000 + " kbit/s | "+f.format(CODEC_SAMPLE_RATE/1000.0)+" kHz";
	}
}
