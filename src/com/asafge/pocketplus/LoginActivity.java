package com.asafge.pocketplus;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

import com.noinnion.android.reader.api.ReaderException;
import com.noinnion.android.reader.api.ReaderExtension;
import com.asafge.pocketplus.Prefs;

public class LoginActivity extends Activity {
	
	protected ProgressDialog mBusy;
	
	/* 
	 * Initialize the authentication process
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String action = getIntent().getAction();
		Context c = getApplicationContext();
		
		if (action != null && action.equals(ReaderExtension.ACTION_LOGOUT)) {
			Prefs.setLoggedIn(c, false);
			Prefs.setSessionData(c, null);
			setResult(ReaderExtension.RESULT_LOGOUT);
			finish();
		}
		if (Prefs.isLoggedIn(c)) {
			setResult(ReaderExtension.RESULT_LOGIN);
			finish();
		}
		if (Prefs.getSessionData(c) == null) {
			new GetRequestToken().execute(APICall.API_OAUTH_CONSUMER_KEY, APICall.API_OAUTH_REDIRECT);
			finish();
		}
	}
	
	/* 
	 * Callback from Pocket API application approve page
	 */
	@Override
	protected void onResume() {
		Context c = getApplicationContext();
		try {
			super.onResume();
			new GetAccessToken().execute(APICall.API_OAUTH_CONSUMER_KEY, Prefs.getSessionData(c).getString("code"));
			finish();
		}
		catch (JSONException e) {
			Prefs.setSessionData(c, null);
		}
	}
	
	/*
	 * OAuth step 1 - get request token for Pocket+, for this user.
	 */
	private class GetRequestToken extends AsyncTask<String, Void, Boolean> {
       
        // Get request token from Pocket API
		protected Boolean doInBackground(String... params) {
			Context c = getApplicationContext();
			APICall ac = new APICall(APICall.API_URL_OAUTH_REQUEST_TOKEN, c);
			ac.addPostParam("consumer_key", params[0]);
			ac.addPostParam("redirect_uri", params[1]);
			
			try {
				ac.sync();
				Prefs.setSessionData(c, ac.Json);
				setResult(ReaderExtension.RESULT_OK);
				return true;
			}
			catch (ReaderException e) {
				Prefs.setLoggedIn(c, false);
				return false;
			}
		}
		
		// Save request token code
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			try {
				Context c = getApplicationContext();
				JSONObject json = Prefs.getSessionData(c);
				if (json != null) {
					Uri.Builder b = Uri.parse(APICall.API_URL_OAUTH_AUTHORIZE_APP).buildUpon();			
					b.appendQueryParameter("request_token", json.getString("code"));
					b.appendQueryParameter("redirect_uri", APICall.API_OAUTH_REDIRECT);
					Intent launchBrowser = new Intent(Intent.ACTION_VIEW, b.build());
					startActivity(launchBrowser);
				}
			}
			catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	/*
	 * OAuth step 2 - upgrade to access token
	 */
    private class GetAccessToken extends AsyncTask<String, Void, Boolean> {

        // Show the login... process dialog
        protected void onPreExecute() {
            mBusy = ProgressDialog.show(LoginActivity.this, null, getText(R.string.msg_login_running), true, true);
        }

        // Get access token from Pocket API
        protected Boolean doInBackground(String... params) {
        	Context c = getApplicationContext();
            APICall ac = new APICall(APICall.API_URL_OAUTH_ACCESS_TOKEN, c);
            ac.addPostParam("consumer_key", params[0]);
            ac.addPostParam("code", params[1]);
			try {
				ac.sync();
				Prefs.setSessionData(c, ac.Json);
				Prefs.setLoggedIn(c, true);
				setResult(ReaderExtension.RESULT_LOGIN);
				return true;
			}
			catch (ReaderException e) {
				Prefs.setSessionData(c, null);
				Prefs.setLoggedIn(c, false);
				return false;
			}
        }

        // On callback - show toast if failed / go to main screen
        protected void onPostExecute(Boolean result) {
            /*Context c = getApplicationContext();
            if (mBusy != null && mBusy.isShowing())
                mBusy.dismiss();
            if (result)
                finish();
            else
                Toast.makeText(c, getText(R.string.msg_login_fail), Toast.LENGTH_LONG).show();
                */
        }
    }
}
