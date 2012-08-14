package ntu.real.sense;

import android.graphics.Color;

public class Target {
	float degree;
	String name;
	int color;

	Target(String n, float d, int c) {
		degree = d;
		name = n;
		color = c;

	}

	public boolean equals(Target t) {
		if (name.equals(t.name) && degree == t.degree) {
			return true;
		}
		return false;
	}

}
