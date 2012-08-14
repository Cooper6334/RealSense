package ntu.real.sense;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.os.Bundle;
import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.app.NavUtils;

public class MainActivity extends Activity {
	ArrayList<Target> target = new ArrayList<Target>();
	Set<Target> selected = new HashSet<Target>();

	SurfaceView surface;
	TextView tv;
	TouchPoint tp = new TouchPoint();
	boolean flagIsRunning = true;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.surface);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		surface = (SurfaceView) findViewById(R.id.surfaceView1);

		tv = (TextView) findViewById(R.id.textView1);
		target.add(new Target("Allen", 90, Color.YELLOW));
		target.add(new Target("Bob", 180, Color.GREEN));
		target.add(new Target("Cooper", 270, Color.BLUE));

		ImageButton btn = (ImageButton) findViewById(R.id.imageButton1);

		btn = new ImageButton(this) {
			@Override
			public boolean onTouchEvent(MotionEvent e) {
				return false;
			}
		};
		btn.setOnLongClickListener(new ImageButton.OnLongClickListener() {

			@Override
			public boolean onLongClick(View v) {
				// TODO Auto-generated method stub
				Toast.makeText(MainActivity.this, "LongClick",
						Toast.LENGTH_LONG).show();
				return false;
			}
		});

		new Thread(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				while (flagIsRunning) {
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
	public boolean onTouchEvent(MotionEvent e) {

		switch (e.getAction()) {
		case MotionEvent.ACTION_DOWN:
			tv.setText("");
			tp.setTouch(e.getX(), e.getY());
			break;
		case MotionEvent.ACTION_MOVE:
			double deg = tp.moveTouch(e.getX(), e.getY());

			if (deg == -1) {
				selected.clear();
			} else {
				for (Target t : target) {
					if (t.degree - deg < 30 && t.degree - deg > -30) {
						selected.add(t);
					}
				}
			}
			break;
		case MotionEvent.ACTION_UP:
			// double deg = tp.removeTouch(e.getX(), e.getY());
			Toast.makeText(MainActivity.this, "Touch UP", Toast.LENGTH_LONG)
					.show();
			boolean inrange = tp.removeTouch(e.getX(), e.getY());
			if (!inrange) {
				tv.setText("cancel");
			} else {
				if (selected.size() == 0) {
					tv.setText("No target");
				} else {
					String s = "Send to:";
					for (Target t : selected) {
						s = s + t.name + ",";
					}
					tv.setText(s);
				}

				// Toast.makeText(this, (int)deg + "",
				// Toast.LENGTH_SHORT).show();
			}
		}

		return true;
	}

	void drawView() {

		SurfaceHolder holder = surface.getHolder();
		Canvas canvas = holder.lockCanvas();

		if (canvas != null) {
			canvas.drawColor(Color.WHITE);
			if (tp.isTouch) {
				Paint p = new Paint();
				p.setColor(Color.RED);
				// 除去title bar跟notification bar的高度
				canvas.drawCircle(tp.tx, tp.ty, 50, p);

				for (Target t : target) {

					Paint p3 = new Paint();
					p3.setColor(t.color);
					p3.setTextSize(14);

					RectF oval = new RectF();
					oval.left = tp.tx - 50;
					oval.top = tp.ty - 50;
					oval.right = tp.tx + 50;
					oval.bottom = tp.ty + 50;
					canvas.drawArc(oval, t.degree + 60, 60, true, p3);

					double ox = 70 * Math.cos((t.degree + 90) / 180 * Math.PI);
					double oy = 70 * Math.sin((t.degree + 90) / 180 * Math.PI);

					canvas.drawText(t.name, (float) (tp.tx + ox) - 15,
							(float) (tp.ty + oy), p3);
				}

				Paint p2 = new Paint();
				p2.setColor(Color.WHITE);
				canvas.drawCircle(tp.tx, tp.ty, 45, p2);
			}
			holder.unlockCanvasAndPost(canvas);
		}
	}
}
