package com.asafge.pocketplus;

import java.net.HttpURLConnection;

import oauth.signpost.OAuthConsumer;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;

import org.apache.http.HttpRequest;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.androidquery.AQuery;
import com.androidquery.WebDialog;
import com.androidquery.auth.AccountHandle;
import com.androidquery.callback.AbstractAjaxCallback;
import com.androidquery.callback.AjaxStatus;
import com.androidquery.util.AQUtility;

public class PocketHandle extends AccountHandle{

	private static final String OAUTH_REQUEST_TOKEN = "https://getpocket.com/v3/oauth/request";
	private static final String OAUTH_ACCESS_TOKEN = "https://getpocket.com/v3/oauth/authorize";
	private static final String OAUTH_AUTHORIZE = "https://getpocket.com/v3/oauth/authorize";
	private static final String CALLBACK_URI = "twitter://callback";
	private static final String CANCEL_URI = "twitter://cancel";
	
	private Activity act;
	private WebDialog dialog;
	private CommonsHttpOAuthConsumer consumer;
	private CommonsHttpOAuthProvider provider;
	private String token;
	private String secret;
	
	public PocketHandle(Activity act, String consumerKey) {
		this.act = act;
		
		consumer = new CommonsHttpOAuthConsumer(consumerKey, "");
		token = fetchToken(POCKET_TOKEN);
		secret = fetchToken(POCKET_SECRET);
		
		if(token != null && secret != null)
			consumer.setTokenWithSecret(token, secret);
		provider = new CommonsHttpOAuthProvider(OAUTH_REQUEST_TOKEN, OAUTH_ACCESS_TOKEN, OAUTH_AUTHORIZE);		  
	}

	public String getToken(){
		return token;
	}
	
	public String getSecret(){
		return secret;
	}

	private void dismiss(){
		if(dialog != null){
			new AQuery(act).dismiss(dialog);
			dialog = null;
		}
	}
	
	private void show(){
		if(dialog != null){
			new AQuery(act).show(dialog);
		}
	}
	
	private void failure() {
		dismiss();
		failure(act, 401, "cancel");
	}
	
	protected void auth() {
		Task task = new Task();
		task.execute();
		
	}
	
	private class Task extends AsyncTask<String, String, String> implements OnCancelListener, Runnable {
		
		private AbstractAjaxCallback<?, ?> cb;
		
		@Override
		protected String doInBackground(String... params) {
			String url = null;
			try {
				url = provider.retrieveRequestToken(consumer, CALLBACK_URI);
			}
			catch(Exception e) {
				AQUtility.report(e);				
				return null;
			} 
			return url;
		}

		@Override
		protected void onPostExecute(String url) {
			
			if(url != null) {
				dialog = new WebDialog(act, url, new TwWebViewClient());		
				dialog.setOnCancelListener(this);			
				show();		
				dialog.load();
				
			} else {
				failure();
			}
		}

		@Override
		public void onCancel(DialogInterface arg0) {
			failure();
		}

		@Override
		public void run() {
			auth(cb);
		}	
	}
	
	public void authenticate(boolean refreshToken) {
		if (!refreshToken && token != null && secret != null) {
			authenticated(secret, token);
		}
		else {
			auth();
		}
	}
	
	protected void authenticated(String secret, String token) {
		
	}
	
	private static final String POCKET_TOKEN = "aq.pocket.token";
	private static final String POCKET_SECRET = "aq.pocket.secret";
	
	private String fetchToken(String key) {
		return PreferenceManager.getDefaultSharedPreferences(act).getString(key, null);	
	}
	
	private void storeToken(String key1, String token1, String key2, String token2) {
		PreferenceManager.getDefaultSharedPreferences(act).edit().putString(key1, token1).putString(key2, token2).commit();	
	}

	private String extract(String url, String param) {
		Uri uri = Uri.parse(url);
		String value = uri.getQueryParameter(param);
		return value;	
	}

	private class Task2 extends AsyncTask<String, String, String> {

		@Override
		protected String doInBackground(String... params) {		
			try {
				provider.retrieveAccessToken(consumer, params[0]);
			} 
			catch(Exception e) {
				AQUtility.report(e);
				return null;
			}
			return "";
		}
		
		@Override
		protected void onPostExecute(String url) {
			if(url != null){			
				token = consumer.getToken();
				secret = consumer.getTokenSecret();
				
				AQUtility.debug("token", token);
				AQUtility.debug("secret", secret);
				
				storeToken(POCKET_TOKEN, token, POCKET_SECRET, secret);
				
				dismiss();
				success(act);
				
				authenticated(secret, token);
			}
			else {
				failure();
				authenticated(null, null);
			}
		}
	}
	
	private class TwWebViewClient extends WebViewClient {
		
		private boolean checkDone(String url) {
			if(url.startsWith(CALLBACK_URI)) {
            	String verf = extract(url, "oauth_verifier");           	      
            	dismiss();
            	Task2 task = new Task2();
            	task.execute(verf);
                return true;
            }
			else if(url.startsWith(CANCEL_URI)) {               
    			failure();
                return true;
            }
            return false;
		}
		
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return checkDone(url);
		}
		
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			AQUtility.debug("started", url);		
			if(checkDone(url)) {
			}
			else {
				super.onPageStarted(view, url, favicon);
			}
		}
		
		@Override
		public void onPageFinished(WebView view, String url) {
			AQUtility.debug("finished", url);
			super.onPageFinished(view, url);
			show();
		}
		
		@Override
		public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
			failure();
		}
	}

	@Override
	public boolean expired(AbstractAjaxCallback<?, ?> cb, AjaxStatus status) {
		int code = status.getCode();
		return code == 400 || code == 401;
	}

	@Override
	public boolean reauth(final AbstractAjaxCallback<?, ?> cb) {		
		token = null;
		secret = null;
		storeToken(POCKET_TOKEN, null, POCKET_SECRET, null);
		
		Task task = new Task();
		task.cb = cb;
		AQUtility.post(cb);
		return false;
	}

	@Override
	public void applyToken(AbstractAjaxCallback<?, ?> cb, HttpRequest request) {
		AQUtility.debug("apply token", cb.getUrl());
		try {
			consumer.sign(request);
		} 
		catch(Exception e) {
			AQUtility.report(e);
		}
	}
	
	@Override	
	public void applyToken(AbstractAjaxCallback<?, ?> cb, HttpURLConnection conn) {
		AQUtility.debug("apply token multipart", cb.getUrl());
		
		OAuthConsumer oac = new DefaultOAuthConsumer(consumer.getConsumerKey(), consumer.getConsumerSecret());
		oac.setTokenWithSecret(consumer.getToken(), consumer.getTokenSecret());
		
		try {
			oac.sign(conn);
		} 
		catch(Exception e) {
			AQUtility.report(e);
		}
	}

	@Override
	public boolean authenticated() {
		return token != null && secret != null;
	}

	@Override
	public void unauth() {
		token = null;
		secret = null;
		CookieSyncManager.createInstance(act);
		CookieManager.getInstance().removeAllCookie();		
		storeToken(POCKET_TOKEN, null, POCKET_SECRET, null);
	}
}
