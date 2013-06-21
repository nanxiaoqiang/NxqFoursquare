package com.nanxiaoqiang.test.nxqfoursquare;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class CheckinActivity extends Activity {
	private TextView venuenameText;
	private TextView venueidText;
	private TextView venuelatText;
	private TextView venuelogText;
	private TextView venueaddrText;
	private TextView venuecategoryText;
	private TextView venuedistanceText;
	private TextView venueherenowText;

	private CheckBox cbTwitter;
	private CheckBox cbFacebook;
	private CheckBox cbPublic;

	private EditText shoutText;

	private Button btnCheckin;

	private FoursquareApp fsqApp;

	private ProgressDialog mProgress;

	private boolean isCheckin = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.check_in);

		venuenameText = (TextView) findViewById(R.id.venue_name);
		venueidText = (TextView) findViewById(R.id.venue_id);
		venuelatText = (TextView) findViewById(R.id.venue_latitude);
		venuelogText = (TextView) findViewById(R.id.venue_longitude);
		venueaddrText = (TextView) findViewById(R.id.venue_address);
		venuecategoryText = (TextView) findViewById(R.id.venue_category);
		venuedistanceText = (TextView) findViewById(R.id.venue_distance);
		venueherenowText = (TextView) findViewById(R.id.venue_here_now);

		cbTwitter = (CheckBox) findViewById(R.id.cb_twitter);
		cbFacebook = (CheckBox) findViewById(R.id.cb_facebook);
		cbPublic = (CheckBox) findViewById(R.id.cb_public);

		shoutText = (EditText) findViewById(R.id.et_shout);

		btnCheckin = (Button) findViewById(R.id.btn_check_in);

		fsqApp = new FoursquareApp(this, ApiInfo.CLIENT_ID,
				ApiInfo.CLIENT_SECRET);

		Bundle bundle = this.getIntent().getExtras();
		if (bundle != null) {
			// venue = (FsqVenue) bundle.get("BUNDLE_VENUE");
			String[] str_array = bundle.getStringArray("BUNDLE_VENUE");
			if (str_array != null) {
				// 设置显示
				// venuenameText.setText(venue.name);
				// venueidText.setText(venue.id);
				// venuelatText.setText(venue.location.getLatitude() + "");
				// venuelogText.setText(venue.location.getLongitude() + "");
				// venueaddrText.setText(venue.address);
				// venuecategoryText.setText(venue.categoryName);
				// venuedistanceText.setText(venue.distance);
				// venueherenowText.setText(venue.herenow);
				venuenameText.setText(str_array[0]);
				venueidText.setText(str_array[1]);
				venuelatText.setText(str_array[2]);
				venuelogText.setText(str_array[3]);
				venueaddrText.setText(str_array[4]);
				venuecategoryText.setText(str_array[5]);
				venuedistanceText.setText(str_array[6]);
				venueherenowText.setText(str_array[7]);

				mProgress = new ProgressDialog(this);

				mProgress.setMessage("Loading data ...");

				btnCheckin.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						// 准备好venueId，shout，broadcast，ll，venue，oauth_token这几个提交选项
						String latitude = venuelatText.getText().toString();
						String longitude = venuelogText.getText().toString();
						double lat = Double.valueOf(latitude);
						double lon = Double.valueOf(longitude);
						String venueId = venueidText.getText().toString();
						String venue = venuenameText.getText().toString();

						String shout = shoutText.getText().toString();
						boolean isPublic = cbPublic.isChecked();
						boolean isTwitter = cbTwitter.isChecked();
						boolean isFacebook = cbFacebook.isChecked();

						String broadcast = "private";
						if (isPublic) {
							broadcast = "public";
						}
						if (isTwitter) {
							broadcast += ",twitter";
						}
						if (isFacebook) {
							broadcast += ",facebook";
						}
						// 提交
						// 没有EVENTID的判断
						// https://api.foursquare.com/v2/checkins/add?broadcast=public,twitter&eventId
						checkin(venueId, venue, shout, broadcast, lat, lon);
					}
				});
			}
		}
	}

	private void checkin(final String venueId, final String venue,
			final String shout, final String broadcast, final double latitude,
			final double longitude) {
		mProgress.show();

		new Thread() {
			@Override
			public void run() {
				int what = 0;

				try {
					boolean isCheckin = fsqApp.checkin(venueId, venue,
							broadcast, shout, latitude, longitude);
					if (!isCheckin)
						what = 1;
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
				Toast.makeText(CheckinActivity.this, "check-in success!",
						Toast.LENGTH_SHORT).show();
				CheckinActivity.this.finish();
			} else {
				Toast.makeText(CheckinActivity.this, "Failed to check-in",
						Toast.LENGTH_SHORT).show();
			}
		}
	};
}
