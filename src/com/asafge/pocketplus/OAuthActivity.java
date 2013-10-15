package com.asafge.pocketplus;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.noinnion.android.reader.api.ReaderException;

public class OAuthActivity extends Activity {

	/* 
	 * Authenticate or logout
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Context c = getApplicationContext();
		setResult(RESULT_CANCELED);
		if (Prefs.getSessionData(c) == null) {
			new GetRequestToken().execute();
		}
		else {
			new GetAccessToken().execute();
		}
	}

	@Override
	protected void onNewIntent (Intent intent) {
		new GetAccessToken().execute();	
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
				return true;
			}
			catch (ReaderException e) {
				Prefs.setSessionData(c, null);
				return false;
			}
		}

		// Save request token code
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			final Context c = getApplicationContext();
			try {
				JSONObject json = Prefs.getSessionData(c);
				if (json != null) {
					Uri.Builder b = Uri.parse(APICall.API_URL_OAUTH_AUTHORIZE_APP).buildUpon();			
					b.appendQueryParameter("request_token", json.getString("code"));
					b.appendQueryParameter("redirect_uri", APICall.API_OAUTH_REDIRECT);
					
					StringBuilder builder = new StringBuilder();
					builder.append(APICall.API_URL_OAUTH_AUTHORIZE_APP).append("?request_token=").append(json.getString("code")).append("&redirect_uri=").append(APICall.API_OAUTH_REDIRECT);
					
					Log.e("Test", "request url: " + builder.toString());
					Intent launchBrowser = new Intent(Intent.ACTION_VIEW, Uri.parse(builder.toString()));
					launchBrowser.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
					startActivity(launchBrowser);
				}
			}
			catch (JSONException e) {
				Prefs.setSessionData(c, null);
				Log.e("Pocket+ Debug", e.getMessage());
			}
		}
	}

	/*
	 * OAuth step 2 - upgrade to access token
	 */
    private class GetAccessToken extends AsyncTask<Void, Void, Boolean> {

        // Get access token from Pocket API
        protected Boolean doInBackground(Void... params) {
			Log.e("Test", "GetAccessToken");
        	final Context c = getApplicationContext();
			try {
				APICall ac = new APICall(APICall.API_URL_OAUTH_ACCESS_TOKEN, c);
	            ac.addPostParam("consumer_key", APICall.API_OAUTH_CONSUMER_KEY);
	            ac.addPostParam("code", Prefs.getSessionData(c).getString("code"));
				ac.sync();
							
				if (ac.Json.getString("access_token") != "") {
					Prefs.setSessionData(c, ac.Json);
					Prefs.setLoggedIn(c, true);
					setResult(RESULT_OK);
					return true;
				}
				else {
					Prefs.setSessionData(c, null);
					Prefs.setLoggedIn(c, false);
					return false;	
				}
			}
			catch (JSONException e) {
				Prefs.setSessionData(c, null);
				Prefs.setLoggedIn(c, false);
				Log.e("Pocket+ Debug", e.getMessage());
				return false;
			}
			catch (ReaderException e) {
				Prefs.setSessionData(c, null);
				Prefs.setLoggedIn(c, false);
				return false;
			}
        }

        // On callback from the authorize API, show toast if failed or finish activity
        protected void onPostExecute(Boolean result) {
            final Context c = getApplicationContext();
            if (!result) {
            	Toast.makeText(c, getText(R.string.msg_login_fail), Toast.LENGTH_LONG).show();
            }
            finish();
        }
    }
}
