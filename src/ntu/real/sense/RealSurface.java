package ntu.real.sense;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.PorterDuff.Mode;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

public class RealSurface extends SurfaceView {
	int selectedPhoto;
	boolean flagTouchUp = false;
	boolean flagLongTouch = false;
	boolean flagCanSend = false;
	boolean flagClick = false;
	int displayWidth = 480;
	int displayHeight = 800;
	int myDeg;
	int showMyDeg;
	int radius = 150;
	float px, py;
	SurfaceHolder holder;
	ArrayList<Target> target = new ArrayList<Target>();
	ArrayList<Target> showTarget = new ArrayList<Target>();
	Set<Target> selected = new HashSet<Target>();
	TouchPoint tp = new TouchPoint(radius);

	Handler h = new Handler() {
		@Override
		public void handleMessage(Message m) {
			switch (m.what) {
			case 0x101:
				if (!flagTouchUp) {
					showTarget.clear();
					for (Target t : target) {
						showTarget.add(t.clone());
					}
					showMyDeg = myDeg;
					flagLongTouch = true;
				}
				flagTouchUp = false;
				break;

			}
		}
	};

	public RealSurface(Context context) {
		super(context);
		setZOrderOnTop(true);
		holder = getHolder();
		holder.setFormat(PixelFormat.TRANSPARENT);
		// TODO Auto-generated constructor stub
	}
	
	public RealSurface(Context context, int width, int height) {
		super(context);
		setZOrderOnTop(true);
		holder = getHolder();
		holder.setFormat(PixelFormat.TRANSPARENT);
		displayWidth = width;
		displayHeight = height;
		radius = radius * displayWidth / 768;
		// TODO Auto-generated constructor stub
	}

	void drawView() {

		Canvas canvas = holder.lockCanvas();

		if (canvas != null) {
			// canvas.drawColor(Color.argb(0, 0, 0, 0));
			canvas.drawColor(Color.TRANSPARENT, Mode.CLEAR);

			if (flagLongTouch) {

				Paint p2 = new Paint();
				p2.setColor(Color.WHITE);
				canvas.drawCircle(px, py, radius * 1.5f, p2);

				Paint p = new Paint();
				p.setColor(Color.RED);
				// 除去title bar跟notification bar的高度
				canvas.drawCircle(px, py, radius, p);

				for (Target t : showTarget) {

					float deg = (int) (t.degree - showMyDeg) % 360;
					Paint p3 = new Paint();
					p3.setColor(t.color);
					p3.setTextSize(32);

					RectF oval = new RectF();
					oval.left = px - radius;
					oval.top = py - radius;
					oval.right = px + radius;
					oval.bottom = py + radius;
					canvas.drawArc(oval, deg + 60, 60, true, p3);

					Log.e("deg", deg + "");

					double ox = 200 * Math.cos((deg + 90) / 180 * Math.PI);
					double oy = 200 * Math.sin((deg + 90) / 180 * Math.PI);

					canvas.drawText(t.name, (float) (px + ox) - 50,
							(float) (py + oy), p3);
				}

				canvas.drawCircle(px, py, radius - 5, p2);
			}
			holder.unlockCanvasAndPost(canvas);
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent e) {
		Log.e("sur", "touch");
		if (!flagLongTouch) {
			px = e.getX();
			py = e.getY();
			tp.setTouch(e.getX(), e.getY());
			
		}
		switch (e.getAction()) {
		case MotionEvent.ACTION_DOWN:
			h.removeMessages(0x101);
			h.sendEmptyMessageDelayed(0x101, 500);
			flagTouchUp = false;
			return true;
		case MotionEvent.ACTION_MOVE:
			Log.e("tar", "  ");
			if (flagLongTouch) {
				double deg = tp.moveTouch(e.getX(), e.getY());

				if (deg == -1) {
					selected.clear();
				} else {
					for (Target t : showTarget) {

						float td = t.degree - showMyDeg;
						while (td < 0) {
							td += 360;
						}
						Log.e("tar", t.name + ":" + td + " " + deg);
						if (td - deg < 30 && td - deg > -30) {
							selected.add(t);
						}
					}
				}
			}
			break;
		case MotionEvent.ACTION_UP:
			flagTouchUp = true;
			if (flagLongTouch) {
				flagLongTouch = false;

				boolean inrange = tp.removeTouch(e.getX(), e.getY());
				if (inrange && selected.size() > 0) {

					flagCanSend = true;
				}
				// else {
				// Toast.makeText(this.getContext(), "not in range",
				// Toast.LENGTH_LONG).show();
				// }
			} else {
				flagClick = true;
			}
			return false;
		}
		return true;
	}
}
