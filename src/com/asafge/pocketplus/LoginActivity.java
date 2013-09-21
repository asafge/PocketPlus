package com.asafge.pocketplus;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import com.noinnion.android.reader.api.ReaderException;
import com.noinnion.android.reader.api.ReaderExtension;
import com.asafge.pocketplus.Prefs;

public class LoginActivity extends Activity {

	protected ProgressDialog mBusy;

	/* 
	 * Authenticate or logout
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
		else if (Prefs.isLoggedIn(c)) {
			setResult(ReaderExtension.RESULT_LOGIN);
			finish();
		}
		else if (Prefs.getSessionData(c) == null) {
			new GetRequestToken().execute();
			finish();
		}
	}

	/* 
	 * Callback from Pocket API application approve page
	 */
	@Override
	protected void onResume() {
		super.onResume();
		new GetAccessToken().execute();
		finish();
	}

	/*
	 * OAuth step 1 - get request token for Pocket+, for this user.
	 */
	private class GetRequestToken extends AsyncTask<Void, Void, Boolean> {
       
        // Get request token from Pocket API
		protected Boolean doInBackground(Void... params) {
			final Context c = getApplicationContext();
			try {
				APICall ac = new APICall(APICall.API_URL_OAUTH_REQUEST_TOKEN, c);
				ac.addPostParam("consumer_key", APICall.API_OAUTH_CONSUMER_KEY);
				ac.addPostParam("redirect_uri", APICall.API_OAUTH_REDIRECT);
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
					b.appendQueryParameter("request_token", URLEncoder.encode(json.getString("code"), "UTF-8"));
					b.appendQueryParameter("redirect_uri", URLEncoder.encode(APICall.API_OAUTH_REDIRECT, "UTF-8"));
					Intent launchBrowser = new Intent(Intent.ACTION_VIEW, b.build());
					startActivity(launchBrowser);
				}
			}
			catch (JSONException e) {
				// TODO
			}
			catch (UnsupportedEncodingException e) {
				// TODO
			}
		}
	}

	/*
	 * OAuth step 2 - upgrade to access token
	 */
    private class GetAccessToken extends AsyncTask<Void, Void, Boolean> {

        // Show the login... process dialog
        protected void onPreExecute() {
            mBusy = ProgressDialog.show(LoginActivity.this, null, getText(R.string.msg_login_running), true, true);
        }

        // Get access token from Pocket API
        protected Boolean doInBackground(Void... params) {
        	final Context c = getApplicationContext();
			try {
				APICall ac = new APICall(APICall.API_URL_OAUTH_ACCESS_TOKEN, c);
	            ac.addPostParam("consumer_key", APICall.API_OAUTH_CONSUMER_KEY);
	            ac.addPostParam("code", Prefs.getSessionData(c).getString("code"));
				ac.sync();
				
				Prefs.setSessionData(c, ac.Json);
				Prefs.setLoggedIn(c, true);
				setResult(ReaderExtension.RESULT_LOGIN);
				return true;
			}
			catch (JSONException e) {
				// TODO
				return false;
			}
			catch (ReaderException e) {
				Prefs.setSessionData(c, null);
				Prefs.setLoggedIn(c, false);
				return false;
			}
        }

        // On callback - show toast if failed / go to main screen
        protected void onPostExecute(Boolean result) {
            /*final Context c = getApplicationContext();
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