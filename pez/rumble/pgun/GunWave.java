package pez.rumble.pgun;

import pez.rumble.utils.Wave;

public class GunWave extends Wave {
	boolean hit(double diff) {
		return Math.abs(diff) < ((double)this.botWidth() / 1.5);
	}
}


class VisitsIndex implements Comparable<Object> {
	int visits;
	int index;

	public VisitsIndex(double v, int i) {
		visits = (int)(v * 100000d);
		index = i;
	}

	public int compareTo(Object o) {
		return (int)(((VisitsIndex)o).visits - visits);
	}
}
