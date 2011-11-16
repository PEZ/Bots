package pez.rumble.pgun;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import pez.rumble.utils.PUtils;

abstract public class Guessor implements Comparable<Object>, Serializable {
	static final long serialVersionUID = 7;
	transient static final int ACCEL_INDEXES = 3;
	transient static final double[] DISTANCE_SLICES = { 150, 300, 450, 600 };
	transient static final double[] DISTANCE_SLICES_FASTER = { 150, 300, 500 };
	transient static final double[] VELOCITY_SLICES = { 1, 3, 5, 7 };
	transient static final double[] VELOCITY_SLICES_FASTER = { 2, 4, 6 };
	transient static final double[] WALL_SLICES = { 0.1, 0.2, 0.3, 0.99 };
	transient static final double[] WALL_SLICES_FASTER = { 0.35, 0.99 };
	transient static final double[] WALL_SLICES_REVERSE = { 0.3 };
	transient static final double[] TIMER_SLICES = {.03, .1, .25, .55}; //{ 0.1, 0.3, 0.7, 1.2 };
	transient static final double[] TIMER_SLICES_FASTER = {.05, .35, .65}; //{ 0.1, 0.3, 0.7 };

	transient double[][][][][][] faster = new double[DISTANCE_SLICES_FASTER.length + 1][VELOCITY_SLICES_FASTER.length + 1]
			[ACCEL_INDEXES][TIMER_SLICES_FASTER.length + 1][WALL_SLICES_FASTER.length + 1][BeeWave.BINS];
	transient double[][][] distVel = new double[DISTANCE_SLICES.length + 1][VELOCITY_SLICES.length + 1][BeeWave.BINS];
	double[][][][] distWall = new double[DISTANCE_SLICES.length + 1][WALL_SLICES.length + 1]
			[WALL_SLICES_REVERSE.length + 1][BeeWave.BINS];
	double[][][][] accelWall = new double[ACCEL_INDEXES][WALL_SLICES.length + 1]
			[WALL_SLICES_REVERSE.length + 1][BeeWave.BINS];
	double[][][][][][][] slower = new double[DISTANCE_SLICES.length + 1][VELOCITY_SLICES.length + 1]
			[ACCEL_INDEXES][TIMER_SLICES.length + 1][WALL_SLICES.length + 1][WALL_SLICES_REVERSE.length + 1][BeeWave.BINS];
	double[][][][][][][] distVelWallTimers = new double[DISTANCE_SLICES.length + 1][VELOCITY_SLICES.length + 1]
			[TIMER_SLICES.length + 1][WALL_SLICES.length + 1][TIMER_SLICES.length + 1][TIMER_SLICES.length + 1][BeeWave.BINS];
	transient double[][][][] velTimers = new double[VELOCITY_SLICES_FASTER.length + 1][TIMER_SLICES.length + 1]
			[TIMER_SLICES.length + 1][BeeWave.BINS];
	transient double[][][][] accelTimers = new double[ACCEL_INDEXES][TIMER_SLICES.length + 1]
			[TIMER_SLICES.length + 1][BeeWave.BINS];
//	transient double[][][][][][][][][] all = new double[DISTANCE_SLICES.length + 1][VELOCITY_SLICES.length + 1]
//			[ACCEL_INDEXES][TIMER_SLICES.length + 1][WALL_SLICES.length + 1][WALL_SLICES_REVERSE.length + 1]
//			[TIMER_SLICES.length + 1][TIMER_SLICES.length + 1][BeeWave.BINS];

	static private long realBulletsFired;
	static private long virtualBulletsFired;
	private long realHits;
	private long virtualHits;
	private double rollingRealRating;
	private double rollingVirtualRating;
	int rounds;
	transient private int guess;

	abstract double getRollingDepth();
	abstract double getWaveWeight(BeeWave wave);

	void registerVisit(BeeWave w, Map<Guessor, Integer> guesses) {
		int index = Math.max(1, w.visitingIndex());
		updateRating(index, ((Integer)guesses.get(this)).intValue(), w);
		registerVisit(index, w);
	}

