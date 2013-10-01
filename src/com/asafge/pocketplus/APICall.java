package com.asafge.pocketplus;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.noinnion.android.reader.api.ReaderException;

public class APICall {
	
	private class Param {
		public Param(String key, String value) {
			Key = key;
			Value = value;
		}
		public String Key;
		public String Value;
	}

	private List<Param> params_post = new ArrayList<Param>();
	
	public JSONObject Json;
	public AjaxStatus Status;
	private AjaxCallback<JSONObject> callback;
	private AQuery aquery;
	private String callbackUrl;
	
	// Constructor
	public APICall(String url,  Context c) {
		aquery = new AQuery(c);
		createCallback(url, c);
	}

	// Create a callback object, before running sync
	private void createCallback(String url, Context c) {
		callbackUrl = url;
		callback = new AjaxCallback<JSONObject>();
		callback.header("User-Agent", Prefs.USER_AGENT);
		callback.header("X-Accept", "application/json");
		callback.header("Content-Type", "application/x-www-form-urlencoded; charset=UTF8");
		callback.url(callbackUrl).type(JSONObject.class);
		callback.timeout(5000);
		callback.retry(3);
		callback.method(AQuery.METHOD_POST);
	}
	
	// Add a Post parameter to this call
	public boolean addPostParam(String key, String value) {
		if (callback == null)
			return false;
		params_post.add(new Param(key, value));
		return true;
	}
			
	// Add the Get and Post params to the callback before executing
	private void addAllParams() {
		for (Param p: params_post)
			callback.param(p.Key, p.Value);
	}
	
	// Make this API call an authenticated one.
	public APICall makeAuthenticated() throws ReaderException {
		final Context c = aquery.getContext();
		try {
			this.addPostParam("consumer_key", APICall.API_OAUTH_CONSUMER_KEY);
			this.addPostParam("access_token", Prefs.getSessionData(c).getString("access_token"));
			return this;
		}
		catch (JSONException e) {
			Prefs.setSessionData(c, null);
			Prefs.setLoggedIn(c, false);
			throw new ReaderException("User not authenticated");
		}
	}
	
	// Run synchronous HTTP request and check for valid response
	public void sync() throws ReaderException {
		if (callback == null)
			return;
		addAllParams();
		aquery.sync(callback);
		Json = callback.getResult();
		Status = callback.getStatus();
		if (Json == null) {
			Log.e("Pocket+ Debug", "URL: " + callbackUrl);
			Log.e("Pocket+ Debug", "Status: " + Status.getMessage() + " | " + String.valueOf(Status.getCode()));
			Log.e("Pocket+ Debug", "Session Data: " + Prefs.getSessionData(aquery.getContext()));
			throw new ReaderException("Pocket server unreachable");
		}
		if (Status.getCode() != 200)
			throw new ReaderException("OAuth process error");
	}
		
	// Run synchronous HTTP request, check valid response + successful operation 
	public boolean syncGetResultOk() throws ReaderException {
		try {
			sync();
			return (this.Json.getInt("status") == 1);
		} 
		catch (JSONException e) {
			Log.e("Pocket+ Debug", "JSON Object: " + Json.toString());
			throw new ReaderException("Unknown API response");
		}		
	}
	
	// API constants
    public static String API_OAUTH_CONSUMER_KEY = "16932-b0d065023261f24a7fa5ffcd";
    public static String API_OAUTH_REDIRECT = "pocketplus:authorizationFinished";

    // Authentication process
	public static String API_URL_BASE_SECURE = "https://getpocket.com/";
	public static String API_URL_OAUTH_REQUEST_TOKEN = API_URL_BASE_SECURE + "v3/oauth/request";
	public static String API_URL_OAUTH_AUTHORIZE_APP = API_URL_BASE_SECURE + "auth/authorize";
	public static String API_URL_OAUTH_ACCESS_TOKEN = API_URL_BASE_SECURE + "v3/oauth/authorize";
	
	// Stories handling
	public static String API_URL_ADD = API_URL_BASE_SECURE + "v3/add";
	public static String API_URL_GET = API_URL_BASE_SECURE + "v3/get";
	public static String API_URL_SEND = API_URL_BASE_SECURE + "v3/send";
}
