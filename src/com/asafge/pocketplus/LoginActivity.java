package com.asafge.pocketplus;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.noinnion.android.reader.api.ReaderExtension;
import com.noinnion.android.reader.api.util.Utils;

public class LoginActivity extends Activity {

    public static final String NEWSPLUS_PACKAGE = "com.noinnion.android.newsplus";
    public final static int    REQUEST_OAUTH = 1;

    /*
	 * Authenticate or logout
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String action = getIntent().getAction();
		Context c = getApplicationContext();

		Log.e("Test", "LoginActivit.onCreate: " + action);
		if (action != null && action.equals(ReaderExtension.ACTION_LOGOUT)) {
			Prefs.setLoggedIn(c, false);
			Prefs.setSessionData(c, null);
			setResult(ReaderExtension.RESULT_LOGOUT);
			finish();
		}
		else if (Prefs.isLoggedIn(c)) {
			setResult(ReaderExtension.RESULT_LOGIN);
			finish();
		}
		else {
			Intent intent = new Intent(this, OAuthActivity.class);
            startActivityForResult(intent, REQUEST_OAUTH);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
        Utils.startAppPackage(this, WelcomeActivity.NEWSPLUS_PACKAGE);

		final Context c = getApplicationContext();
		if (Prefs.isLoggedIn(c)) {
			setResult(ReaderExtension.RESULT_LOGIN);
            Utils.startAppPackage(this, NEWSPLUS_PACKAGE);
			finish();
		}
	}
}
