package com.asafge.pocketplus;

import com.noinnion.android.reader.api.provider.ITag;

/*
 * A simple singleton object for the untagged items tag
 */
public class UntaggedTag {
	private static ITag untagged = null;
	protected UntaggedTag() {
		untagged = PocketPlus.createTag("Untagged", false);
   	}
	public static ITag get() {
		if(untagged == null)
			new UntaggedTag();
		return untagged;
	}
}