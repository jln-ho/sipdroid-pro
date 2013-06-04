/*
 * Copyright (C) 2010 The Sipdroid Open Source Project
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
import java.util.StringTokenizer;
import java.util.Vector;

import org.sipdroid.sipua.R;
import org.sipdroid.sipua.ui.Receiver;
import org.sipdroid.sipua.ui.Settings;
import org.zoolu.sdp.MediaField;
import org.zoolu.sdp.SessionDescriptor;
import org.zoolu.sdp.AttributeField;

import android.content.Context;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class Codecs {
    	private static final Vector<Codec> codecs = new Vector<Codec>() {{
			add(new Opus());		//added by Julian Howes
			add(new AAC());
    		add(new G722());			
//			add(new SILK24());		save space (until a common library for all bitrates gets available?)
//			add(new SILK16());
//			add(new SILK8());
			add(new alaw());
			//add(new ulaw());
			//add(new Speex());
			//add(new GSM());
			//add(new BV16());
		}};
	private static final HashMap<Integer, Codec> codecsNumbers;
	private static final HashMap<String, Codec> codecsNames;

	static {
		final int size = codecs.size();
		codecsNumbers = new HashMap<Integer, Codec>(size);
		codecsNames = new HashMap<String, Codec>(size);

		for (Codec c : codecs) {
			codecsNames.put(c.name(), c);
			codecsNumbers.put(c.number(), c);
		}

		SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext);
		String prefs = sp.getString(Settings.PREF_CODECS, Settings.DEFAULT_CODECS);
		if (prefs == null) {
			String v = "";
			SharedPreferences.Editor e = sp.edit();

			for (Codec c : codecs)
				v = v + c.number() + " ";
			e.putString(Settings.PREF_CODECS, v);
			e.commit();
		} else {
			String[] vals = prefs.split(" ");
			for (String v: vals) {
				try {
					int i = Integer.parseInt(v);
					Codec c = codecsNumbers.get(i);
					/* moves the codec to the end
					 * of the list so we end up
					 * with the new codecs (if
					 * any) at the top and the
					 * remaining ones ordered
					 * according to the user */
					if (c != null) {
						codecs.remove(c);
						codecs.add(c);
					}
				} catch (Exception e) {
					// do nothing (expecting
					// NumberFormatException and
					// indexnot found
				}
			}
		}
	}
	
	// added by Julian Howes
	public static void put(Codec c) {
		if(c instanceof AAC || c instanceof Opus){
			Codec codecByName = codecsNames.get(c.name());
			if(codecByName != null){
				if(codecByName instanceof AAC){
					((AAC)codecByName).setProfile(((AAC)c).getConfig(), ((AAC) c).getBitrate());
				}
				if(codecByName instanceof Opus){
					((Opus)codecByName).setFrameSize(((Opus) c).getFrameSizeMs());
					((Opus)codecByName).setMode(((Opus) c).getMode());
					((Opus)codecByName).setSampleRate(((Opus) c).samp_rate());
				}
			}
			else{
				codecsNames.put(c.name(), c);
			}
			Codec codecByNumber = codecsNumbers.get(c.number());
			if(codecByNumber != null){
				if(codecByNumber instanceof AAC){
					((AAC)codecByNumber).setProfile(((AAC)c).getConfig(), ((AAC) c).getBitrate());
				}
				if(codecByNumber instanceof Opus){
					((Opus)codecByNumber).setFrameSize(((Opus) c).getFrameSizeMs());
					((Opus)codecByNumber).setMode(((Opus) c).getMode());
					((Opus)codecByNumber).setSampleRate(((Opus) c).samp_rate());
				}
			}
			else{
				codecsNumbers.put(c.number(), c);
			}
			boolean updated = false;
			for(Codec oldCodec : codecs){
				if(oldCodec instanceof AAC && c instanceof AAC){
					((AAC)oldCodec).setProfile(((AAC)c).getConfig(), ((AAC) c).getBitrate());
					updated = true;
					break;
				}
				if(oldCodec instanceof Opus && c instanceof Opus){
					((Opus)oldCodec).setFrameSize(((Opus) c).getFrameSizeMs());
					((Opus)oldCodec).setMode(((Opus) c).getMode());
					((Opus)oldCodec).setSampleRate(((Opus) c).samp_rate());
					updated = true;
					break;
				}
			}
			if(!updated){
				codecs.add(c);
			}
		}
		else{
			if(codecsNames.get(c.name()) != null){
				codecsNames.remove(c.name());
			}
			if(codecsNumbers.get(c.number()) != null){
				codecsNumbers.remove(c.number());
			}
			codecsNames.put(c.name(), c);
			codecsNumbers.put(c.number(), c);
			boolean updated = false;
			for(Codec oldCodec : codecs){
				if(oldCodec.name().equals(c.name()) && oldCodec.number() == c.number()){
					int index = codecs.indexOf(oldCodec);
					codecs.remove(index);
					codecs.add(index, c);
					updated = true;
					break;
				}
			}
			if(!updated){
				codecs.add(c);
			}
		}
	}

	public static Codec get(int key) {
		return codecsNumbers.get(key);
	}

	public static Codec getName(String name) {
		return codecsNames.get(name);
	}

	public static void check() {
		HashMap<String, String> old = new HashMap<String, String>(codecs.size());

		for(Codec c : codecs) {
			c.update();
			old.put(c.name(), c.getValue());
			if (!c.isLoaded()) {
				SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext);
				SharedPreferences.Editor e = sp.edit();

				e.putString(c.key(), "never");
				e.commit();
			}
		}
		
		for(Codec c : codecs)
			if (!old.get(c.name()).equals("never")) {
				c.init();
				if (c.isLoaded()) {
					SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext);
					SharedPreferences.Editor e = sp.edit();
	
					e.putString(c.key(), old.get(c.name()));
					e.commit();
					c.init();
				} else
					c.fail();
			}
	}
	
	private static void addPreferences(PreferenceScreen ps) {
		Context cx = ps.getContext();
		Resources r = cx.getResources();
		ps.setOrderingAsAdded(true);

		for(Codec c : codecs) {
			ListPreference l = new ListPreference(cx);
			l.setEntries(r.getStringArray(R.array.compression_display_values));
			l.setEntryValues(r.getStringArray(R.array.compression_values));
			l.setKey(c.key());
			l.setPersistent(true);
			l.setEnabled(!c.isFailed());
			c.setListPreference(l);
			if (c.number() == 9)
				if (ps.getSharedPreferences().getString(Settings.PREF_SERVER, Settings.DEFAULT_SERVER).equals(Settings.DEFAULT_SERVER))
					l.setSummary(l.getEntry()+" ("+r.getString(R.string.settings_improve2)+")");
				else
					l.setSummary(l.getEntry()+" ("+r.getString(R.string.settings_hdvoice)+")");
			else
				l.setSummary(l.getEntry());
			l.setTitle(c.getTitle());
			ps.addPreference(l);
		}
	}

	public static int[] getCodecs() {
		Vector<Integer> v = new Vector<Integer>(codecs.size());

		for (Codec c : codecs) {
			c.update();
			if (!c.isValid())
				continue;
			v.add(c.number());
		}
		int i[] = new int[v.size()];
		for (int j = 0; j < i.length; j++)
			i[j] = v.elementAt(j);
		return i;
	}

	public static class Map {
		public int number;
		public Codec codec;
		Vector<Integer> numbers;
		Vector<Codec> codecs;

		Map(int n, Codec c, Vector<Integer> ns, Vector<Codec> cs) {
			number = n;
			codec = c;
			numbers = ns;
			codecs = cs;
		}

		public boolean change(int n) {
			int i = numbers.indexOf(n);
			
			if (i >= 0 && codecs.elementAt(i) != null) {
				codec.close();
				number = n;
				codec = codecs.elementAt(i);
				return true;
			}
			return false;
		}
		
		public String toString() {
			return "Codecs.Map { " + number + ": " + codec + "}";
		}
	};

	public static Map getCodec(SessionDescriptor offers) {
		MediaField m = offers.getMediaDescriptor("audio").getMedia(); 
		if (m==null) 
			return null;

		String proto = m.getTransport();
		//see http://tools.ietf.org/html/rfc4566#page-22, paragraph 5.14, <fmt> description 
		if ( proto.equals("RTP/AVP") || proto.equals("RTP/SAVP") ) {
			Vector<String> formats = m.getFormatList();
			Vector<String> names = new Vector<String>(formats.size());
			Vector<Integer> numbers = new Vector<Integer>(formats.size());
			Vector<Codec> codecmap = new Vector<Codec>(formats.size());

			//add all avail formats with empty names
			for (String fmt : formats) {
				try {
					int number = Integer.parseInt(fmt);
					numbers.add(number);
					names.add("");
					codecmap.add(null);
				} catch (NumberFormatException e) {
					// continue ... remote sent bogus rtp setting
				}
			};
		
			//if we have attrs for format -> set name
			Vector<AttributeField> attrs = offers.getMediaDescriptor("audio").getAttributes("rtpmap");			
			for (AttributeField a : attrs) {
				String s = a.getValue();
				// skip over "rtpmap:"
				s = s.substring(7, s.indexOf("/"));
				int i = s.indexOf(" ");
				try {
					String name = s.substring(i + 1);
					int number = Integer.parseInt(s.substring(0, i));
					int index = numbers.indexOf(number);
					if (index >=0)
						names.set(index, name.toLowerCase());
				} catch (NumberFormatException e) {
					// continue ... remote sent bogus rtp setting
				}
			}
			
			Codec codec = null;
			int index = formats.size() + 1;
			
			for (Codec c : codecs) {
				c.update();
				if (!c.isValid())
					continue;

				//search current codec in offers by name
				int i = names.indexOf(c.userName().toLowerCase());
				if (i >= 0) {
					codecmap.set(i, c);
					if ( (codec==null) || (i < index) ) {
						//added by Julian Howes
						if(c instanceof AAC){
							Vector<AttributeField> fmtpAttrs = offers.getMediaDescriptor("audio").getAttributes("fmtp");
							for(AttributeField attr : fmtpAttrs){
								//check mpeg-4 specific parameters
								String fmtpLine = attr.getValue().toLowerCase();
								fmtpLine = fmtpLine.replace(" ", "").replace("fmtp:96", "");
								if(!fmtpLine.contains("streamtype") || !fmtpLine.contains("profile-level-id")
								  || !fmtpLine.contains("mode") || !fmtpLine.contains("config")
								  || !fmtpLine.contains("sizelength") || !fmtpLine.contains("indexlength")
								  || !fmtpLine.contains("indexdeltalength") || !fmtpLine.contains("constantduration")
								  || !fmtpLine.contains("bitrate")){
									continue;
								}
								StringTokenizer tokenizer = new StringTokenizer(fmtpLine, ";");
								boolean validParams = true;
								int bitRate = 0;
								AAC aacCodec = null;
								while(tokenizer.hasMoreTokens()){
									String currToken = tokenizer.nextToken();
									String[] param = currToken.split("\\=");
									if(param.length == 2){
										if(param[0].equals("streamtype")){
											if(!param[1].equals("5")){
												validParams = false;
												break;
											}
										}
										else if(param[0].equals("mode")){
											if(!param[1].equals("aac-hbr")){
												validParams = false;
												break;
											}
										}
										else if(param[0].equals("config")){
											if(!AAC.isSupportedProfile(param[1])){
												validParams = false;
												break;
											}
											else{
												aacCodec = new AAC(param[1]);
											}
										}
										else if(param[0].equals("sizelength")){
											if(!param[1].equals("13")){
												validParams = false;
												break;
											}
										}
										else if(param[0].equals("indexlength")){
											if(!param[1].equals("3")){
												validParams = false;
												break;
											}
										}
										else if(param[0].equals("indexdeltalength")){
											if(!param[1].equals("3")){
												validParams = false;
												break;
											}
										}
										else if(param[0].equals("bitrate")){
											try{
												bitRate = Integer.parseInt(param[1]);
											}
											catch(Exception e){
												validParams = false;
												break;
											}
										}
									}
									else{
										validParams = false;
										break;
									}
								}
								if(validParams && aacCodec != null && bitRate > 0){
									aacCodec.setBitrate(bitRate);
									aacCodec.init();
									if(aacCodec.isFailed()) continue;
									Codecs.put(aacCodec);
									c = Codecs.get(96);
								}
								else{
									continue;
								}
							}
						}
						else if(c instanceof Opus){
							if(numbers.get(i) != c.number()){
								((Opus)c).overrideNumber(numbers.get(i));
								Log.d("OPUS OVERRIDE", "changed opus number to " + c.number());
							}
							boolean validParams = true;
							int maxPtime = 120;
							Opus opusCodec = new Opus((Opus)c);
							Vector<AttributeField> attrFields = offers.getMediaDescriptor("audio").getAttributes("maxptime");
							for(AttributeField attr : attrFields){
								String attrLine = attr.getValue().toLowerCase();
								String maxPtimeStr = attrLine.replace(" ", "").replace("maxptime:", "");
								try{
									maxPtime = Integer.parseInt(maxPtimeStr);
									if(maxPtime < Opus.getMinFrameSizeMs()){
										Log.d("SDP offer", "offered maxptime " + maxPtimeStr + " is less than local minptime "+ Opus.getMinFrameSizeMs());
										validParams = false;
									}
								}
								catch(NumberFormatException e){
									Log.d("SDP offer", "Invalid maxptime value + " + maxPtimeStr + " in attribute line: " + attrLine);
									validParams = false;
									e.printStackTrace();
								}
							}
							attrFields = offers.getMediaDescriptor("audio").getAttributes("ptime");
							for(AttributeField attr : attrFields){
								String attrLine = attr.getValue().toLowerCase();
								String ptimeStr = attrLine.replace(" ", "").replace("ptime:", "");
								try{
									int ptime = Integer.parseInt(ptimeStr);
									if (ptime <= maxPtime){
										if(!opusCodec.setFrameSize(ptime)){
											Log.d("SDP offer", "Unsupported ptime value " + ptimeStr + " in attribute line: " + attrLine); 
											validParams = false;
										}
									}
									else{
										Log.d("SDP offer", "ptime value " + ptimeStr + " was ignored since it was greater than maxptime value " + maxPtime);
										if(!opusCodec.setFrameSize(maxPtime)){
											Log.d("SDP offer", "Unsupported maxptime value " + maxPtime + " in attribute line: " + attrLine); 
											validParams = false;
										}
									}
								}
								catch (NumberFormatException e){
									Log.d("SDP offer", "Invalid ptime value + " + ptimeStr + " in attribute line: " + attrLine);
									validParams = false;
									e.printStackTrace();
								}
							}
							attrFields = offers.getMediaDescriptor("audio").getAttributes("fmtp");
							for(AttributeField attr : attrFields){
								//check mpeg-4 specific parameters
								String fmtpLine = attr.getValue().toLowerCase();
								fmtpLine = fmtpLine.replace(" ", "").replace("fmtp:107", "");
								StringTokenizer tokenizer = new StringTokenizer(fmtpLine, ";");
								while(tokenizer.hasMoreTokens()){
									String currToken = tokenizer.nextToken();
									String[] param = currToken.split("\\=");
									if(param.length == 2){
										if(param[0].equals("maxplaybackrate")){
											try{
												int maxRate = Integer.parseInt(param[1]);
												if(opusCodec.samp_rate() > maxRate){
													switch(maxRate){
														case Opus.SAMPLING_RATE_16 :
														case Opus.SAMPLING_RATE_8  : opusCodec.setSampleRate(maxRate); break;
														case Opus.SAMPLING_RATE_24 : opusCodec.setSampleRate(Opus.SAMPLING_RATE_16); break;
														case Opus.SAMPLING_RATE_12 : opusCodec.setSampleRate(Opus.SAMPLING_RATE_8); break;
														default: opusCodec.setSampleRate(Opus.SAMPLING_RATE_8); break;
													}
												}
											}
											catch(NumberFormatException e){
												Log.d("SDP offer", "Invalid maxplaybackrate value + " + param[1] + " in attribute line: " + fmtpLine);
												validParams = false;
												e.printStackTrace();
											}
										}
										else if(param[0].equals("minptime")){
											try{
												int minPtime = Integer.parseInt(param[1]);
												if(minPtime > Opus.getMaxFrameSizeMs()){
													Log.d("SDP offer", "offered minptime " + param[1] + " is greater than local maxptime "+ Opus.getMaxFrameSizeMs());
													validParams = false;
												}
												if(opusCodec.getFrameSizeMsInt() < minPtime){
													if(!opusCodec.setFrameSize(minPtime)){
														Log.d("SDP offer", "unsupported minptime value + "+ param[1] + " in attribute line: " + fmtpLine);
														validParams = false;
													}
												}
											} catch (NumberFormatException e) {
												Log.d("SDP offer", "Invalid maxptime value + "+ param[1] + " in attribute line: " + fmtpLine);
												validParams = false;
												e.printStackTrace();
											}
										} 
										else if (param[0].equals("maxaveragebitrate")) {
											try{
												int maxBrate = Integer.parseInt(param[1]);
												if(maxBrate >= 6000 && maxBrate <= 510000){
													opusCodec.setMaxBitRate(maxBrate);
												}
											} catch (NumberFormatException e) {
												Log.d("SDP offer", "Invalid maxaveragebitrate value + "+ param[1] + " in attribute line: " + fmtpLine);
												validParams = false;
												e.printStackTrace();
											}
										} 

										else if (param[0].equals("cbr")) {
											if(param[1].equals("1")){
												opusCodec.setCBR(true);
											}
											else{
												opusCodec.setCBR(false);
											}
										} 
										else if (param[0].equals("useinbandfec")) {
											if(param[1].equals("1")){
												opusCodec.setFEC(true);
											}
											else{
												opusCodec.setFEC(false);
											}
										} 
//										else if (param[0].equals("usedtx")) {
//										
//										}
//										else if(param[0].equals("sprop-maxcapturerate")){
//											
//										}
//										else if (param[0].equals("stereo")) {
//
//										} 
//										else if (param[0].equals("sprop-stereo")) {
//
//										} 
									}
								}
							}
							if(validParams){
								opusCodec.init();
								if(opusCodec.isFailed()) continue;
								Codecs.put(opusCodec);
								c = Codecs.get(107);
							}
							else{
								continue;
							}
						}
						codec = c;
						codecmap.set(i, c);
						index = i;
						continue;
					}
				}
				
				//search current codec in offers by number
				i = numbers.indexOf(c.number());
				if (i >= 0) {
						if ( names.elementAt(i).equals("")) {
							codecmap.set(i, c);
							if ( (codec==null) || (i < index) )  {
								//fmt number has no attr with name 
								codec = c;
								index = i;
								continue;
							}
						}
				}
			}			
			if (codec!=null) 
				return new Map(numbers.elementAt(index), codec, numbers, codecmap);
			else
				// no codec found ... we can't talk
				return null;
		} else
			/*formats of other protocols not supported yet*/
			return null;
	}

	public static class CodecSettings extends PreferenceActivity {

		private static final int MENU_UP = 0;
		private static final int MENU_DOWN = 1;

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);

			addPreferencesFromResource(R.xml.codec_settings);

			// for long-press gesture on a profile preference
			registerForContextMenu(getListView());

			addPreferences(getPreferenceScreen());
		}

		@Override
		public void onCreateContextMenu(ContextMenu menu, View v,
						ContextMenuInfo menuInfo) {
			super.onCreateContextMenu(menu, v, menuInfo);

			menu.setHeaderTitle(R.string.codecs_move);
			menu.add(Menu.NONE, MENU_UP, 0,
				 R.string.codecs_move_up);
			menu.add(Menu.NONE, MENU_DOWN, 0,
				 R.string.codecs_move_down);
		}

		@Override
		public boolean onContextItemSelected(MenuItem item) {

			int posn = (int)((AdapterContextMenuInfo)item.getMenuInfo()).position;
			Codec c = codecs.elementAt(posn);
			if (item.getItemId() == MENU_UP) {
				if (posn == 0)
					return super.onContextItemSelected(item);
				Codec tmp = codecs.elementAt(posn - 1);
				codecs.set(posn - 1, c);
				codecs.set(posn, tmp);
			} else if (item.getItemId() == MENU_DOWN) {
				if (posn == codecs.size() - 1)
					return super.onContextItemSelected(item);
				Codec tmp = codecs.elementAt(posn + 1);
				codecs.set(posn + 1, c);
				codecs.set(posn, tmp);
			}
			PreferenceScreen ps = getPreferenceScreen();
			SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(Receiver.mContext);
			String v = "";
			SharedPreferences.Editor e = sp.edit();

			for (Codec d : codecs)
				v = v + d.number() + " ";
			e.putString(Settings.PREF_CODECS, v);
			e.commit();
			ps.removeAll();
			addPreferences(ps);
			return super.onContextItemSelected(item);
		}

		@Override
		public boolean onPreferenceTreeClick(PreferenceScreen ps, Preference p) {
			ListPreference l = (ListPreference) p;
			for (Codec c : codecs)
				if (c.key().equals(l.getKey())) {
					c.init();
					if (!c.isLoaded()) {
						l.setValue("never");
						c.fail();
						l.setEnabled(false);
						l.setSummary(l.getEntry());
						if (l.getDialog() != null)
							l.getDialog().dismiss();
					}
				}
			return super.onPreferenceTreeClick(ps,p);
		}
		
		@Override
		public void onDestroy() {
			super.onDestroy();
			unregisterForContextMenu(getListView());
		}
	}
}