	void updateRating(int index, int guess, BeeWave w) {
		if (w.hit(guess - index)) {
			if (w.weight > 2) {
				realHits++;
				rollingRealRating = PUtils.rollingAvg(rollingRealRating, 1, 100);
			}
			virtualHits++;
			rollingVirtualRating = PUtils.rollingAvg(rollingVirtualRating, 1, 1500);
		}
		else {
			if (w.weight > 2) {
				rollingRealRating = PUtils.rollingAvg(rollingRealRating, 0, 100);
			}
			rollingVirtualRating = PUtils.rollingAvg(rollingVirtualRating, 0, 1500);
		}
	}

	double[][] buffers(BeeWave w) {
		return new double[][] {
				faster[w.distanceSegmentFaster][w.velocitySegmentFaster][w.accelSegment][w.vChangeSegmentFaster][w.wallSegmentFaster],
				distVel[w.distanceSegment][w.velocitySegment],
				distWall[w.distanceSegment][w.wallSegment][w.reverseWallSegment],
				accelWall[w.accelSegment][w.wallSegment][w.reverseWallSegment],
				accelTimers[w.accelSegment][w.sinceStationarySegment][w.sinceMaxSpeedSegment],
				velTimers[w.velocitySegmentFaster][w.sinceStationarySegment][w.sinceMaxSpeedSegment],
				slower[w.distanceSegment][w.velocitySegment][w.accelSegment][w.vChangeSegment][w.wallSegment][w.reverseWallSegment],
				distVelWallTimers[w.distanceSegment][w.velocitySegment][w.vChangeSegment][w.wallSegment][w.sinceStationarySegment][w.sinceMaxSpeedSegment],
				//all[w.distanceSegment][w.velocitySegment][w.accelSegment][w.vChangeSegment][w.wallSegment][w.reverseWallSegment][w.sinceStationarySegment][w.sinceMaxSpeedSegment],
		};
	}

	int mostVisited(BeeWave w) {
		int defaultIndex = BeeWave.MIDDLE_BIN + BeeWave.MIDDLE_BIN / 4;
		double uses = 0;
		double[][] buffers = buffers(w);
		for (int b = 0; b < buffers.length; b++) {
			uses += buffers[b][0];
		}
		if (uses < 1) {
			return defaultIndex;
		}
		List<VisitsIndex> visitRanks = new ArrayList<VisitsIndex>();
		for (int i = 1; i < BeeWave.BINS; i++) {
			double visits = 0;
			for (int b = 0; b < buffers.length; b++) {
				visits += uses * buffers[b][i] / Math.pow(Math.max(1, buffers[b][0]), 1.05);
			}
			visitRanks.add(new VisitsIndex(visits, i));
		}
		Collections.sort(visitRanks);
		return ((VisitsIndex)visitRanks.get(0)).index;
	}

	boolean shouldConsiderWave(BeeWave w) {
		return true;
	}

	void registerVisit(int index, BeeWave w) {
		if (shouldConsiderWave(w)) {
			double[][] buffers = buffers(w);
			for (int b = 0; b < buffers.length; b++) {
				buffers[b][0]++;
				for (int i = 1; i < BeeWave.BINS; i++) {
					buffers[b][i] =  (float)PUtils.rollingAvg(buffers[b][i], getWaveWeight(w) / Math.pow(Math.abs(i - index) + 1, 2), getRollingDepth());
				}
			}
		}
	}
	
	void guess(BeeWave w) {
		guess = mostVisited(w);
	}

	int guessed() {
		return guess;
	}

	static void registerFire() {
		realBulletsFired++;
	}

	static void registerVirtualFire() {
		virtualBulletsFired++;
	}

	double realRating() {
		return (double)realHits / (double)Math.max(1, realBulletsFired);
	}

	double virtualRating() {
		return (double)virtualHits / (double)Math.max(1, virtualBulletsFired);
	}

	double totalRating() {
		return 0.25 * virtualRating() + 0.75 * realRating();
	}

	private double rollingRating() {
		return 0.25 * rollingVirtualRating + 0.75 * rollingRealRating;
	}
	
	double rating() {
		return 0.6 * totalRating() + 0.4 * rollingRating();
	}
	
