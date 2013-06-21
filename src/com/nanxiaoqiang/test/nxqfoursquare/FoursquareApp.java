package com.nanxiaoqiang.test.nxqfoursquare;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.nanxiaoqiang.test.nxqfoursquare.FoursquareDialog.FsqDialogListener;

public class FoursquareApp {
	private FoursquareSession mSession;
	private FoursquareDialog mDialog;
	private FsqAuthListener mListener;

	private ProgressDialog mProgress;

	private String mTokenUrl;
	private String mAccessToken;

	private static final String TAG = "FoursquareApi";

	public FoursquareApp(Context context, String clientId, String clientSecret) {
		mSession = new FoursquareSession(context);

		mAccessToken = mSession.getAccessToken();

		mTokenUrl = ApiInfo.TOKEN_URL + "&client_id=" + clientId
				+ "&client_secret=" + clientSecret + "&redirect_uri="
				+ ApiInfo.CALLBACK_URL;

		String url = ApiInfo.AUTH_URL + "&client_id=" + clientId
				+ "&redirect_uri=" + ApiInfo.CALLBACK_URL;

		FsqDialogListener listener = new FsqDialogListener() {
			@Override
			public void onComplete(String code) {
				getAccessToken(code);
			}

			@Override
			public void onError(String error) {
				mListener.onFail("Authorization failed");
			}
		};

		mDialog = new FoursquareDialog(context, url, listener);
		mProgress = new ProgressDialog(context);

		mProgress.setCancelable(false);
	}

	private void getAccessToken(final String code) {
		mProgress.setMessage("Getting access token ...");
		mProgress.show();

		new Thread() {
			@Override
			public void run() {
				Log.i(TAG, "Getting access token");

				int what = 0;

				try {
					URL url = new URL(mTokenUrl + "&code=" + code);

					Log.i(TAG, "Opening URL " + url.toString());

					HttpURLConnection urlConnection = (HttpURLConnection) url
							.openConnection();

					urlConnection.setRequestMethod("GET");
					urlConnection.setDoInput(true);
					// urlConnection.setDoOutput(true);

					urlConnection.connect();

					JSONObject jsonObj = (JSONObject) new JSONTokener(
							streamToString(urlConnection.getInputStream()))
							.nextValue();
					mAccessToken = jsonObj.getString("access_token");

					Log.i(TAG, "Got access token: " + mAccessToken);
				} catch (Exception ex) {
					what = 1;
					Log.e(TAG, "what is "+what + " | " + ex.getMessage());
					ex.printStackTrace();
				}

				mHandler.sendMessage(mHandler.obtainMessage(what, 1, 0));
			}
		}.start();
	}

