package org.sipdroid.sipua.ui;

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

import java.util.HashMap;

import org.sipdroid.media.RtpStreamReceiver;
import org.sipdroid.media.RtpStreamSender;
import org.sipdroid.sipua.R;
import org.sipdroid.sipua.UserAgent;
import org.sipdroid.sipua.phone.Call;
import org.sipdroid.sipua.phone.CallCard;
import org.sipdroid.sipua.phone.Phone;
import org.sipdroid.sipua.phone.SlidingCardManager;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;

public class InCallScreen extends CallScreen implements View.OnClickListener, SensorEventListener {

	protected static final int REQUEST_CODE_PICK_FILE_OR_DIRECTORY = 1;
	final int MSG_ANSWER = 1;
	final int MSG_ANSWER_SPEAKER = 2;
	final int MSG_BACK = 3;
	final int MSG_TICK = 4;
	
	final int SCREEN_OFF_TIMEOUT = 12000;
	
	CallCard mCallCard;
	Phone ccPhone;
	int oldtimeout;
	SensorManager sensorManager;
    Sensor proximitySensor;
    boolean first;

    static Button playBtn;
    static Button stopBtn;
    private static boolean playing = false;
    final int MSG_PLAY = 5;
    final int MSG_STOP = 6;
    
	public static void setPlaying(boolean playing) {
		InCallScreen.playing = playing;
	}

	public static boolean isPlaying() {
		return playing;
	}

	void screenOff(boolean off) {
        ContentResolver cr = getContentResolver();
        
        if (proximitySensor != null)
        	return;
        if (off) {
        	if (oldtimeout == 0) {
        		oldtimeout = Settings.System.getInt(cr, Settings.System.SCREEN_OFF_TIMEOUT, 60000);
	        	Settings.System.putInt(cr, Settings.System.SCREEN_OFF_TIMEOUT, SCREEN_OFF_TIMEOUT);
        	}
        } else {
        	if (oldtimeout == 0 && Settings.System.getInt(cr, Settings.System.SCREEN_OFF_TIMEOUT, 60000) == SCREEN_OFF_TIMEOUT)
        		oldtimeout = 60000;
        	if (oldtimeout != 0) {
	        	Settings.System.putInt(cr, Settings.System.SCREEN_OFF_TIMEOUT, oldtimeout);
        		oldtimeout = 0;
        	}
        }
	}
	
	@Override
	public void onStop() {
		super.onStop();
		mHandler.removeMessages(MSG_BACK);
		if (Receiver.call_state == UserAgent.UA_STATE_IDLE)
			finish();
		sensorManager.unregisterListener(this);
		started = false;
	}
	
	@Override
	public void onStart() {
		super.onStart();
		if (Receiver.call_state == UserAgent.UA_STATE_IDLE)
     		mHandler.sendEmptyMessageDelayed(MSG_BACK, Receiver.call_end_reason == -1?
    				2000:5000);
	    first = true;
	    sensorManager.registerListener(this,proximitySensor,SensorManager.SENSOR_DELAY_NORMAL);
	    started = true;
	}

	@Override
	public void onPause() {
		super.onPause();
    	if (!Sipdroid.release) Log.i("SipUA:","on pause");
    	switch (Receiver.call_state) {
    	case UserAgent.UA_STATE_INCOMING_CALL:
    		if (!RtpStreamReceiver.isBluetoothAvailable()) Receiver.moveTop();
    		break;
    	case UserAgent.UA_STATE_IDLE:
    		if (Receiver.ccCall != null)
    			mCallCard.displayMainCallStatus(ccPhone,Receiver.ccCall);
     		mHandler.sendEmptyMessageDelayed(MSG_BACK, Receiver.call_end_reason == -1?
    				2000:5000);
    		break;
    	}
		if (t != null) {
			running = false;
			t.interrupt();
		}
		screenOff(false);
		if (mCallCard.mElapsedTime != null) mCallCard.mElapsedTime.stop();
	}
	