	public int compareTo(Object o) {
		double ratingA = this.rating();
		double ratingB = ((Guessor)o).rating();
		if (ratingA > ratingB) {
			return -1;
		}
		else if (ratingB > ratingA) {
			return 1;
		}
		return 0;
	}

	static VirtualStatsComparator getVirtualStatsComparator() {
		return new VirtualStatsComparator();
	}

	static class VirtualStatsComparator implements Comparator<Object> {
		public int compare(Object a, Object b) {
			return ((Guessor)a).compareTo(b);
		}
	}

	String name() {
		String name = this.getClass().getName();
		return name.substring(name.lastIndexOf(".") + 1);
	}

	String echoStats() {
		return name() + " r:"+ logNum(rating()) +
				" tr:" + logNum(totalRating()) +
				" vr:" + logNum(virtualRating()) +
				" rr:" + logNum(realRating()) +
				" roll_r:" + logNum(rollingRating()) +
				" roll_vr:" + logNum(rollingVirtualRating) +
				" roll_rr:" + logNum(rollingRealRating) + "\n";
	}

	static String logHeader(String tag) {
		return "Rounds " + tag + "\t" + 
		"rRating " + tag;
	}

	static String logNum(double num) {
		return java.text.NumberFormat.getNumberInstance().format(num);
	}

	String logRow() {
		return rounds + "\t" +
		logNum(realHits);
	}

	public String toString() {
		return echoStats();
	}
}

class BeeRealisor extends Guessor {
	static final long serialVersionUID = 7;

	@Override
	double getRollingDepth() {
		return 100;
	}

	@Override
	double getWaveWeight(BeeWave wave) {
		if (wave.weight < 2.0) {
			return 0.25;
		}
		return 1.0;
	}
	
	@Override
	boolean shouldConsiderWave(BeeWave wave) {
		if (wave.weight < 2.0) {
			return true;
		}
		return true;
	}
}

class BeeVirtualisor extends Guessor {
	static final long serialVersionUID = 7;

	@Override
	double getRollingDepth() {
		return 2000;
	}

	@Override
	double getWaveWeight(BeeWave wave) {
		return 1.0;
	}
}

class BeeRealForgettor extends Guessor {
	static final long serialVersionUID = 7;
	
	void registerHit(int index, BeeWave w) {
		/*
		double[][] buffers = buffers(w);
		for (int b = 0; b < buffers.length; b++) {
			buffers[b][0]++;
			for (int i = 1; i < BeeWave.BINS; i++) {
				//buffers[b][i] =  (float)PUtils.rollingAvg(buffers[b][i], index == i ? -1.0 : 0, getRollingDepth() / 2);
				buffers[b][i] =  (float)PUtils.rollingAvg(buffers[b][i], -getWaveWeight(w) / Math.pow(Math.abs(i - index) + 1, 2), getRollingDepth());
			}
		}
		*/
	}

	@Override
	double getRollingDepth() {
		return 0.8;
	}

	@Override
	double getWaveWeight(BeeWave wave) {
		if (wave.weight < 2.0) {
			return 0.25;
		}
		return 1.0;
	}
	
	@Override
	boolean shouldConsiderWave(BeeWave wave) {
		if (wave.weight < 2.0) {
			return true;
		}
		return true;
	}
}

class BeeVirtualForgettor extends Guessor {
	static final long serialVersionUID = 7;
	
	void registerHit(int index, BeeWave w) {
		/*
		double[][] buffers = buffers(w);
		for (int b = 0; b < buffers.length; b++) {
			buffers[b][0]++;
			for (int i = 1; i < BeeWave.BINS; i++) {
				//buffers[b][i] =  (float)PUtils.rollingAvg(buffers[b][i], index == i ? -1.0 : 0, getRollingDepth() / 2);
				buffers[b][i] =  (float)PUtils.rollingAvg(buffers[b][i], -getWaveWeight(w) / Math.pow(Math.abs(i - index) + 1, 2), getRollingDepth());
			}
		}
		*/
	}

	@Override
	double getRollingDepth() {
		return 0.8;
	}

	@Override
	double getWaveWeight(BeeWave wave) {
		return 1.0;
	}
}
