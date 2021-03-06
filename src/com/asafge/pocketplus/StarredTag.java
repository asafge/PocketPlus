package com.asafge.pocketplus;

import com.noinnion.android.reader.api.provider.ITag;

/*
 * A simple singleton object for the starred items tag
 */
public class StarredTag {
	private static ITag star = null;
	protected StarredTag() {
		star = PocketPlus.createTag("Starred items", true);
   	}
	public static ITag get() {
		if(star == null)
			new StarredTag();
		return star;
	}
}