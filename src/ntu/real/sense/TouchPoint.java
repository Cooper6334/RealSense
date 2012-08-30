package ntu.real.sense;

public class TouchPoint {
	float tx;
	float ty;
	float radius;
	boolean isTouch = false;

	TouchPoint(float r) {
		radius = r;
	}

	void setTouch(float x, float y) {
		isTouch = true;
		String s = "123";
		String s2 = new String("456");
		s2 = s;
		tx = x;
		ty = y;

	}

	double moveTouch(float x, float y) {
		if ((x - tx) * (x - tx) + (y - ty) * (y - ty) < radius * radius) {
			return -1;
		}

		double deg = ((-Math.atan2(x - tx, y - ty) + Math.PI) / Math.PI * 180 + 180) % 360;
		return deg;
	}

	boolean removeTouch(float x, float y) {
		isTouch = false;
		if ((x - tx) * (x - tx) + (y - ty) * (y - ty) < radius * radius) {
			return false;
		}
		return true;
	}

}
