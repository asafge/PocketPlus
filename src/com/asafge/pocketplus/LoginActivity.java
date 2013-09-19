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
import android.net.Uri.Builder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

import com.noinnion.android.reader.api.ReaderException;
import com.noinnion.android.reader.api.ReaderExtension;
import com.asafge.pocketplus.Prefs;

public class LoginActivity extends Activity {
	
	protected ProgressDialog mBusy;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Context c = getApplicationContext();
		setResult(RESULT_CANCELED);	
		
		String action = getIntent().getAction();
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
		else {
			new GetRequestToken().execute(APICall.API_OAUTH_CONSUMER_KEY, APICall.API_OAUTH_REDIRECT);
			try {
				JSONObject json = Prefs.getSessionData(c);
				if (json != null) {
					Uri.Builder b = Uri.parse(APICall.API_URL_OAUTH_AUTHORIZE_APP).buildUpon();			
					b.appendQueryParameter("request_token", json.getString("code"));
					b.appendQueryParameter("redirect_uri", URLEncoder.encode(APICall.API_OAUTH_REDIRECT, "UTF-8"));
					Intent launchBrowser = new Intent(Intent.ACTION_VIEW, b.build());
					startActivity(launchBrowser);
				}
			}
			catch (JSONException e) {
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class GetRequestToken extends AsyncTask<String, Void, Boolean> {
		protected Boolean doInBackground(String... params) {
			String key = params[0];
			String redirect_uri = params[1];

			final Context c = getApplicationContext();
			APICall ac = new APICall(APICall.API_URL_OAUTH_REQUEST_TOKEN, c);
			ac.addPostParam("consumer_key", key);
			ac.addPostParam("redirect_uri", redirect_uri);
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
	}

    private class GetAccessToken extends AsyncTask<String, Void, Boolean> {

        // Show the login... process dialog
        protected void onPreExecute() {
            mBusy = ProgressDialog.show(LoginActivity.this, null, getText(R.string.msg_login_running), true, true);
        }

        // Async call to Pocket API for OAuth request token
        protected Boolean doInBackground(String... params) {
            String key = params[0];
            String redirect_uri = params[1];

            final Context c = getApplicationContext();
            APICall ac = new APICall(APICall.API_URL_OAUTH_ACCESS_TOKEN, c);
            ac.addPostParam("consumer_key", key);
            ac.addPostParam("redirect_uri", redirect_uri);

			try {
				ac.sync();
				Prefs.setSessionData(c, ac.Json);
				Prefs.setLoggedIn(c, true);
				setResult(ReaderExtension.RESULT_LOGIN);
				return true;
			}
			catch (ReaderException e) {
				Prefs.setLoggedIn(c, false);
				return false;
			}
        }

        // On callback - show toast if failed / go to main screen
        protected void onPostExecute(Boolean result) {
            final Context c = getApplicationContext();
            if (mBusy != null && mBusy.isShowing())
                mBusy.dismiss();
            if (result)
                finish();
            else
                Toast.makeText(c, getText(R.string.msg_login_fail), Toast.LENGTH_LONG).show();
        }
    }
}
