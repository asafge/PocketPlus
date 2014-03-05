package com.asafge.pocketplus;

import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.noinnion.android.reader.api.ReaderException;
import com.asafge.pocketplus.util.Utils;

public class AddActivity extends Activity {

	/* 
	 * Add anew URL to Pocket using Pocket+
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
	    Intent intent = getIntent();
	    String action = intent.getAction();
	    String type = intent.getType();

	    if (Intent.ACTION_SEND.equals(action) && type != null && "text/plain".equals(type)) {
			try {
                String urlStr = Utils.extractURL(intent.getStringExtra(Intent.EXTRA_TEXT));
				URL u = new URL(urlStr);
				new AddToPocket().execute(u.toString());
			}
			catch (MalformedURLException e) {
				Log.e("Pocket+ Debug", "Add to Pocket+ Exception:" + e.getMessage());
				Context c = getApplicationContext();
				Toast.makeText(c, getString(R.string.not_added_to_pocket_invalid), Toast.LENGTH_LONG).show();
			}
	    }
	    finish();
	}
	
	/*
	 * Asynchronous call to add a URL to Pocket
	 */
	private class AddToPocket extends AsyncTask<String, Void, Boolean> {
		protected Boolean doInBackground(String... params) {
			final Context c = getApplicationContext();
			try {
				APICall ac = new APICall(APICall.API_URL_ADD, c);
				ac.addPostParam("url", params[0]);
				return ac.makeAuthenticated().syncGetResultOk();
			}
			catch (ReaderException e) {
				Log.e("Pocket+ Debug", "JSONException: " + e.getMessage());
				return false;
			}
		}

		protected void onPostExecute(Boolean result) {
			final Context c = getApplicationContext();
			if (result) {
				Toast.makeText(c, getString(R.string.added_to_pocket), Toast.LENGTH_SHORT).show();
			}
			else {
				Toast.makeText(c, getString(R.string.not_added_to_pocket), Toast.LENGTH_LONG).show();
			}
		}
	}
}
