package com.asafge.pocketplus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import com.asafge.pocketplus.APICall;
import com.asafge.pocketplus.APIHelper;
import com.asafge.pocketplus.Prefs;
import com.asafge.pocketplus.RotateQueue;
import com.asafge.pocketplus.StarredTag;
import com.noinnion.android.reader.api.ReaderException;
import com.noinnion.android.reader.api.ReaderExtension;
import com.noinnion.android.reader.api.internal.IItemIdListHandler;
import com.noinnion.android.reader.api.internal.IItemListHandler;
import com.noinnion.android.reader.api.internal.ISubscriptionListHandler;
import com.noinnion.android.reader.api.internal.ITagListHandler;
import com.noinnion.android.reader.api.provider.IItem;

public class PocketPlus extends ReaderExtension {
	private Context c;
	
	/*
	 * Constructor
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		c = getApplicationContext();
	};
	
	/*
	 * Non implemented
	 */
	@Override
	public void handleReaderList(ITagListHandler arg0,
			ISubscriptionListHandler arg1, long arg2) throws IOException,
			ReaderException {
	}
	
	/* 
	 * Get a list of unread story IDS (URLs), UI will mark all other as read.
	 * This really speeds up the sync process. 
	 */
	@Override
	public void handleItemIdList(IItemIdListHandler handler, long syncTime) throws ReaderException {
		try {
			int limit = handler.limit();
			String uid = handler.stream();
			
			if (uid.startsWith(ReaderExtension.STATE_STARRED))
				handler.items(APIHelper.getStarredHashes(c, limit, null));
			else if (uid.startsWith(ReaderExtension.STATE_READING_LIST)) {
				List<String> hashes = APIHelper.getUnreadHashes(c, limit, null, null);
				handler.items(hashes);
			}
			else {
				Log.e("Pocket+ Debug", "Unknown reading state: " + uid);
				throw new ReaderException("Unknown reading state");
			}
		}
		catch (RemoteException e) {
			throw new ReaderException("ItemID handler error", e);
		}
	}
	
	/*
	 * Handle a single item list (a feed or a folder).
	 * This functions calls the parseItemList function.
	 */
	@Override
	public void handleItemList(IItemListHandler handler, long syncTime) throws ReaderException {
		try {
			String uid = handler.stream();
			String url = APICall.API_URL_RIVER;
			int limit = handler.limit();
			int chunk = 100;
			List<String> hashes;
			
			// Load the seen hashes from prefs
			if (uid.startsWith(ReaderExtension.STATE_READING_LIST) && (handler.startTime() <= 0))
				Prefs.setHashesList(c, "");
			RotateQueue<String> seenHashes = new RotateQueue<String>(1000, Prefs.getHashesList(c));
					
			if (uid.startsWith(ReaderExtension.STATE_STARRED)) {
				hashes = APIHelper.getStarredHashes(c, limit, seenHashes);
				url = APICall.API_URL_STARRED_STORIES;
			}
			else if (uid.startsWith(ReaderExtension.STATE_READING_LIST)) {
				List<String> unread_hashes = APIHelper.getUnreadHashes(c, limit, null, seenHashes);
				hashes =  new ArrayList<String>();
				/*for (String h : unread_hashes)
					if (!handler.excludedStreams().contains(APIHelper.getFeedUrlFromFeedId(h)))
						hashes.add(h);*/
			}
			else {
				Log.e("Pocket+ Debug", "Unknown reading state: " + uid);
				throw new ReaderException("Unknown reading state");
			}
				
			for (int start=0; start < hashes.size(); start += chunk) {
				APICall ac = new APICall(url, c);
				int end = (start+chunk < hashes.size()) ? start + chunk : hashes.size();
				ac.addGetParams("h", hashes.subList(start, end));
				ac.sync();
				parseItemList(ac.Json, handler, seenHashes);
			}
			// Save the seen hashes as a serialized String
			Prefs.setHashesList(c, seenHashes.toString());
		}
		catch (RemoteException e) {
			throw new ReaderException("ItemList handler error", e);
		}
	}
	
	/*
	 * Parse an array of items that are in the NewsBlur JSON format.
	 */
	public void parseItemList(JSONObject json, IItemListHandler handler, RotateQueue<String> seenHashes) throws ReaderException {
		try {
			int length = 0;
			List<IItem> items = new ArrayList<IItem>();
			JSONArray arr = json.getJSONArray("stories");
			for (int i=0; i<arr.length(); i++) {
				JSONObject story = arr.getJSONObject(i);
				IItem item = new IItem();
				item.title = story.getString("story_title");
				item.link = story.getString("story_permalink");
				item.uid = story.getString("story_hash");
				item.updatedTime = story.getLong("story_timestamp");
				item.publishedTime = story.getLong("story_timestamp");
				item.read = (story.getInt("read_status") == 1);
				item.content = story.getString("story_content");
				item.starred = (story.has("starred") && story.getString("starred") == "true");
				if (item.starred)
					item.addCategory(StarredTag.get().uid);
				items.add(item);
				seenHashes.AddElement(item.uid);
				
				length += item.getLength();
				if (items.size() % 200 == 0 || length > 300000) {
					handler.items(items, 0);
					items.clear();
					length = 0;
				}
			}
			handler.items(items, 0);
		}
		catch (JSONException e) {
			Log.e("Pocket+ Debug", "JSONExceotion: " + e.getMessage());
			Log.e("Pocket+ Debug", "JSON: " + json.toString());
			throw new ReaderException("ParseItemList parse error", e);
		}
		catch (RemoteException e) {
			throw new ReaderException("SingleItem handler error", e);
		}
	}
	
