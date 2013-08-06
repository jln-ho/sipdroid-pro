package org.sipdroid.sipua.ui;

import org.sipdroid.codecs.Codecs;
import org.sipdroid.codecs.Opus;
import org.sipdroid.codecs.Opus.FrameSizeMs;
import org.sipdroid.media.RtpStreamSender;
import org.sipdroid.sipua.R;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * A Dialog that enables the user to configure the Opus codec
 * @author Julian Howes
 */
public class OpusSettingsDialog extends Dialog {
	
	private Spinner sp_opus_profile;
	private Spinner sp_opus_framelength;
	private Spinner sp_opus_samprate;
	 
	public OpusSettingsDialog(final Context context, final OnCodecSelectionListener listener) {
		super(context);
		setTitle(R.string.opus_codec_settings);
		setContentView(R.layout.opus_settings_dialog);
		
		// reference views
		sp_opus_profile = (Spinner) findViewById(R.id.sp_opus_profile);
		sp_opus_framelength = (Spinner) findViewById(R.id.sp_opus_framelength);
		sp_opus_samprate = (Spinner) findViewById(R.id.sp_opus_samprate);
		 
		// initialize views according to the current configuration
		setCurrentValues();
		
		// apply settings
		findViewById(R.id.btn_aac_apply).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(RtpStreamSender.isSupportedSampRate(getSelectedSampRate())){
					Opus opus = (Opus) Codecs.get(107);
					if(opus != null){
						opus.setFrameSize(getSelectedFrameLength());
						opus.setMode(getSelectedProfile());
						opus.setSampleRate(getSelectedSampRate());
					}
					else{
						opus = new Opus(getSelectedProfile(), getSelectedFrameLength(), getSelectedSampRate());
					}
					opus.init();
					if(!opus.isFailed()){
						listener.onCodecSelected(opus);
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
		Opus opus = null;
		try{
			opus = (Opus) Codecs.get(107);
			if(opus == null) return;
		}
		catch(Exception e){
			return;
		}
		switch(opus.samp_rate()){
			case Opus.SAMPLING_RATE_24 : sp_opus_samprate.setSelection(1); break;
			case Opus.SAMPLING_RATE_16 : sp_opus_samprate.setSelection(2); break;
			case Opus.SAMPLING_RATE_12 : sp_opus_samprate.setSelection(3); break;
			case Opus.SAMPLING_RATE_8 : sp_opus_samprate.setSelection(4); break;
			default: sp_opus_samprate.setSelection(0);
		}
		switch(opus.getFrameSizeMs()){
			case FOURTY_MS: sp_opus_framelength.setSelection(1);  break;
			case TWENTY_MS: sp_opus_framelength.setSelection(0);  break;
			default: sp_opus_framelength.setSelection(2);
		}
		switch(opus.getMode()){
			case VOIP: sp_opus_profile.setSelection(1); break;
			case LOW_DELAY: sp_opus_profile.setSelection(2); break;
			default: sp_opus_profile.setSelection(0);
		}
	}
	
	private int getSelectedSampRate(){
		String selected = (String)(sp_opus_samprate.getSelectedItem());
		if(selected.equals("48 kHz")) return Opus.SAMPLING_RATE_48;
		if(selected.equals("24 kHz")) return Opus.SAMPLING_RATE_24;
		if(selected.equals("16 kHz")) return Opus.SAMPLING_RATE_16;
		if(selected.equals("12 kHz")) return Opus.SAMPLING_RATE_12;
		return Opus.SAMPLING_RATE_8;
	}
	
	private FrameSizeMs getSelectedFrameLength(){
		String selected = (String)(sp_opus_framelength.getSelectedItem());
		if(selected.equals("40 ms")) return Opus.FrameSizeMs.FOURTY_MS;
		if(selected.equals("60 ms")) return Opus.FrameSizeMs.SIXTY_MS;
		return Opus.FrameSizeMs.TWENTY_MS;
	}
	
	private Opus.Mode getSelectedProfile(){
		String selected = (String)(sp_opus_profile.getSelectedItem());
		if(selected.equals("VOIP")) return Opus.Mode.VOIP;
		if(selected.equals("LOW DELAY")) return Opus.Mode.LOW_DELAY;
		return Opus.Mode.AUDIO;
	}
}
