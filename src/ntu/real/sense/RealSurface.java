package ntu.real.sense;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
	boolean flagTouchUp = false;
	boolean flagLongTouch = false;
	int myDeg;
	boolean flagCanSend = false;
	float px, py;
	ArrayList<Target> target = new ArrayList<Target>();
	Set<Target> selected = new HashSet<Target>();
	TouchPoint tp = new TouchPoint();

	Handler h = new Handler() {
		@Override
		public void handleMessage(Message m) {
			switch (m.what) {
			case 0x101:
				if (!flagTouchUp) {
					flagLongTouch = true;
				}
				flagTouchUp = false;
				break;

			}
		}
	};

	public RealSurface(Context context) {
		super(context);

		// TODO Auto-generated constructor stub
	}

	void drawView() {

		SurfaceHolder holder = getHolder();
		Canvas canvas = holder.lockCanvas();

		if (canvas != null) {
			// canvas.drawColor(Color.argb(0, 0, 0, 0));
			canvas.drawColor(Color.TRANSPARENT, Mode.CLEAR);

			if (flagLongTouch) {

				Paint p2 = new Paint();
				p2.setColor(Color.WHITE);
				canvas.drawCircle(px, py, 245, p2);

				Paint p = new Paint();
				p.setColor(Color.RED);
				// 除去title bar跟notification bar的高度
				canvas.drawCircle(px, py, 150, p);

				for (Target t : target) {

					float deg = (int) (t.degree - myDeg) % 360;
					Paint p3 = new Paint();
					p3.setColor(t.color);
					p3.setTextSize(32);

					RectF oval = new RectF();
					oval.left = px - 150;
					oval.top = py - 150;
					oval.right = px + 150;
					oval.bottom = py + 150;
					canvas.drawArc(oval, deg + 60, 60, true, p3);

					Log.e("deg", deg + "");

					double ox = 200 * Math.cos((deg + 90) / 180 * Math.PI);
					double oy = 200 * Math.sin((deg + 90) / 180 * Math.PI);

					canvas.drawText(t.name, (float) (px + ox) - 50,
							(float) (py + oy), p3);
				}

				canvas.drawCircle(px, py, 145, p2);
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
					for (Target t : target) {

						float td = t.degree - myDeg;
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
				if (inrange) {

					flagCanSend = true;
				}
				// else {
				// Toast.makeText(this.getContext(), "not in range",
				// Toast.LENGTH_LONG).show();
				// }
			}
			return false;
		}
		return true;
	}
}
