package com.nanxiaoqiang.test.nxqfoursquare;

import java.io.Serializable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.location.Location;
import android.location.LocationManager;

public class FsqVenue implements Serializable {
	private static final long serialVersionUID = 1L;

	public String id = "";// ID：4cf59921dc40a35d34554354
	public String name = "";// 名称：中国铁道科学研究院
	// contact 暂时没有解析
	public String address = "";
	public String type = "";
	// "location":{"address":"中国","crossStreet":"大柳树路2号","lat":39.95622870940392,"lng":116.33317411733033,"distance":75,"city":"北京市","state":"北京市","country":"China","cc":"CN"}
	public Location location = null;
	public int direction = -1;
	public int distance = -1;
	public int herenow = 0;

	public String iconUrl = "https://foursquare.com/img/categories_v2/none_bg_64.png";
	public String categoryName = "";

	public FsqVenue() {
		super();
	}

	public FsqVenue(JSONObject json) throws JSONException {
		this.id = json.getString("id");
		this.name = json.getString("name");

		JSONObject location = (JSONObject) json.getJSONObject("location");

		Location loc = new Location(LocationManager.GPS_PROVIDER);

		loc.setLatitude(Double.valueOf(location.getString("lat")));
		loc.setLongitude(Double.valueOf(location.getString("lng")));

		this.location = loc;
		if (location.has("address")) {
			this.address = location.getString("address");
		}
		this.distance = location.getInt("distance");
		this.herenow = json.getJSONObject("hereNow").getInt("count");

		JSONArray categories = (JSONArray) json.getJSONArray("categories");
		if (categories.length() > 0) {
			for (int i = 0; i < categories.length(); i++) {
				JSONObject category = (JSONObject) categories.get(i);

				if (category.has("primary")) {
					boolean primary = category.getBoolean("primary");
					if (primary) {
						this.categoryName = category.getString("name");
						this.iconUrl = category.getJSONObject("icon")
								.getString("prefix")
								+ "64"
								+ category.getJSONObject("icon").getString(
										"suffix");
					} else {
						continue;
					}
				} else {
					continue;
				}
			}
		}
	}
}
