package com.nanxiaoqiang.test.nxqfoursquare;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class FoursquareSession {
	private SharedPreferences sharedPref;

	private Editor editor;

	private static final String SHARED = "NxqFoursquare_Preferences";
	private static final String FSQ_USERNAME = "username";
	private static final String FSQ_ACCESS_TOKEN = "access_token";
	private static final String FSQ_CLIENT_ID = "client_id";// 暂时无用
	private static final String FSQ_CLIENT_SECRET = "client_secret";// 暂时无用

	public FoursquareSession(Context context) {
		sharedPref = context.getSharedPreferences(SHARED, Context.MODE_PRIVATE);

		editor = sharedPref.edit();
	}

	public void storeAccessToken(String accessToken, String username) {
		editor.putString(FSQ_ACCESS_TOKEN, accessToken);
		editor.putString(FSQ_USERNAME, username);

		editor.commit();
	}

	public void resetAccessToken() {
		editor.putString(FSQ_ACCESS_TOKEN, null);
		editor.putString(FSQ_USERNAME, null);

		editor.commit();
	}

	public String getUsername() {
		return sharedPref.getString(FSQ_USERNAME, null);
	}

	public String getAccessToken() {
		return sharedPref.getString(FSQ_ACCESS_TOKEN, null);
	}
}
