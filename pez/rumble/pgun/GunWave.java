package pez.rumble.pgun;

import pez.rumble.utils.Wave;

public class GunWave extends Wave {
	boolean hit(double diff) {
		return Math.abs(diff) < ((double)this.botWidth() / 1.6);
	}
}


class VisitsIndex implements Comparable<Object> {
	double visits;
	int index;

	public VisitsIndex(double v, int i) {
		visits = v;
		index = i;
	}

	public int compareTo(Object o) {
		VisitsIndex other = (VisitsIndex)o;
		if (other.visits < visits) {
			return -1;
		}
		else if (other.visits > visits) {
			return 1;
		}
		else {
			return 0;
		}
	}
}
