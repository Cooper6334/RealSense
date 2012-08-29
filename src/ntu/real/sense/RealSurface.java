package ntu.real.sense;

import java.util.ArrayList;

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

public class RealSurface extends SurfaceView {
	boolean flagTouchUp = false;
	boolean flagLongTouch = false;
	float px, py;
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

	void drawView(ArrayList<Target> target, double myDeg) {

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
		if (!flagLongTouch) {
			px = e.getX();
			py = e.getY();
		}
		switch (e.getAction()) {
		case MotionEvent.ACTION_DOWN:
			h.sendEmptyMessageDelayed(0x101, 500);
			flagTouchUp = false;
			break;
		case MotionEvent.ACTION_UP:
			flagTouchUp = true;
			flagLongTouch = false;
			break;
		}
		return true;
	}
}
