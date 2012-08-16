package ntu.real.sense;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

public class ServerActivity extends Activity implements SensorEventListener {

	ArrayList<Target> target = new ArrayList<Target>();
	Set<Target> selected = new HashSet<Target>();

	SurfaceView surface;
	TextView tv;

	ServerAgent msa = Global.mServerAgent;
	SensorManager sensorManager;

	int users;
	String[] userName = { "Allen", "Bob", "Cooper", "David", "Eric" };
	int[] userColor = { Color.BLACK, Color.BLUE, Color.GREEN, Color.GRAY,
			Color.YELLOW };
	int degs[];
	int sId;
	int myDeg;
	Thread[] readClientThread;

	Handler handler = new Handler() {
		@Override
		public void handleMessage(Message m) {
			switch (m.what) {
			// 改變角度時廣播給所有client
			case 0x101:

				target.get(m.arg1).degree = m.arg2;
				msa.writeAll("setdeg");
				msa.writeAll("" + m.arg1);
				msa.writeAll("" + m.arg2);
				break;

			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.surface);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		Global.flagIsPlaying = true;

		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		List<Sensor> sensors = sensorManager
				.getSensorList(Sensor.TYPE_ORIENTATION);
		if (sensors.size() > 0) {
			sensorManager.registerListener(this, sensors.get(0),
					SensorManager.SENSOR_DELAY_NORMAL);
		}

		sId = msa.getCount();
		users = sId + 1;

		for (int i = 0; i < users; i++) {
			target.add(new Target(userName[i], 0, userColor[i]));
		}

		degs = new int[sId + 1];
		Log.e("cnt", "" + sId);
		readClientThread = new Thread[sId];
		for (int i = 0; i < sId; i++) {
			msa.writeToId("init", i);
			msa.writeToId("" + i, i);
			msa.writeToId("" + users, i);
			readClientThread[i] = new Thread(new ReadClientThread(i, handler));
			readClientThread[i].start();
		}

		surface = (SurfaceView) findViewById(R.id.surfaceView1);

		tv = (TextView) findViewById(R.id.textView1);

		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				while (Global.flagIsPlaying) {
					drawView();
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}).start();
		// setContentView(R.layout.activity_main);
	}

	@Override
	public void onPause() {
		super.onPause();
		sensorManager.unregisterListener(this);
		Global.flagIsPlaying = false;
	}

	void drawView() {

		SurfaceHolder holder = surface.getHolder();
		Canvas canvas = holder.lockCanvas();

		if (canvas != null) {
			canvas.drawColor(Color.WHITE);

			Paint p = new Paint();
			p.setColor(Color.RED);
			// 除去title bar跟notification bar的高度
			canvas.drawCircle(300, 500, 150, p);

			for (Target t : target) {

				float deg = (int) (t.degree - myDeg) % 360;
				Paint p3 = new Paint();
				p3.setColor(t.color);
				p3.setTextSize(32);

				RectF oval = new RectF();
				oval.left = 150;
				oval.top = 350;
				oval.right = 450;
				oval.bottom = 650;
				canvas.drawArc(oval, deg + 60, 60, true, p3);

				Log.e("deg", deg + "");

				double ox = 200 * Math.cos((deg + 90) / 180 * Math.PI);
				double oy = 200 * Math.sin((deg + 90) / 180 * Math.PI);

				canvas.drawText(t.name, (float) (300 + ox) - 50,
						(float) (500 + oy), p3);
			}

			Paint p2 = new Paint();
			p2.setColor(Color.WHITE);
			canvas.drawCircle(300, 500, 145, p2);

			holder.unlockCanvasAndPost(canvas);
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		if (myDeg - event.values[0] > 10 || event.values[0] - myDeg > 10) {
			myDeg = (int) event.values[0];
			Message m = new Message();
			m.what = 0x101;
			m.arg1 = sId;
			m.arg2 = (int) event.values[0];
			handler.sendMessage(m);
		}
		tv.setText("" + myDeg);

	}

	/*
	 * @Override public boolean onTouchEvent(MotionEvent e) {
	 * 
	 * switch (e.getAction()) { case MotionEvent.ACTION_DOWN: tv.setText("");
	 * tp.setTouch(e.getX(), e.getY()); break; case MotionEvent.ACTION_MOVE:
	 * double deg = tp.moveTouch(e.getX(), e.getY());
	 * 
	 * if (deg == -1) { selected.clear(); } else { for (Target t : target) { if
	 * (t.degree - deg < 30 && t.degree - deg > -30) { selected.add(t); } } }
	 * break; case MotionEvent.ACTION_UP: // double deg =
	 * tp.removeTouch(e.getX(), e.getY()); Toast.makeText(MainActivity.this,
	 * "Touch UP", Toast.LENGTH_LONG) .show(); boolean inrange =
	 * tp.removeTouch(e.getX(), e.getY()); if (!inrange) { tv.setText("cancel");
	 * } else { if (selected.size() == 0) { tv.setText("No target"); } else {
	 * String s = "Send to:"; for (Target t : selected) { s = s + t.name + ",";
	 * } tv.setText(s); }
	 * 
	 * // Toast.makeText(this, (int)deg + "", // Toast.LENGTH_SHORT).show(); } }
	 * 
	 * return true; }
	 */

}
