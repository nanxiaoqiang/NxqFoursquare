package com.nanxiaoqiang.test.nxqfoursquare;

import java.util.ArrayList;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.nanxiaoqiang.test.nxqfoursquare.FoursquareApp.FsqAuthListener;

public class MainActivity extends Activity {
	//
	// public static final String CLIENT_ID =
	// "KJL0BHQYSTCUQRQ2S4D0BSZV2SECX5KN5RK2HG0PDJJ0BFBA";
	// public static final String CLIENT_SECRET =
	// "DOAB01IHEYIJR1ZGW1WGJX1JQ2JD3FB0S0YGDJEC3QVE2NPD";

	private Button connectBtn;// 登录按钮
	private Button btnGo;// 用于检查Near Close的按钮
	private Button locationBtn;// 显示当前经纬度的按钮
	// 输入经纬度
	private EditText etLatitude;
	private EditText etLongitude;
	private TextView loginNameTv;// 登录名称
	private ListView mListView;// 查询结果集
	private EditText queryEt;// 查询输入框
	private Button queryBtn;// 查询按钮

	private FoursquareApp fsqApp;

	private NearbyAdapter mAdapter;
	private ArrayList<FsqVenue> mNearbyList;
	private ProgressDialog mProgress;

	// 经纬度取得相关
	private LocationManager locationManager;
	private LocationListener locationListener;
	private Location location;
	private double latitude;
	private double longitude;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		loginNameTv = (TextView) findViewById(R.id.tv_name);
		connectBtn = (Button) findViewById(R.id.btn_connect);
		etLatitude = (EditText) findViewById(R.id.et_latitude);
		etLongitude = (EditText) findViewById(R.id.et_longitude);
		locationBtn = (Button) findViewById(R.id.btn_location);
		btnGo = (Button) findViewById(R.id.btn_go);
		mListView = (ListView) findViewById(R.id.lv_places);
		queryEt = (EditText) findViewById(R.id.et_query);
		queryBtn = (Button) findViewById(R.id.btn_query);

		// 初始化Foursquare
		fsqApp = new FoursquareApp(this, ApiInfo.CLIENT_ID,
				ApiInfo.CLIENT_SECRET);
		mAdapter = new NearbyAdapter(this);
		mNearbyList = new ArrayList<FsqVenue>();
		mProgress = new ProgressDialog(this);

		mProgress.setMessage("Loading data ...");

		// 判断是否登录，如果已经登陆，把登录名称写到TextView中
		if (fsqApp.hasAccessToken())
			loginNameTv.setText("Connected as " + fsqApp.getUserName());

