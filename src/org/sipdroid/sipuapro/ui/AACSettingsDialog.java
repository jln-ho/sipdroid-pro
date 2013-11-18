package org.sipdroid.sipuapro.ui;

import java.security.InvalidParameterException;
import java.util.Hashtable;

import org.sipdroid.codecs.AAC;
import org.sipdroid.codecs.Codec;
import org.sipdroid.codecs.Codecs;
import org.sipdroid.media.RtpStreamSender;
import org.sipdroid.sipua.R;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

/**
 * A Dialog that enables the user to configure the AAC codec
 * @author Julian Howes
 */

public class AACSettingsDialog extends Dialog {
	private Spinner sp_aac_profile;
	private Spinner sp_aac_bitrate;
	private Spinner sp_aac_samprate;
	private CheckBox cb_eld_sbr;
	private TableRow tr_eld_sbr;
	
	public AACSettingsDialog(final Context context, final OnCodecSelectionListener listener) {
		super(context);
		setTitle(R.string.aac_codec_settings);
		setContentView(R.layout.aac_settings_dialog);
		
		// reference views
		sp_aac_profile = (Spinner) findViewById(R.id.sp_aac_profile);
		sp_aac_bitrate = (Spinner) findViewById(R.id.sp_aac_bitrate);
		sp_aac_samprate = (Spinner) findViewById(R.id.sp_aac_samprate);
		cb_eld_sbr = (CheckBox) findViewById(R.id.cb_eld_sbr);
		tr_eld_sbr = (TableRow) findViewById(R.id.tr_eld_sbr);
		
		// initialize views according to the current configuration
		setCurrentValues();
		
		// show the SBR checkbox only for AAC-ELD
		sp_aac_profile.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
				if(((TextView)view).getText().toString().equals("AAC-ELD")){
					tr_eld_sbr.setVisibility(View.VISIBLE);
				}
				else{
					tr_eld_sbr.setVisibility(View.GONE);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// do nothing
			}
		});
		
		// apply settings
		findViewById(R.id.btn_aac_apply).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(RtpStreamSender.isSupportedSampRate(getSelectedSampRate())){
					AAC aac;
					try{
						aac = new AAC(getSelectedProfile(), getSelectedSampRate(), getSelectedBitRate(), 
								tr_eld_sbr.getVisibility() == View.VISIBLE && cb_eld_sbr.isChecked());
					}
					catch (InvalidParameterException e){
						Toast.makeText(getContext(), R.string.codec_error_combination, Toast.LENGTH_SHORT).show();
						return;
					}
					aac.init();
					if(!aac.isFailed()){
						listener.onCodecSelected(aac);
						dismiss();
					}
					else{
						Toast.makeText(getContext(), R.string.codec_error_combination, Toast.LENGTH_SHORT).show();
					}
				}
				else{
					Toast.makeText(getContext(), R.string.codec_error_samplingrate, Toast.LENGTH_SHORT).show();
				}
			}
		});
		
		// close without applying settings
		findViewById(R.id.btn_aac_cancel).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				listener.onCodecSelected(null);
				dismiss();
			}
		});
	}
	
	private void setCurrentValues(){
		AAC aac = null;
		try{
			aac = (AAC) Codecs.get(96);
			if(aac == null) return;
		}
		catch(Exception e){
			return;
		}
		switch(aac.samp_rate()){
			case AAC.SAMPLING_RATE_44 : sp_aac_samprate.setSelection(1); break;
			case AAC.SAMPLING_RATE_32 : sp_aac_samprate.setSelection(2); break;
			default: sp_aac_samprate.setSelection(0);
		}
		switch(aac.getAOT()){
			case AAC.AOT_HE_AAC: sp_aac_profile.setSelection(1);  break;
			case AAC.AOT_AAC_LD: sp_aac_profile.setSelection(2); break;
			case AAC.AOT_AAC_ELD: sp_aac_profile.setSelection(3); 
					 if(aac.isEldSbr()){
						 tr_eld_sbr.setVisibility(View.VISIBLE);
						 cb_eld_sbr.setChecked(true);
					 }
					 else {
						 tr_eld_sbr.setVisibility(View.GONE);
						 cb_eld_sbr.setChecked(false);
					 }
					 break;
			default: sp_aac_profile.setSelection(0);
		}
		switch(aac.getBitrate()){
			case AAC.BITRATE_48: sp_aac_bitrate.setSelection(1); break;
			case AAC.BITRATE_32: sp_aac_bitrate.setSelection(2); break;
			case AAC.BITRATE_24: sp_aac_bitrate.setSelection(3); break;
			default: sp_aac_bitrate.setSelection(0);
		}
	}
	
	private int getSelectedSampRate(){
		String selected = (String)(sp_aac_samprate.getSelectedItem());
		if(selected.equals("48 kHz")) return AAC.SAMPLING_RATE_48;
		if(selected.equals("44.1 kHz")) return AAC.SAMPLING_RATE_44;
		return AAC.SAMPLING_RATE_32;
	}
	
	private int getSelectedBitRate(){
		String selected = (String)(sp_aac_bitrate.getSelectedItem());
		if(selected.equals("64 kbit/s")) return AAC.BITRATE_64;
		if(selected.equals("48 kbit/s")) return AAC.BITRATE_48;
		if(selected.equals("32 kbit/s")) return AAC.BITRATE_32;
		return AAC.BITRATE_24;
	}
	
	private int getSelectedProfile(){
		String selected = (String)(sp_aac_profile.getSelectedItem());
		if(selected.equals("AAC-LC")) return AAC.AOT_AAC_LC;
		if(selected.equals("HE-AAC")) return AAC.AOT_HE_AAC;
		if(selected.equals("AAC-LD")) return AAC.AOT_AAC_LD;
		return AAC.AOT_AAC_ELD;
	}
}
