package org.sipdroid.sipuapro.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/** This activity shows nothing; instead, it restarts the android process */
public class RestartActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.exit(0);
    }
    public static void doRestart(Activity anyActivity) {
        anyActivity.startActivity(new Intent(anyActivity.getApplicationContext(), RestartActivity.class));
    }
}