		// 登录监听
		FsqAuthListener listener = new FsqAuthListener() {
			@Override
			public void onSuccess() {
				Toast.makeText(MainActivity.this,
						"Connected as " + fsqApp.getUserName(),
						Toast.LENGTH_SHORT).show();
				loginNameTv.setText("Connected as " + fsqApp.getUserName());
			}

			@Override
			public void onFail(String error) {
				Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT)
						.show();
			}
		};

		fsqApp.setListener(listener);

		// 用户连接
		connectBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				fsqApp.authorize();
			}
		});

		// 附近
		btnGo.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				String latitude = etLatitude.getText().toString();
				String longitude = etLongitude.getText().toString();

				if (latitude.equals("") || longitude.equals("")) {
					Toast.makeText(MainActivity.this,
							"Latitude or longitude is empty",
							Toast.LENGTH_SHORT).show();
					return;
				}

				double lat = Double.valueOf(latitude);
				double lon = Double.valueOf(longitude);

				loadNearbyPlaces(lat, lon);
			}
		});

		// 查询
		queryBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// 首先，检查当前是否有坐标
				String latitude = etLatitude.getText().toString();
				String longitude = etLongitude.getText().toString();

				if (latitude.equals("") || longitude.equals("")) {
					Toast.makeText(MainActivity.this,
							"Latitude or longitude is empty",
							Toast.LENGTH_SHORT).show();
					return;
				}

				double lat = Double.valueOf(latitude);
				double lon = Double.valueOf(longitude);
				// 检查当前是否有查询文字(貌似没有也可以？)
				String queryStr = queryEt.getText().toString();
				// 查询
				loadQueryPlaces(lat, lon, queryStr);
			}
		});

		// Check-in
		mListView.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				FsqVenue venue = mNearbyList.get(arg2);
				if (venue != null) {
					Intent intent = new Intent();
					intent.setClass(MainActivity.this, CheckinActivity.class);
					Bundle bundle = new Bundle();
					bundle.putStringArray(
							"BUNDLE_VENUE",
							new String[] { venue.name, venue.id,
									venue.location.getLatitude() + "",
									venue.location.getLongitude() + "",
									venue.address, venue.categoryName,
									venue.distance + "m", venue.herenow + "" });

					// bundle.putSerializable("BUNDLE_VENUE", venue);
					intent.putExtras(bundle);
					startActivity(intent);
				}
			}
		});

		// 初始化位置
		initLocation();
		// 取得经纬度
		locationBtn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				getLocation();
			}
		});
	}

	private void initLocation() {
		// 获取LocationManager服务
		locationManager = (LocationManager) this
				.getSystemService(Context.LOCATION_SERVICE);
		if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
			locationListener = new LocationListener() {

				@Override
				public void onLocationChanged(Location location) {
					// if (location != null) {
					// Log.e("Map",
					// "Location changed : Lat: "
					// + location.getLatitude() + " Lng: "
					// + location.getLongitude());
					// etLatitude.setText(location.getLatitude() + "");
					// etLongitude.setText(location.getLongitude() + "");
					// }
				}

				@Override
				public void onStatusChanged(String provider, int status,
						Bundle extras) {
					// TODO Auto-generated method stub

				}

				@Override
				public void onProviderEnabled(String provider) {
					// TODO Auto-generated method stub

				}

				@Override
				public void onProviderDisabled(String provider) {
					// TODO Auto-generated method stub

				}
			};
			// Location location = locationManager
			// .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			// if (location != null) {
			// latitude = location.getLatitude(); // 经度
			// longitude = location.getLongitude(); // 纬度
			// }
			// locationManager.removeUpdates(locationListener);
		}
	}

	private void getLocation() {
		if (locationListener != null && locationManager != null) {
			// 更新时间，1000毫秒---------->更改时长
			locationManager.removeUpdates(locationListener);
			locationManager
					.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
							1000, 0, locationListener);
			location = locationManager
					.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			if (location != null) {
				Log.e("Map",
						"Location changed : Lat: " + location.getLatitude()
								+ " Lng: " + location.getLongitude());
				etLatitude.setText(location.getLatitude() + "");
				etLongitude.setText(location.getLongitude() + "");
			}
		}
	}

	private void loadQueryPlaces(final double latitude, final double longitude,
			final String queryStr) {
		mProgress.show();

		new Thread() {
			@Override
			public void run() {
				int what = 2;

				try {
					mNearbyList = fsqApp.getNearBy(latitude, longitude,
							queryStr);
				} catch (Exception e) {
					what = 1;
					e.printStackTrace();
				}
				mHandler.sendMessage(mHandler.obtainMessage(what));
			}
		}.start();
	}

	private void loadNearbyPlaces(final double latitude, final double longitude) {
		mProgress.show();

		new Thread() {
			@Override
			public void run() {
				int what = 0;

				try {
					mNearbyList = fsqApp.getNearby(latitude, longitude);
				} catch (Exception e) {
					what = 1;
					e.printStackTrace();
				}

				mHandler.sendMessage(mHandler.obtainMessage(what));
			}
		}.start();
	}

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			mProgress.dismiss();

			if (msg.what == 0) {
				if (mNearbyList.size() == 0) {
					Toast.makeText(MainActivity.this,
							"No nearby places available", Toast.LENGTH_SHORT)
							.show();
					// return;
				}
				// mAdapter.setData(mNearbyList);
				// mListView.setAdapter(mAdapter);
			} else if (msg.what == 1) {
				Toast.makeText(MainActivity.this,
						"Failed to load nearby places", Toast.LENGTH_SHORT)
						.show();
			} else if (msg.what == 2) {
				if (mNearbyList.size() == 0) {
					Toast.makeText(MainActivity.this,
							"No nearby places available", Toast.LENGTH_SHORT)
							.show();
					// return;
				}

				// mAdapter.setData(mNearbyList);
				// mListView.setAdapter(mAdapter);
			} else {
				Toast.makeText(MainActivity.this,
						"Something Failed in load Foursquare.",
						Toast.LENGTH_SHORT).show();
			}

			mAdapter.setData(mNearbyList);
			mListView.setAdapter(mAdapter);
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		if (locationManager != null && locationListener != null) {
			locationManager.removeUpdates(locationListener);
		}
		super.onDestroy();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		if (locationManager != null && locationListener != null) {
			locationManager.removeUpdates(locationListener);
		}
		super.onPause();
	}

}
