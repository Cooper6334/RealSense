package ntu.real.sense;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ntu.real.sense.RealsenseGallery.MyImage;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
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
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

public class ServerActivity extends Activity implements SensorEventListener {

	RealSurface surface;
	TextView tv;

	ServerAgent msa = Global.mServerAgent;
	SensorManager sensorManager;

	int users;
	String[] userName = { "Allen", "Bob", "Cooper", "David", "Eric" };
	int[] userColor = { Color.BLACK, Color.BLUE, Color.GREEN, Color.GRAY,
			Color.YELLOW };
	int degs[];
	int sId;
	Thread[] readClientThread;

	List<String> pics = new ArrayList<String>();
	List<TableRow> TRs = new ArrayList<TableRow>();
	List<MyImage> IBs = new ArrayList<MyImage>();
	Bitmap bmp;
	int currentRowNum = -1;

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
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		// setContentView(R.layout.surface);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		Global.flagIsPlaying = true;

		ScrollView sv = new ScrollView(this);
		TableLayout layout = new TableLayout(this);

		this.addContentView(sv, new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.WRAP_CONTENT));
		sv.addView(layout, 0, new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));

		pics = readSDCard();

		for (int i = 0; i < pics.size(); i++) {
			if (i / 3 > currentRowNum) {
				currentRowNum++;
				TRs.add(new TableRow(this));
				layout.addView(TRs.get(currentRowNum), new LayoutParams(
						LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			}
			IBs.add(new MyImage(this, i));
			bmp = decodeBitmap(pics.get(pics.size() - i - 1));
			// IBs.get(i).setOnLongClickListener(this);
			// IBs.get(i).setOnTouchListener(this);
			IBs.get(i).setImageBitmap(bmp);
			TRs.get(currentRowNum).addView(IBs.get(i), 200, 200);
		}

		sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		List<Sensor> sensors = sensorManager
				.getSensorList(Sensor.TYPE_ORIENTATION);
		if (sensors.size() > 0) {
			sensorManager.registerListener(this, sensors.get(0),
					SensorManager.SENSOR_DELAY_NORMAL);
		}

		// setContentView(R.layout.activity_main);

		// surface = (SurfaceView) findViewById(R.id.surfaceView1);
		//
		// tv = (TextView) findViewById(R.id.textView1);
		surface = new RealSurface(this);
		surface.setZOrderOnTop(true);
		SurfaceHolder holder = surface.getHolder();
		holder.setFormat(PixelFormat.TRANSPARENT);

		tv = new TextView(this);

		this.addContentView(surface, new LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT));

		// this.addContentView(tv, new LayoutParams(LayoutParams.FILL_PARENT,
		// LayoutParams.WRAP_CONTENT));

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

	private List<String> readSDCard() {
		List<String> tFileList = new ArrayList<String>();

		// It have to be matched with the directory in SDCard
		File f = new File(
				Environment
						.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
						+ "/");
		File[] files = f.listFiles();

		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			/*
			 * It's assumed that all file in the path are in supported type
			 */
			if (file.isDirectory() && !file.toString().endsWith(".thumbnails")) {
				File[] moreFiles = file.listFiles();
				for (int j = 0; j < moreFiles.length; j++) {
					File moreFile = moreFiles[j];
					tFileList.add(moreFile.getPath());
					if (tFileList.size() > 10) {
						return tFileList;
					}
				}
			}
			if (!file.isDirectory())
				tFileList.add(file.getPath());
		}

		return tFileList;
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

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		if (!surface.flagLongTouch) {
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
		tv.setText("" + surface.myDeg);

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
	class MyImage extends ImageButton {
		int id;

		public MyImage(Context context, int i) {
			super(context);
			id = i;
			// TODO Auto-generated constructor stub
		}

		@Override
		public boolean onTouchEvent(MotionEvent e) {
			// if (e.getAction() != MotionEvent.ACTION_MOVE) {
			// Log.e("ges", id + ":" + e.getAction());
			// }
			// if (e.getAction() == MotionEvent.ACTION_UP) {
			// Toast.makeText(RealsenseGallery.this, "aaa" + id,
			// Toast.LENGTH_LONG).show();
			// }
			// if (e.getAction() == MotionEvent.ACTION_CANCEL) {
			// return false;
			// }
			Toast.makeText(ServerActivity.this, "test", Toast.LENGTH_LONG)
					.show();
			return true;
		}

	}
}
