package ntu.real.sense;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class ServerActivity extends Activity implements SensorEventListener {

	String[] userName = { "Allen", "Bob", "Cooper", "David", "Eric" };
	int[] userColor = { Color.BLACK, Color.BLUE, Color.GREEN, Color.GRAY,
			Color.YELLOW };

	RealSurface surface;
	RelativeLayout layout;
	int CurrentButtonNumber = 0; // CurrentButtonNumber流水號 設定物件ID

	ServerAgent msa = Global.mServerAgent;
	SensorManager sensorManager;

	int users;
	int degs[];
	int sId;
	Thread[] readClientThread;

	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message m) {
			switch (m.what) {
			// 改變角度時廣播給所有client
			case 0x101:

				surface.target.get(m.arg1).degree = m.arg2;
				msa.writeAll("setdeg");
				msa.writeAll("" + m.arg1);
				msa.writeAll("" + m.arg2);
				break;

			// 點擊
			case 0x102:
				Toast.makeText(ServerActivity.this, "click", Toast.LENGTH_SHORT)
						.show();
				break;

			// 收到傳遞照片的訊息
			case 0x103:
				String show = (String) m.obj;
				Toast.makeText(ServerActivity.this, show, Toast.LENGTH_SHORT)
						.show();
				break;
			}

		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 隱藏title bar&notifiaction bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// 設定顯示照片的layout
		layout = new RelativeLayout(this);
		layout.setBackgroundColor(Color.BLACK);
		setContentView(layout);
		// 讀取照片
		ListAllPath demoTest = new ListAllPath();
		File rootFile = new File("/sdcard/DCIM");
		demoTest.print(rootFile, 0);

		RelativeLayout RL_temp = new RelativeLayout(this);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.WRAP_CONTENT,
				RelativeLayout.LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
		RL_temp.setLayoutParams(params);
		layout.addView(RL_temp);
		// 注意顯示太多照片會out of memory
		int index = demoTest.file_list.size();
		if (index > 13) {
			index = 13;
		}

		for (int i = 1; i < index; i++) {

			Log.e("圖片網址：", demoTest.file_list.get(i));
			Bitmap bitmap = decodeBitmap(demoTest.file_list.get(i));

			ImageButton image_temp = new ImageButton(this);
			image_temp.setImageBitmap(bitmap);
			image_temp.setBackgroundColor(Color.BLUE);
			Log.e("oriID", Integer.toString(image_temp.getId()));
			image_temp.setId(i); // ID不能是零，不然會爛掉！
			Log.e("newID", Integer.toString(image_temp.getId()));
			image_temp.setLayoutParams(params);
			params = new RelativeLayout.LayoutParams(180, 180);
			params.setMargins(15, 15, 15, 15);
			if (i == 1) {

			}
			if (i > 3) {
				Log.e("在誰的下面：", Integer.toString(i - 3));
				params.addRule(RelativeLayout.BELOW, (i - 3));
			}
			if (i % 3 != 1) {// 非列首的條件，要margin
				Log.e("在誰的右邊：", Integer.toString(i - 1));
				params.addRule(RelativeLayout.RIGHT_OF, (i - 1));
			}

			image_temp.setLayoutParams(params);
			RL_temp.addView(image_temp);

		}

		Global.flagIsPlaying = true;

		// 註冊orientation sensor
		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		List<Sensor> sensors = sensorManager
				.getSensorList(Sensor.TYPE_ORIENTATION);
		if (sensors.size() > 0) {
			sensorManager.registerListener(this, sensors.get(0),
					SensorManager.SENSOR_DELAY_NORMAL);
		}

		// 加入RealSense
		surface = new RealSurface(this);
		this.addContentView(surface, new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));

		// 加入使用者名單到RealSense
		sId = msa.getCount();
		users = sId + 1;

		for (int i = 0; i < users; i++) {
			surface.target.add(new Target(userName[i], 0, userColor[i]));
		}

		degs = new int[sId + 1];

		readClientThread = new Thread[sId];
		for (int i = 0; i < sId; i++) {
			msa.writeToId("init", i);
			msa.writeToId("" + i, i);
			msa.writeToId("" + users, i);
			readClientThread[i] = new Thread(new ReadClientThread(i, handler));
			readClientThread[i].start();
		}

		// 設定繪圖與傳遞照片之Thread
		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				while (Global.flagIsPlaying) {
					surface.drawView();
					if (surface.flagCanSend) {
						surface.flagCanSend = false;
						String show = "Send to:";
						for (Target t : surface.selected) {
							show += (t.name + " ");
						}
						Message m = new Message();
						m.what = 0x103;
						m.obj = show;
						handler.sendMessage(m);
						surface.selected.clear();
					}
					if (surface.flagClick) {
						surface.flagClick = false;
						Message m = new Message();
						m.what = 0x102;
						handler.sendMessage(m);
					}

					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}).start();
	}

	@Override
	public void onPause() {
		super.onPause();
		sensorManager.unregisterListener(this);
		Global.flagIsPlaying = false;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub

		if (surface.myDeg - event.values[0] > 10
				|| event.values[0] - surface.myDeg > 10) {
			surface.myDeg = (int) event.values[0];
			Message m = new Message();
			m.what = 0x101;
			m.arg1 = sId;
			m.arg2 = (int) event.values[0];
			handler.sendMessage(m);
		}

	}

	private Bitmap decodeBitmap(String path) {
		BitmapFactory.Options op = new BitmapFactory.Options();
		op.inJustDecodeBounds = true;
		op.inSampleSize = 4;
		Bitmap bmp = BitmapFactory.decodeFile(path, op);
		op.inJustDecodeBounds = false;
		bmp = BitmapFactory.decodeFile(path, op);
		return bmp;
	}
}
