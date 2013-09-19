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
import java.util.Hashtable;
import java.util.StringTokenizer;

import org.sipdroid.sipua.ui.Receiver;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;

class CodecBase implements Preference.OnPreferenceChangeListener {
	protected String CODEC_NAME;
	protected String CODEC_USER_NAME;
	protected int CODEC_NUMBER;
	protected int CODEC_SAMPLE_RATE=8000;		// default for most narrow band codecs
	protected int CODEC_FRAME_SIZE=160;		// default for most narrow band codecs
	protected String CODEC_DESCRIPTION;
	protected String CODEC_DEFAULT_SETTING = "always";

	private boolean loaded = false,failed = false;
	private boolean enabled = false;
	private boolean wlanOnly = false,wlanOr3GOnly = false;
	private String value;
	private boolean forced = false;
	
	protected Hashtable<String, String> KV = new Hashtable<String, String>();

	public void update() {
		if (value == null) {
			value = CODEC_DEFAULT_SETTING;
			updateFlags(value);
		}
		if (Receiver.mContext != null) {
			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext);
			if(!forced){
				value = sp.getString(key(), CODEC_DEFAULT_SETTING);
			}
			else{
				value = "always";
			}
			updateFlags(value);
		}
	}
	
	public void force(){
		value = "always";
		enable(true);
		forced = true;
		updateFlags(value);
	}
	
	public String getValue() {
		return value;
	}
	
	void load() {
		update();
		loaded = true;
	}
	
	public int samp_rate() {
		return CODEC_SAMPLE_RATE;
	}
	
	public int frame_size() {
		return CODEC_FRAME_SIZE;
	}

	public boolean isLoaded() {
		return loaded;
	}
    
	public boolean isFailed() {
		return failed;
	}
	
	public void fail() {
		update();
		failed = true;
	}
	
	public void enable(boolean e) {
		enabled = e;
	}

	public boolean isEnabled() {
		return enabled;
	}

	TelephonyManager tm;
	int nt;
	
	public boolean isValid() {
		if (!isEnabled())
			return false;
		if (Receiver.on_wlan)
			return true;
		if (wlanOnly())
			return false;
		if (tm == null) tm = (TelephonyManager) Receiver.mContext.getSystemService(Context.TELEPHONY_SERVICE);
		nt = tm.getNetworkType();
		if (wlanOr3GOnly() && nt < TelephonyManager.NETWORK_TYPE_UMTS)
			return false;
		if (nt < TelephonyManager.NETWORK_TYPE_EDGE)
			return false;
		return true;
	}
		
	private boolean wlanOnly() {
		return enabled && wlanOnly;
	}
	
	private boolean wlanOr3GOnly() {
		return enabled && wlanOr3GOnly;
	}

	public String name() {
		return CODEC_NAME;
	}

	public String key() {
		return CODEC_NAME+"_new";
	}
	
	public String userName() {
		return CODEC_USER_NAME;
	}

	public String getTitle() {
		return CODEC_NAME + " (" + CODEC_DESCRIPTION + ")";
	}

	public int number() {
		return CODEC_NUMBER;
	}

	public void setListPreference(ListPreference l) {
		l.setOnPreferenceChangeListener(this);
		l.setValue(value);
	}

	public boolean onPreferenceChange(Preference p, Object newValue) {
		ListPreference l = (ListPreference)p;
		value = (String)newValue;

		updateFlags(value);

		l.setValue(value);
		l.setSummary(l.getEntry());

		return true;
	}

	private void updateFlags(String v) {

		if (v.equals("never")) {
			enabled = false;
		} else {
			enabled = true;
			if (v.equals("wlan"))
				wlanOnly = true;
			else
				wlanOnly = false;
			if (v.equals("wlanor3g"))
				wlanOr3GOnly = true;
			else
				wlanOr3GOnly = false;
		}
	}

	public String toString() { 
		return "CODEC{ " + CODEC_NUMBER + ": " + getTitle() + "}";
	}
	
	public void configureFromString(String config) throws InvalidParameterException{
		KV.clear();
		StringTokenizer strTok = new StringTokenizer(config, ";");
		while (strTok.hasMoreTokens()) {
			String currToken = strTok.nextToken();
			if (currToken.contains(":")) {
				// divde tokens at the colon and put them into the hashmaps
				String key = currToken.substring(0, currToken.indexOf(":"));
				String value = currToken.substring(currToken.indexOf(":") + 1);
				KV.put(key, value);
			} 
		}
		if(KV.get("codec") == null || KV.get("number") == null || KV.get("samplerate") == null || KV.get("framesize") == null){
			throw new InvalidParameterException("Invalid config: " + config);
		}
		try{
			CODEC_USER_NAME = KV.get("codec");
			CODEC_FRAME_SIZE = Integer.parseInt(KV.get("framesize"));
			CODEC_SAMPLE_RATE= Integer.parseInt(KV.get("samplerate"));
			CODEC_NUMBER= Integer.parseInt(KV.get("number"));
		}
		catch(Exception e){
			throw new InvalidParameterException("Invalid config: " + config);
		}
	};
	
	public String getConfigString(){
		return "codec:"+userName()+";"+"number:"+number()+";"+"samplerate:"+samp_rate()+";"+"framesize:"+frame_size()+";";
	}
}