	void moveBack() {
		if (Receiver.ccConn != null && !Receiver.ccConn.isIncoming()) {
			RestartActivity.doRestart(this);
		}
		onStop();
	}
	
	Context mContext = this;

	@Override
	public void onResume() {
		super.onResume();
    	if (!Sipdroid.release) Log.i("SipUA:","on resume");
		switch (Receiver.call_state) {
		case UserAgent.UA_STATE_INCOMING_CALL:
			if (Receiver.pstn_state == null || Receiver.pstn_state.equals("IDLE"))
				if (PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(org.sipdroid.sipua.ui.Settings.PREF_AUTO_ON, org.sipdroid.sipua.ui.Settings.DEFAULT_AUTO_ON) &&
						!mKeyguardManager.inKeyguardRestrictedInputMode())
					mHandler.sendEmptyMessageDelayed(MSG_ANSWER, 1000);
				else if ((PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(org.sipdroid.sipua.ui.Settings.PREF_AUTO_ONDEMAND, org.sipdroid.sipua.ui.Settings.DEFAULT_AUTO_ONDEMAND) &&
						PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(org.sipdroid.sipua.ui.Settings.PREF_AUTO_DEMAND, org.sipdroid.sipua.ui.Settings.DEFAULT_AUTO_DEMAND)) ||
						(PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean(org.sipdroid.sipua.ui.Settings.PREF_AUTO_HEADSET, org.sipdroid.sipua.ui.Settings.DEFAULT_AUTO_HEADSET) &&
								Receiver.headset > 0))
					mHandler.sendEmptyMessageDelayed(MSG_ANSWER_SPEAKER, 10000);
			break;
		case UserAgent.UA_STATE_INCALL:
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
				mDialerDrawer.close();
				mDialerDrawer.setVisibility(View.GONE);
			} else
				mDialerDrawer.setVisibility(View.VISIBLE);
			if (Receiver.docked <= 0)
				screenOff(true);
			break;
		case UserAgent.UA_STATE_IDLE:
			if (!mHandler.hasMessages(MSG_BACK))
				moveBack();
			break;
		}
		if (Receiver.call_state != UserAgent.UA_STATE_INCALL) {
			mDialerDrawer.close();
			mDialerDrawer.setVisibility(View.GONE);
		}
		if (Receiver.ccCall != null) mCallCard.displayMainCallStatus(ccPhone,Receiver.ccCall);
        if (mSlidingCardManager != null) mSlidingCardManager.showPopup();
		mHandler.sendEmptyMessage(MSG_TICK);
	    if (t == null && Receiver.call_state != UserAgent.UA_STATE_IDLE) {
			mDigits.setText("");
			running = true;
	        (t = new Thread() {
				public void run() {
					int len = 0;
					long time;
					ToneGenerator tg = null;
	
					if (Settings.System.getInt(getContentResolver(),
							Settings.System.DTMF_TONE_WHEN_DIALING, 1) == 1)
						tg = new ToneGenerator(AudioManager.STREAM_VOICE_CALL, (int)(ToneGenerator.MAX_VOLUME*2*org.sipdroid.sipua.ui.Settings.getEarGain()));
					for (;;) {
						if (!running) {
							t = null;
							break;
						}
						if (isPlaying()) {
							mHandler.sendEmptyMessage(MSG_PLAY);
						} else {
							mHandler.sendEmptyMessage(MSG_STOP);
						}
						if (len != mDigits.getText().length()) {
							time = SystemClock.elapsedRealtime();
							if (tg != null) tg.startTone(mToneMap.get(mDigits.getText().charAt(len)));
							Receiver.engine(Receiver.mContext).info(mDigits.getText().charAt(len++),250);
							time = 250-(SystemClock.elapsedRealtime()-time);
							try {
								if (time > 0) sleep(time);
							} catch (InterruptedException e) {
							}
							if (tg != null) tg.stopTone();
							try {
								if (running) sleep(250);
							} catch (InterruptedException e) {
							}
							continue;
						}
						mHandler.sendEmptyMessage(MSG_TICK);
						try {
							sleep(1000);
						} catch (InterruptedException e) {
						}
					}
					if (tg != null) tg.release();
				}
			}).start();
	    }
	}
	
    Handler mHandler = new Handler() {
    	public void handleMessage(Message msg) {
    		switch (msg.what) {
    		
    		case MSG_PLAY:
    			if (stopBtn.getVisibility() != Button.VISIBLE) {
    				showStopButton();
    			}
    			break;
    		case MSG_STOP:
    			if (playBtn.getVisibility() != Button.VISIBLE) {
    				showPlayButton();
//        			muteMic(false);
    			}
    			break;
    		case MSG_ANSWER:
        		if (Receiver.call_state == UserAgent.UA_STATE_INCOMING_CALL)
        			answer();
        		break;
    		case MSG_ANSWER_SPEAKER:
        		if (Receiver.call_state == UserAgent.UA_STATE_INCOMING_CALL) {
        			answer();
    				Receiver.engine(mContext).speaker(AudioManager.MODE_NORMAL);
        		}
        		break;
    		case MSG_BACK:
    			moveBack();
    			break;
    		case MSG_TICK:
    			mCodec.setText(RtpStreamReceiver.getCodec());
    			if (RtpStreamReceiver.good != 0) {
    				if (RtpStreamReceiver.timeout != 0)
    					mStats.setText("no data");
    				else if (RtpStreamSender.m == 2)
	    				mStats.setText(Math.round(RtpStreamReceiver.loss/RtpStreamReceiver.good*100)+"%loss, "+
	    						Math.round(RtpStreamReceiver.lost/RtpStreamReceiver.good*100)+"%lost, "+
	    						Math.round(RtpStreamReceiver.late/RtpStreamReceiver.good*100)+"%late (>"+
	    						(RtpStreamReceiver.jitter-250*RtpStreamReceiver.mu)/8/RtpStreamReceiver.mu+"ms)");
    				else
	    				mStats.setText(Math.round(RtpStreamReceiver.lost/RtpStreamReceiver.good*100)+"%lost, "+
	    						Math.round(RtpStreamReceiver.late/RtpStreamReceiver.good*100)+"%late (>"+
	    						(RtpStreamReceiver.jitter-250*RtpStreamReceiver.mu)/8/RtpStreamReceiver.mu+"ms)");
    				mStats.setVisibility(View.VISIBLE);
    			} else
    				mStats.setVisibility(View.GONE);
    			break;
    		}
    	}
    };

	ViewGroup mInCallPanel,mMainFrame;
	SlidingDrawer mDialerDrawer;
	public static SlidingCardManager mSlidingCardManager;
	TextView mStats;
	TextView mCodec;
	
	protected EditText mEditText;
	
    public void initInCallScreen() {
        mInCallPanel = (ViewGroup) findViewById(R.id.inCallPanel);
        mMainFrame = (ViewGroup) findViewById(R.id.mainFrame);
        View callCardLayout = getLayoutInflater().inflate(
                    R.layout.call_card_popup,
                    mInCallPanel);
        mCallCard = (CallCard) callCardLayout.findViewById(R.id.callCard);
        mCallCard.reset();

        mSlidingCardManager = new SlidingCardManager();
        mSlidingCardManager.init(ccPhone, this, mMainFrame);
        SlidingCardManager.WindowAttachNotifierView wanv =
            new SlidingCardManager.WindowAttachNotifierView(this);
	    wanv.setSlidingCardManager(mSlidingCardManager);
	    wanv.setVisibility(View.GONE);
	    RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(0, 0);
	    mMainFrame.addView(wanv, lp);

	    mStats = (TextView) findViewById(R.id.stats);
	    mCodec = (TextView) findViewById(R.id.codec);
        mDialerDrawer = (SlidingDrawer) findViewById(R.id.dialer_container);
        mCallCard.displayOnHoldCallStatus(ccPhone,null);
        mCallCard.displayOngoingCallStatus(ccPhone,null);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
        	mCallCard.updateForLandscapeMode();
        
        // Have the WindowManager filter out touch events that are "too fat".
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES);

	    mDigits = (EditText) findViewById(R.id.digits);
        mDisplayMap.put(R.id.one, '1');
        mDisplayMap.put(R.id.two, '2');
        mDisplayMap.put(R.id.three, '3');
        mDisplayMap.put(R.id.four, '4');
        mDisplayMap.put(R.id.five, '5');
        mDisplayMap.put(R.id.six, '6');
        mDisplayMap.put(R.id.seven, '7');
        mDisplayMap.put(R.id.eight, '8');
        mDisplayMap.put(R.id.nine, '9');
        mDisplayMap.put(R.id.zero, '0');
        mDisplayMap.put(R.id.pound, '#');
        mDisplayMap.put(R.id.star, '*');
        
        mToneMap.put('1', ToneGenerator.TONE_DTMF_1);
        mToneMap.put('2', ToneGenerator.TONE_DTMF_2);
        mToneMap.put('3', ToneGenerator.TONE_DTMF_3);
        mToneMap.put('4', ToneGenerator.TONE_DTMF_4);
        mToneMap.put('5', ToneGenerator.TONE_DTMF_5);
        mToneMap.put('6', ToneGenerator.TONE_DTMF_6);
        mToneMap.put('7', ToneGenerator.TONE_DTMF_7);
        mToneMap.put('8', ToneGenerator.TONE_DTMF_8);
        mToneMap.put('9', ToneGenerator.TONE_DTMF_9);
        mToneMap.put('0', ToneGenerator.TONE_DTMF_0);
        mToneMap.put('#', ToneGenerator.TONE_DTMF_P);
        mToneMap.put('*', ToneGenerator.TONE_DTMF_S);

        View button;
        for (int viewId : mDisplayMap.keySet()) {
            button = findViewById(viewId);
            button.setOnClickListener(this);
        }
        
        mEditText = (EditText) findViewById(R.id.file_path);
	    SeekBar volumeControl = (SeekBar) findViewById(R.id.mix_seek);
	    SeekBar wiretap = (SeekBar) findViewById(R.id.wiretap_seek);
	    CheckBox checkBoxMic = (CheckBox) findViewById(R.id.mute_mic);
	    CheckBox checkBoxWiretap = (CheckBox) findViewById(R.id.mute_wiretap);
	    final CheckBox checkBoxDuplication = (CheckBox) findViewById(R.id.packet_duplication);
	    final CheckBox checkBoxInterleaving = (CheckBox) findViewById(R.id.interleaving);
		playBtn = (Button) findViewById(R.id.play_button);
		stopBtn = (Button) findViewById(R.id.stop_button);
        
        Button btn = (Button) findViewById(R.id.browse_button);
	    btn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				pickFile();
			}
	    });

	    playBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				playMP3();
			}
	    });
	    
	    stopBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				stopMP3();
			}
	    });

	    btn = (Button) findViewById(R.id.showFileControls_button);
	    btn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				LinearLayout dialer = (LinearLayout) findViewById(R.id.dialer);
				dialer.setVisibility(LinearLayout.INVISIBLE);
				LinearLayout fileControls = (LinearLayout) findViewById(R.id.fileControls);
				fileControls.setVisibility(LinearLayout.VISIBLE);
			}
	    });
	    
	    btn = (Button) findViewById(R.id.hideFileControls_button);
	    btn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				LinearLayout dialer = (LinearLayout) findViewById(R.id.dialer);
				dialer.setVisibility(LinearLayout.VISIBLE);
				LinearLayout fileControls = (LinearLayout) findViewById(R.id.fileControls);
				fileControls.setVisibility(LinearLayout.INVISIBLE);
			}
	    });
	    
	    checkBoxInterleaving.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
	    	@Override
	    	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    			RtpStreamSender.setInterleaving(isChecked);
    			SharedPreferences sharedPrefs = getSharedPreferences(Sipdroid.ADDITIONAL_PREFS, MODE_PRIVATE);
    			sharedPrefs.edit().putBoolean(Sipdroid.ADDITIONAL_PREF_PACKET_INTERLEAVING, isChecked).commit();
	    	}
	    });
	   
	    checkBoxDuplication.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
	    	@Override
	    	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    			RtpStreamSender.setPacketDuplication(isChecked);
    			if(checkBoxInterleaving.isChecked() && !isChecked) checkBoxInterleaving.setChecked(false);
    			checkBoxInterleaving.setEnabled(isChecked);
    			SharedPreferences sharedPrefs = getSharedPreferences(Sipdroid.ADDITIONAL_PREFS, MODE_PRIVATE);
    			sharedPrefs.edit().putBoolean(Sipdroid.ADDITIONAL_PREF_PACKET_DUPLICATION, isChecked).commit();
	    	}
	    });
	 	// initialize checkboxes
	    SharedPreferences sharedPrefs = getSharedPreferences(Sipdroid.ADDITIONAL_PREFS, MODE_PRIVATE);
	    
	    checkBoxDuplication.setChecked(sharedPrefs.getBoolean(Sipdroid.ADDITIONAL_PREF_PACKET_DUPLICATION, false));
	    RtpStreamSender.setPacketDuplication(checkBoxDuplication.isChecked());
	    
	    checkBoxInterleaving.setChecked(sharedPrefs.getBoolean(Sipdroid.ADDITIONAL_PREF_PACKET_INTERLEAVING, false));
	    checkBoxInterleaving.setEnabled(checkBoxDuplication.isChecked());
	    RtpStreamSender.setInterleaving(checkBoxInterleaving.isEnabled() && checkBoxInterleaving.isChecked());
	    
	    checkBoxMic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
	    	
	    	@Override
	    	public void onCheckedChanged(CompoundButton buttonView,	boolean isChecked) {
    			RtpStreamSender.setMuteMic(isChecked); 
	    	}
	    });
	    
	    volumeControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

    	   @Override
    	   public void onProgressChanged(SeekBar seekBar, int progress,
    	     boolean fromUser) {
    		   RtpStreamSender.setRatio((float) ((float) progress / 100));
    	   }

    	   @Override
    	   public void onStartTrackingTouch(SeekBar volumeControl) {
    	   }

    	   @Override
    	   public void onStopTrackingTouch(SeekBar volumeControl) {
    	   }
       });

	    
	    checkBoxWiretap.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
	    	
	    	@Override
	    	public void onCheckedChanged(CompoundButton buttonView,	boolean isChecked) {
    			RtpStreamReceiver.setMuteWiretap(isChecked);
	    	}
	    });
	    
	    wiretap.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

    	   @Override
    	   public void onProgressChanged(SeekBar seekBar, int progress,
    	     boolean fromUser) {
    		   RtpStreamReceiver.setRatio((float) ((float) progress / 100));
    	   }

    	   @Override
    	   public void onStartTrackingTouch(SeekBar wiretap) {
    	   }

    	   @Override
    	   public void onStopTrackingTouch(SeekBar wiretap) {
    	   }
       });
	    
    }
    
	Thread t;
	EditText mDigits;
	boolean running;
	public static boolean started;
    private static final HashMap<Integer, Character> mDisplayMap =
        new HashMap<Integer, Character>();
    private static final HashMap<Character, Integer> mToneMap =
        new HashMap<Character, Integer>();
    
	public void onClick(View v) {
        int viewId = v.getId();

        // if the button is recognized
        if (mDisplayMap.containsKey(viewId)) {
                    appendDigit(mDisplayMap.get(viewId));
        }
    }

    void appendDigit(final char c) {
        mDigits.getText().append(c);
    }

    @Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.incall);
		
		initInCallScreen();
		
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        if(!android.os.Build.BRAND.equalsIgnoreCase("archos"))
        	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
    }
		
	public void reject() {
		if (Receiver.ccCall != null) {
			Receiver.stopRingtone();
			Receiver.ccCall.setState(Call.State.DISCONNECTED);
			mCallCard.displayMainCallStatus(ccPhone,Receiver.ccCall);
			mDialerDrawer.close();
			mDialerDrawer.setVisibility(View.GONE);
	        if (mSlidingCardManager != null)
	        	mSlidingCardManager.showPopup();
		}
        (new Thread() {
			public void run() {
        		Receiver.engine(mContext).rejectcall();
			}
		}).start();   	
    }
	
	public void answer() {
        (new Thread() {
			public void run() {
				Receiver.engine(mContext).answercall();
			}
		}).start();   
		if (Receiver.ccCall != null) {
			Receiver.ccCall.setState(Call.State.ACTIVE);
			Receiver.ccCall.base = SystemClock.elapsedRealtime();
			mCallCard.displayMainCallStatus(ccPhone,Receiver.ccCall);
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
				mDialerDrawer.close();
				mDialerDrawer.setVisibility(View.GONE);
			} else {
				mDialerDrawer.setVisibility(View.VISIBLE);
			}
	        if (mSlidingCardManager != null)
	        	mSlidingCardManager.showPopup();
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
        case KeyEvent.KEYCODE_MENU:
        	if (Receiver.call_state == UserAgent.UA_STATE_INCOMING_CALL && mSlidingCardManager == null) {
        		answer();
				return true;
        	}
        	break;
        
        case KeyEvent.KEYCODE_CALL:
        	switch (Receiver.call_state) {
        	case UserAgent.UA_STATE_INCOMING_CALL:
        		answer();
        		break;
        	case UserAgent.UA_STATE_INCALL:
        	case UserAgent.UA_STATE_HOLD:
       			Receiver.engine(this).togglehold();
       			break;
        	}
            // consume KEYCODE_CALL so PhoneWindow doesn't do anything with it
            return true;

        case KeyEvent.KEYCODE_BACK:
        	if (mDialerDrawer.isOpened())
        		mDialerDrawer.animateClose();
        	else if (Receiver.call_state == UserAgent.UA_STATE_INCOMING_CALL)
        		reject();      
            return true;

        case KeyEvent.KEYCODE_CAMERA:
            // Disable the CAMERA button while in-call since it's too
            // easy to press accidentally.
        	return true;
        	
        case KeyEvent.KEYCODE_VOLUME_DOWN:
        case KeyEvent.KEYCODE_VOLUME_UP:
        	if (Receiver.call_state == UserAgent.UA_STATE_INCOMING_CALL) {
        		Receiver.stopRingtone();
        		return true;
        	}
        	RtpStreamReceiver.adjust(keyCode,true);
        	return true;
        }
        if (Receiver.call_state == UserAgent.UA_STATE_INCALL) {
	        char number = event.getNumber();
	        if (Character.isDigit(number) || number == '*' || number == '#') {
	        	appendDigit(number);
	        	return true;
	        }
        }
        return super.onKeyDown(keyCode, event);
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {
        case KeyEvent.KEYCODE_VOLUME_DOWN:
        case KeyEvent.KEYCODE_VOLUME_UP:
        	RtpStreamReceiver.adjust(keyCode,false);
        	return true;
        case KeyEvent.KEYCODE_ENDCALL:
        	if (Receiver.pstn_state == null ||
				(Receiver.pstn_state.equals("IDLE") && (SystemClock.elapsedRealtime()-Receiver.pstn_time) > 3000)) {
        			reject();      
        			return true;		
        	}
        	break;
		}
		Receiver.pstn_time = 0;
		return false;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	void setScreenBacklight(float a) {
        WindowManager.LayoutParams lp = getWindow().getAttributes(); 
        lp.screenBrightness = a; 
        getWindow().setAttributes(lp);		
	}

	static final float PROXIMITY_THRESHOLD = 5.0f;
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		if (first) {
			first = false;
			return;
		}
		float distance = event.values[0];
        boolean active = (distance >= 0.0 && distance < PROXIMITY_THRESHOLD && distance < event.sensor.getMaximumRange());
        setScreenBacklight((float) (active?0.1:-1));
	}
	
    
    private void pickFile() {
		 Intent intentBrowseFiles = new Intent(Intent.ACTION_GET_CONTENT);
         intentBrowseFiles.setType("audio/mpeg");
//         intentBrowseFiles.setType("audio/x-wav");
         intentBrowseFiles.addCategory(Intent.CATEGORY_OPENABLE);
         startActivityForResult(Intent.createChooser(intentBrowseFiles, getString(R.string.open_title)), REQUEST_CODE_PICK_FILE_OR_DIRECTORY);
    }
    
	private void playMP3 () {
	    String file = mEditText.getText().toString();
	    if (!file.equals("")) {
	    	RtpStreamSender.initFile(file);
	    	String error = RtpStreamSender.getError(); 
	    	if (error.equals("No error... (code 0)")) {
	    		RtpStreamSender.setAudioPlay(true);
		    	showStopButton();
		    	setPlaying(true);
			    CheckBox checkBox = (CheckBox) findViewById(R.id.mute_mic);
				checkBox.setChecked(true);
	    	} 
		    else {
	    		AlertDialog.Builder alert = new AlertDialog.Builder(this);
	
	    		alert.setTitle("This file is not supported").setMessage(error);
	    		
	    		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
	    		public void onClick(DialogInterface dialog, int whichButton) {
	    			
	    		  }
	    		});
	
	    		alert.show();
		    }
	    } else {
	    	Toast.makeText(this, R.string.no_file_selected, Toast.LENGTH_SHORT).show();
	    	pickFile();
	    }
	}
	
	private void stopMP3() {
		setPlaying(false);
		showPlayButton();
//		muteMic(false);
		RtpStreamSender.stopAndCleanup();
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_CODE_PICK_FILE_OR_DIRECTORY) {
			if (resultCode == RESULT_OK && data != null) {
				
				String selectedPath;
			    String filename;
			    
				Uri selectedUri = data.getData();

	            // external file manager (astro, andexplorer)
                filename = selectedUri.getPath();

                // media player
                selectedPath = getPath(selectedUri);

                if (selectedPath != null) {
                	filename = selectedPath;
                }
				
				if (filename != null) {
					// Get rid of URI prefix:
					if (filename.startsWith("file://")) {
						filename = filename.substring(7);
					}
					if (filename.toLowerCase().endsWith(".mp3") || filename.toLowerCase().endsWith(".wav") || filename.toLowerCase().endsWith(".aac") || filename.toLowerCase().endsWith(".m4a")) {
						if(filename.toLowerCase().endsWith(".aac")){
							
						}
						mEditText.setText(filename);
					} else {
						Toast.makeText(this, R.string.only_mp3, Toast.LENGTH_SHORT).show();
					}
				}							
			}
		}
	}
	
	public String getPath(Uri uri) {
		String retval = null;
        String[] projection = { 
        		MediaStore.Images.Media.DATA
        };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        if(cursor!=null) {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            retval = cursor.getString(columnIndex);
        }
        return retval;
    }
	
	public void showStopButton() {
		stopBtn.setVisibility(Button.VISIBLE);
		playBtn.setVisibility(Button.GONE);
	}

	public void showPlayButton() {
  		playBtn.setVisibility(Button.VISIBLE);
		stopBtn.setVisibility(Button.GONE);
	}
	
	public void muteMic(boolean mute) {
	    CheckBox checkBox = (CheckBox) findViewById(R.id.mute_mic);
		checkBox.setChecked(mute);
	}
	
}