	private void fetchUserName() {
		mProgress.setMessage("Finalizing ...");

		new Thread() {
			@Override
			public void run() {
				Log.i(TAG, "Fetching user name");
				int what = 0;

				try {
					String v = timeMilisToString(System.currentTimeMillis());
					URL url = new URL(ApiInfo.API_URL
							+ "/users/self?oauth_token=" + mAccessToken + "&v="
							+ v);

					Log.d(TAG, "Opening URL " + url.toString());

					HttpURLConnection urlConnection = (HttpURLConnection) url
							.openConnection();

					urlConnection.setRequestMethod("GET");
					urlConnection.setDoInput(true);
					// urlConnection.setDoOutput(true);

					urlConnection.connect();

					String response = streamToString(urlConnection
							.getInputStream());
					JSONObject jsonObj = (JSONObject) new JSONTokener(response)
							.nextValue();

					JSONObject resp = (JSONObject) jsonObj.get("response");
					JSONObject user = (JSONObject) resp.get("user");

					String firstName = user.getString("firstName");
					// String lastName = user.getString("lastName");

					Log.i(TAG, "Got user name: " + firstName);// + " " +
																// lastName);

					mSession.storeAccessToken(mAccessToken, firstName);// + " "
					// + lastName);
				} catch (Exception ex) {
					what = 1;
					Log.e(TAG, "what is " + what + " | " + ex.getMessage());
					ex.printStackTrace();
				}

				mHandler.sendMessage(mHandler.obtainMessage(what, 2, 0));
			}
		}.start();
	}

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			if (msg.arg1 == 1) {
				if (msg.what == 0) {
					fetchUserName();
				} else {
					mProgress.dismiss();

					mListener.onFail("Failed to get access token");
				}
			} else {
				mProgress.dismiss();

				mListener.onSuccess();
			}
		}
	};

	public boolean hasAccessToken() {
		return (mAccessToken == null) ? false : true;
	}

	public void setListener(FsqAuthListener listener) {
		mListener = listener;
	}

	public String getUserName() {
		return mSession.getUsername();
	}

	public void authorize() {
		mDialog.show();
	}

	public boolean checkin(String venueId, String venue, String broadcast,
			String shout, double latitude, double longitude) {

		// String v = timeMilisToString(System.currentTimeMillis());
		String ll = String.valueOf(latitude) + "," + String.valueOf(longitude);
		String url_string = ApiInfo.API_URL + "/checkins/add";

		Log.d(TAG, "Opening URL " + url_string);

		HttpPost httpRequest = new HttpPost(url_string);

		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("venueId", venueId));
		params.add(new BasicNameValuePair("venue", venue));
		params.add(new BasicNameValuePair("ll", ll));
		params.add(new BasicNameValuePair("broadcast", broadcast));
		params.add(new BasicNameValuePair("shout", shout));
		params.add(new BasicNameValuePair("oauth_token", mAccessToken));

		try {
			// 发出HTTP request
			httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
			// 取得HTTP response
			HttpResponse httpResponse = new DefaultHttpClient()
					.execute(httpRequest);

			// 若状态码为200 ok
			if (httpResponse.getStatusLine().getStatusCode() == 200) {
				// 取出回应字串
				// 在此可以进行返回值解析
				String strResult = EntityUtils.toString(httpResponse
						.getEntity());
				Log.i(TAG, "strResult is :" + strResult);
				return true;
			} else {

			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			Log.e(TAG, e.getMessage());
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			Log.e(TAG, e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			Log.e(TAG, e.getMessage());
		}
		return false;
	}

	public ArrayList<FsqVenue> getNearBy(double latitude, double longitude,
			String queryStr) throws Exception {
		ArrayList<FsqVenue> venueList = new ArrayList<FsqVenue>();
		try {
			String v = timeMilisToString(System.currentTimeMillis());
			String ll = String.valueOf(latitude) + ","
					+ String.valueOf(longitude);
			String url_string = ApiInfo.API_URL + "/venues/search?ll=" + ll
					+ "&oauth_token=" + mAccessToken + "&v=" + v;
			if (!queryStr.trim().equals("")) {
				url_string += "&query=" + java.net.URLEncoder.encode(queryStr);
			}

			URL url = new URL(url_string);

			Log.d(TAG, "Opening URL " + url.toString());

			HttpURLConnection urlConnection = (HttpURLConnection) url
					.openConnection();

			urlConnection.setRequestMethod("GET");
			urlConnection.setDoInput(true);
			// urlConnection.setDoOutput(true);

			urlConnection.connect();

			String response = streamToString(urlConnection.getInputStream());
			JSONObject jsonObj = (JSONObject) new JSONTokener(response)
					.nextValue();
			// 得到返回值
			if (getResponseCode(jsonObj)) {
				JSONArray groups = (JSONArray) jsonObj
						.getJSONObject("response").getJSONArray("venues");

				int length = groups.length();

				if (length > 0) {
					for (int i = 0; i < length; i++) {
						JSONObject item = (JSONObject) groups.get(i);

						FsqVenue venue = new FsqVenue(item);
						venueList.add(venue);
					}
				}
			}
		} catch (Exception ex) {
			throw ex;
		}

		return venueList;
	}

	public ArrayList<FsqVenue> getNearby(double latitude, double longitude)
			throws Exception {
		ArrayList<FsqVenue> venueList = new ArrayList<FsqVenue>();

		try {
			String v = timeMilisToString(System.currentTimeMillis());
			String ll = String.valueOf(latitude) + ","
					+ String.valueOf(longitude);
			URL url = new URL(ApiInfo.API_URL + "/venues/search?ll=" + ll
					+ "&oauth_token=" + mAccessToken + "&v=" + v);

			Log.d(TAG, "Opening URL " + url.toString());

			HttpURLConnection urlConnection = (HttpURLConnection) url
					.openConnection();

			urlConnection.setRequestMethod("GET");
			urlConnection.setDoInput(true);
			// urlConnection.setDoOutput(true);

			urlConnection.connect();

			String response = streamToString(urlConnection.getInputStream());
			JSONObject jsonObj = (JSONObject) new JSONTokener(response)
					.nextValue();
			// 得到返回值
			if (getResponseCode(jsonObj)) {
				JSONArray groups = (JSONArray) jsonObj
						.getJSONObject("response").getJSONArray("venues");

				int length = groups.length();

				if (length > 0) {
					for (int i = 0; i < length; i++) {
						JSONObject item = (JSONObject) groups.get(i);

						FsqVenue venue = new FsqVenue(item);
						// FsqVenue venue = new FsqVenue();
						//
						// venue.id = item.getString("id");
						// venue.name = item.getString("name");
						//
						// JSONObject location = (JSONObject) item
						// .getJSONObject("location");
						//
						// Location loc = new Location(
						// LocationManager.GPS_PROVIDER);
						//
						// loc.setLatitude(Double.valueOf(location
						// .getString("lat")));
						// loc.setLongitude(Double.valueOf(location
						// .getString("lng")));
						//
						// venue.location = loc;
						// venue.address = location.getString("address");
						// venue.distance = location.getInt("distance");
						// venue.herenow = item.getJSONObject("hereNow").getInt(
						// "count");
						// venue.type = group.getString("type");

						venueList.add(venue);
					}
				}
			}
		} catch (Exception ex) {
			throw ex;
		}

		return venueList;
	}

	private boolean getResponseCode(JSONObject jsonObj) throws JSONException {
		int responseCode = jsonObj.getJSONObject("meta").getInt("code");
		Log.i(TAG, "response code is " + responseCode);
		if (responseCode != 200) {
			int errorCode = jsonObj.getJSONObject("meta").getInt("code");
			String errorType = jsonObj.getJSONObject("meta").getString(
					"errorType");
			String errorDetail = jsonObj.getJSONObject("meta").getString(
					"errorDetail");
			Log.e(TAG, "ERROR: " + errorCode + "|errorType:" + errorType
					+ "|errorDetail:" + errorDetail);
			// Toast
			return false;
		}
		return true;
	}

	private String streamToString(InputStream is) throws IOException {
		String str = "";

		if (is != null) {
			StringBuilder sb = new StringBuilder();
			String line;

			try {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(is));

				while ((line = reader.readLine()) != null) {
					sb.append(line);
				}

				reader.close();
			} finally {
				is.close();
			}

			str = sb.toString();
		}

		return str;
	}

	private String timeMilisToString(long milis) {
		SimpleDateFormat sd = new SimpleDateFormat("yyyyMMdd");
		Calendar calendar = Calendar.getInstance();

		calendar.setTimeInMillis(milis);

		return sd.format(calendar.getTime());
	}

	public interface FsqAuthListener {
		public abstract void onSuccess();

		public abstract void onFail(String error);
	}
}
