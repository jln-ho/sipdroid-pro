package org.sipdroid.sipua.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import org.sipdroid.codecs.AAC;
import org.sipdroid.codecs.Codec;
import org.sipdroid.codecs.Codecs;
import org.sipdroid.codecs.G711;
import org.sipdroid.codecs.G722;
import org.sipdroid.codecs.Opus;
import org.sipdroid.codecs.alaw;
import org.sipdroid.sipua.R;
import org.sipdroid.sipua.ui.Sipdroid.CallsAdapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.CallLog.Calls;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class PresetEditor extends Activity implements OnCodecSelectionListener {

	private AutoCompleteTextView txt_uri;
	private ListView list_presets;
	private Spinner sp_codecs;

	private static final String[] PROJECTION = new String[] { Calls._ID,
			Calls.NUMBER, Calls.CACHED_NAME };

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.presets);
		txt_uri  = (AutoCompleteTextView) findViewById(R.id.txt_uri);
		list_presets = (ListView) findViewById(R.id.list_presets);
		sp_codecs = (Spinner) findViewById(R.id.sp_codecs);
		
		ContentResolver content = getContentResolver();
	    Cursor cursor = content.query(Calls.CONTENT_URI, PROJECTION, Calls.NUMBER+" like ?", new String[] { "%@%" }, Calls.DEFAULT_SORT_ORDER);
	    CallsAdapter adapter = new CallsAdapter(this, cursor);
	    txt_uri.setAdapter(adapter);
	    txt_uri.addTextChangedListener(new TextWatcher(){

			@Override
			public void afterTextChanged(Editable s) {
				sp_codecs.setSelection(0);
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
	
	    });
	    
		final Activity listener = this;
	    sp_codecs.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected (AdapterView<?> parent, View view, int pos, long id){
				String selectedCodec = (String) parent.getItemAtPosition(pos);
				if(!selectedCodec.equals("-") && txt_uri.getText().length() == 0){
					parent.setSelection(0);
					Toast.makeText(listener, R.string.enter_uri_reminder, Toast.LENGTH_LONG).show();
					return;
				}
				if(selectedCodec.equals("G.711")){
					onCodecSelected(new alaw());
				}
				else if(selectedCodec.equals("G.722")){
					onCodecSelected(new G722());
				}
				else if(selectedCodec.equals("AAC")){
					new AACSettingsDialog(listener, (OnCodecSelectionListener) listener).show();
				}
				else if(selectedCodec.equals("Opus")){			
					new OpusSettingsDialog(listener, (OnCodecSelectionListener) listener).show();
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
				
			}
	    });
	    final Activity context = this;
	    list_presets.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View v, int arg2, long arg3) {
				AlertDialog.Builder alert = new AlertDialog.Builder(context);
				final String uri = ((TextView)v.findViewById(R.id.tv_preset_uri)).getText().toString();
				String msg = uri + "\n" + ((TextView)v.findViewById(R.id.tv_preset_description)).getText().toString();
				alert.setTitle(R.string.delete_preset_title).setMessage(getResources().getString(R.string.delete_preset_description) + msg);
				alert.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						SharedPreferences sharedPrefs = getSharedPreferences(Sipdroid.ADDITIONAL_PREFS, Context.MODE_PRIVATE);
						sharedPrefs.edit().remove("preset:"+uri).commit();
						updateList();
						Toast.makeText(context, R.string.delete_preset_sucess, Toast.LENGTH_LONG).show();
					}
				});
				alert.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						//do nothing
					}
				});
				alert.show();
			}
		});
	    updateList();
	}
	
	@Override
	public void onCodecSelected(final Codec c) {
		if(c == null){
			sp_codecs.setSelection(0);
			return;
		}
		if(txt_uri.getText().toString().length() == 0){
			Toast.makeText(this, R.string.enter_uri_reminder, Toast.LENGTH_LONG).show();
			return;
		}
		String msg = getResources().getString(R.string.codec_dialog_codec) + c.name() + "\n" + 
					 getResources().getString(R.string.codec_dialog_samplerate) + c.samp_rate() / 1000 + " kHz\n";
		if (c instanceof AAC) {
			msg += getResources().getString(R.string.codec_dialog_bitrate) + ((AAC) c).getBitrate() / 1000 + " kBit/s\n";
		}
		if (c instanceof Opus) {
			msg += getResources().getString(R.string.codec_dialog_profile) + ((Opus) c).getMode().toString();
		}
		final Activity context = this;
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle(R.string.create_preset_title).setMessage(getResources().getString(R.string.create_preset_description) + msg);
		alert.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				SharedPreferences sharedPrefs = getSharedPreferences(Sipdroid.ADDITIONAL_PREFS, Context.MODE_PRIVATE);
				sharedPrefs.edit().putString("preset:"+txt_uri.getText().toString(), c.getConfigString()).commit();
				updateList();
				((EditText)txt_uri).setText("");
				sp_codecs.setSelection(0);
				Toast.makeText(context, R.string.create_preset_sucess, Toast.LENGTH_LONG).show();
			}
		});
		alert.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				sp_codecs.setSelection(0);
			}
		});
		alert.show();
	}
	
	private void updateList(){
		// initialize preset list
	    List<Map<String,String>> listEntries = new ArrayList<Map<String,String>>();
	    SharedPreferences sharedPrefs = getSharedPreferences(Sipdroid.ADDITIONAL_PREFS, Context.MODE_PRIVATE);
		List<String> keyList = new LinkedList<String>();
		for(String key : sharedPrefs.getAll().keySet()){
			if(key.startsWith("preset:")){
				keyList.add(key);
			}
		}
		Collections.sort(keyList);
		for(String key : keyList){
			Codec c = Codecs.getCodecByConfig(sharedPrefs.getString(key, ""));
			if(c != null){
				Map<String,String> newEntry = new HashMap<String,String>();
				newEntry.put("uri", key.substring("preset:".length()));	
				newEntry.put("desc", c.getTitle());	
				listEntries.add(newEntry);
			}
		}
	    list_presets.setAdapter(
				new SimpleAdapter(
						this,
						listEntries,
						R.layout.preset_listitem, 
						new String[] {"uri", "desc"}, 
						new int[] {R.id.tv_preset_uri, R.id.tv_preset_description}));
	}
}