	/*
	 * Main function for marking stories (and their feeds) as read/unread.
	 */
	private boolean markAs(boolean read, String[]  itemUids, String[]  subUIds) throws ReaderException	{	
		APICall ac;
		if (itemUids == null && subUIds == null) {
			ac = new APICall(APICall.API_URL_MARK_ALL_AS_READ, c);
			return ac.syncGetResultOk();
		}
		else {
			if (itemUids != null) {
				String url = read ? APICall.API_URL_MARK_STORY_AS_READ : APICall.API_URL_MARK_STORY_AS_UNREAD;
				ac = new APICall(url, c);
				for (int i=0; i<itemUids.length; i++) {
					ac.addPostParam("story_id", itemUids[i]);
				}
				return ac.syncGetResultOk();
			}
		}
		return false;
	}

	/* 
	 * Mark a list of stories (and their feeds) as read
	 */
	@Override
	public boolean markAsRead(String[]  itemUids, String[]  subUIds) throws ReaderException {
		return this.markAs(true, itemUids, subUIds);
	}

	/* 
	 * Mark a list of stories (and their feeds) as unread
	 */
	@Override
	public boolean markAsUnread(String[]  itemUids, String[]  subUids, boolean keepUnread) throws ReaderException {
		return this.markAs(false, itemUids, subUids);
	}

	/*
	 * Mark all stories on all feeds as read. Iterate all feeds in order to avoid marking excluded feeds as read. 
	 * Note: S = subscription (feed), t = tag
	 */
	@Override
	public boolean markAllAsRead(String arg0, String arg1, String[] arg2, long arg3) throws IOException, ReaderException {
		return false;
	}

	/*
	 * Edit an item's tag - currently supports only starring/unstarring items
	 */
	@Override
	public boolean editItemTag(String[] itemUids, String[] subUids, String[] addTags, String[] removeTags) throws IOException, ReaderException {
		boolean result = true;
		for (int i=0; i<itemUids.length; i++) {
			String url;
			if ((addTags != null) && addTags[i].startsWith(StarredTag.get().uid)) {
				url = APICall.API_URL_MARK_STORY_AS_STARRED;
			}
			else if ((removeTags != null) && removeTags[i].startsWith(StarredTag.get().uid)) {
				url = APICall.API_URL_MARK_STORY_AS_UNSTARRED;
			}
			else {
				result = false;
				throw new ReaderException("This type of tag is not supported");
			}
			APICall ac = new APICall(url, c);
			ac.addPostParam("story_id", itemUids[i]);
			if (!ac.syncGetResultOk())
				break;
		}
		return result;
	}

	/*
	 * Rename a top level folder both in News+ and in NewsBlur server
	 */
	@Override
	public boolean renameTag(String tagUid, String oldLabel, String newLabel) throws IOException, ReaderException {
		if (!tagUid.startsWith("FOL:"))
			return false;
		else {
			APICall ac = new APICall(APICall.API_URL_FOLDER_RENAME, c);
			ac.addPostParam("folder_to_rename", oldLabel);
			ac.addPostParam("new_folder_name", newLabel);
			ac.addPostParam("in_folder", "");
			return ac.syncGetResultOk();
		}
	}
	
	/*
	 * Delete a top level folder both in News+ and in NewsBlur server
	 * This just removes the folder, not the feeds in it
	 */
	@Override
	public boolean disableTag(String tagUid, String label) throws IOException, ReaderException {
		if (tagUid.startsWith("STAR:"))
			return false;
		return false;
	}
	
	/*
	 * Main function for editing subscriptions - add/delete/rename/change-folder
	 */	
	@Override
	public boolean editSubscription(String uid, String title, String feed_url, String[] tags, int action, long syncTime) throws IOException, ReaderException {
		boolean result = false;
		switch (action) {
			// Feed - add/delete/rename
			case ReaderExtension.SUBSCRIPTION_ACTION_SUBCRIBE: {
				APICall ac = new APICall(APICall.API_URL_FEED_ADD, c);
				ac.addPostParam("url", feed_url);
				result = ac.syncGetResultOk();
				break;
			}
			case ReaderExtension.SUBSCRIPTION_ACTION_UNSUBCRIBE: {
				APICall ac = new APICall(APICall.API_URL_FEED_DEL, c);
				result = ac.syncGetResultOk();
				break;
			}
			case ReaderExtension.SUBSCRIPTION_ACTION_EDIT: {
				APICall ac = new APICall(APICall.API_URL_FEED_RENAME, c);
				ac.addPostParam("feed_title", title);
				result = ac.syncGetResultOk();
				break;
			}
			// Feed's parent folder - new_folder/add_to_folder/delete_from_folder
			case ReaderExtension.SUBSCRIPTION_ACTION_NEW_LABEL: {
				APICall ac = new APICall(APICall.API_URL_FOLDER_ADD, c);
				String newTag = tags[0].replace("FOL:", "");
				ac.addPostParam("folder", newTag);
				result = ac.syncGetResultOk();
				break;
			}
			case ReaderExtension.SUBSCRIPTION_ACTION_ADD_LABEL: {
				String newTag = tags[0].replace("FOL:", "");
				//result = APIHelper.moveFeedToFolder(c, APIHelper.getFeedIdFromFeedUrl(uid), "", newTag);
				break;
			}
			case ReaderExtension.SUBSCRIPTION_ACTION_REMOVE_LABEL: {
				String newTag = tags[0].replace("FOL:", "");
				//result = APIHelper.moveFeedToFolder(c, APIHelper.getFeedIdFromFeedUrl(uid), newTag, "");
				break;
			}
		}
		return result;
	}
}
