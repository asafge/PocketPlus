package com.asafge.pocketplus;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import com.noinnion.android.reader.api.ExtensionPrefs;

public class Prefs extends ExtensionPrefs {

	public static final String KEY_LOGGED_IN = "logged_in";
	public static final String KEY_JSON = "key_json";
	public static final String USER_AGENT = System.getProperty("http.agent");

	public static boolean isLoggedIn(Context c) {
		return getBoolean(c, KEY_LOGGED_IN, false);
	}

	public static void setLoggedIn(Context c, boolean value) {
		putBoolean(c, KEY_LOGGED_IN, value);
	}

	public static JSONObject getSessionData(Context c) {
		try {
			return new JSONObject(getString(c, KEY_JSON));
		}
		catch (JSONException e) {
			return null;
		}
		catch (NullPointerException e) {
			return null;
		}
	}

	public static void setSessionData(Context c, JSONObject json) {
		putString(c, KEY_JSON, (json != null) ? json.toString() : "");
	}
}
