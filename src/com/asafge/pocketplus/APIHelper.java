package com.asafge.pocketplus;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.noinnion.android.reader.api.ReaderException;
import com.noinnion.android.reader.api.provider.ITag;

public class APIHelper {
	
	// Get all the unread story hashes at once
	// Note: There's a limit on how many feeds you can pass (~100).
	//		 These are GET params, and NewsBlur API has a limit on URL lengths. 
	public static List<String> getUnreadHashes(Context c, int limit, List<String> feeds, RotateQueue<String> seenHashes) throws ReaderException {
		try {
			List<String> hashes = new ArrayList<String>();

			APICall ac = new APICall(APICall.API_URL_UNREAD_HASHES, c);
			if (feeds != null) 
				ac.addGetParams("feed_id", feeds);
			ac.sync();
			
			JSONObject json_feeds = ac.Json.getJSONObject("unread_feed_story_hashes");
			Iterator<?> keys = json_feeds.keys();
			while (keys.hasNext()) {
				String feedID = (String)keys.next();
				JSONArray items = json_feeds.getJSONArray(feedID);
				for (int i=0; i<items.length() && i<limit; i++) {
					String hash = items.getString(i);
					if (seenHashes == null || !seenHashes.SearchElement(hash))
						hashes.add(hash);
				}
				limit -= items.length();
			}
			return hashes;
		}
		catch (JSONException e) {
			Log.e("NewsBlur+ Debug", "JSONException: " + e.getMessage());
			throw new ReaderException("GetUnreadHashes parse error", e);
		}
	}
	
	// Get all the starred story hashes at once
	public static List<String> getStarredHashes(Context c, int limit, RotateQueue<String> seenHashes) throws ReaderException {
		try {
			List<String> hashes = new ArrayList<String>();
			APICall ac = new APICall(APICall.API_URL_STARRED_HASHES, c);
			ac.sync();
			JSONArray items = ac.Json.getJSONArray("starred_story_hashes");
			for (int i=0; i<items.length() && i<limit; i++) {
				String hash = items.getString(i);
				if (seenHashes == null || !seenHashes.SearchElement(hash))
					hashes.add(hash);
			}
			return hashes;
		}
		catch (JSONException e) {
			Log.e("NewsBlur+ Debug", "JSONException: " + e.getMessage());
			throw new ReaderException("GetStarredHashes parse error", e);
		}
	}
	
	// Create a new tag object
	public static ITag createTag(String name, boolean isStar) {
		ITag tag = new ITag();
		tag.label = name;
		String prefix = isStar ? "STAR" : "FOL";
		tag.uid = name = (prefix + ":" + name);
		tag.type = isStar ? ITag.TYPE_TAG_STARRED : ITag.TYPE_FOLDER;
		return tag;
	}
}
