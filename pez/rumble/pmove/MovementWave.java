package pez.rumble.pmove;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import pez.rumble.utils.PUtils;
import pez.rumble.utils.Wave;
import pez.rumble.utils.WaveGrapher;
import robocode.AdvancedRobot;
import robocode.Bullet;

public class MovementWave extends Wave {
	private static final int FACTORS = 31;
	static final int ACCEL_INDEXES = 3;
	private static final int MIDDLE_FACTOR = (getFactors() - 1) / 2;
	static final double[] APPROACH_SLICES = { -3, 1, 3};
	static final double[] DISTANCE_SLICES = { 300, 450, 550, 650 };
	static final double[] VELOCITY_SLICES = { 1, 3, 5, 7 };
	static final double[] WALL_SLICES = { 0.15, 0.35, 0.65, 0.99 };
	static final double[] WALL_SLICES_REVERSE = { 0.15, 0.35, 0.99 };
	static final double[] TIMER_SLICES = { 0.15, 0.3, 0.7, 1.3 };
	static final int APPROACH_INDEXES = APPROACH_SLICES.length + 1;
	static final int DISTANCE_INDEXES = DISTANCE_SLICES.length + 1;
	static final int VELOCITY_INDEXES = VELOCITY_SLICES.length + 1;
	static final int TIMER_INDEXES = TIMER_SLICES.length + 1;
	static final int WALL_INDEXES = WALL_SLICES.length + 1;
	static final int WALL_INDEXES_REVERSE = WALL_SLICES_REVERSE.length + 1;

	static float[][][][][][] visitCounts;
	static float[] visitCountsFast;
	static float[][][][][] visitCountsTimerWalls;
	static float[][][][][] visitCountsTimer;
	static float[][][][][] visitCountsDistanceVelocityWalls;
	static float[][][][] visitCountsWalls;
	static float[][][][] visitCountsDVA;
	static float[][][] visitCountsVelocityAccel;
	static float[][][] visitCountsVelocityApproach;
	static float[][][] visitCountsDistanceVelocity;
	static float[][] visitCountsVelocity;
	static float[][][][][] hitCountsTimerWalls;
	static float[][][][][] hitCountsTimer;
	static float[][][][][] hitCountsDistanceVelocityWalls;
	static float[][][][] hitCountsWalls;
	static float[][][][] hitCountsDVA;
	static float[][][] hitCountsVelocityAccel;
	static float[][][] hitCountsVelocityApproach;
	static float[][][] hitCountsDistanceVelocity;
	static float[][] hitCountsVelocity;
	static float[] fastHitCounts;
	static float[] randomCounts;

	static double rangeHits;
	static double dangerForward;
	static double dangerReverse;
	static double dangerStop;
	static List<MovementWave> waves;
	static List<MovementWave> bullets;
	static List<MovementWave> surfables;
	static double hitsTaken;

	boolean isSurfable;
	long startTime;
	Butterfly floater;
	double bulletPower;
	int distanceIndex;
	int velocityIndex;
	int lastVelocityIndex;
	int accelIndex;
	int vChangeIndex;
	int wallIndex;
	int wallIndexReverse;
	int approachIndex;
	boolean visitRegistered;

	WaveGrapher grapher; // GL

	static void initStatBuffers() {
		visitCounts = new float[DISTANCE_INDEXES][VELOCITY_INDEXES][ACCEL_INDEXES][TIMER_INDEXES][WALL_INDEXES][getFactors()];
		visitCountsTimerWalls = new float[DISTANCE_INDEXES][VELOCITY_INDEXES][TIMER_INDEXES][WALL_INDEXES][getFactors()];
		visitCountsTimer = new float[DISTANCE_INDEXES][VELOCITY_INDEXES][ACCEL_INDEXES][TIMER_INDEXES][getFactors()];
		visitCountsDistanceVelocityWalls = new float[DISTANCE_INDEXES][VELOCITY_INDEXES][ACCEL_INDEXES][WALL_INDEXES][getFactors()];
		visitCountsDVA = new float[DISTANCE_INDEXES][VELOCITY_INDEXES][ACCEL_INDEXES][getFactors()];
		visitCountsWalls = new float[VELOCITY_INDEXES][ACCEL_INDEXES][WALL_INDEXES][getFactors()];
		visitCountsVelocityAccel = new float[VELOCITY_INDEXES][ACCEL_INDEXES][getFactors()];
		visitCountsVelocityApproach = new float[VELOCITY_INDEXES][APPROACH_INDEXES][getFactors()];
		visitCountsDistanceVelocity = new float[DISTANCE_INDEXES][VELOCITY_INDEXES][getFactors()];
		visitCountsVelocity = new float[VELOCITY_INDEXES][getFactors()];
		visitCountsFast = new float[getFactors()];
		hitCountsTimerWalls = new float[DISTANCE_INDEXES][VELOCITY_INDEXES][TIMER_INDEXES][WALL_INDEXES][getFactors()];
		hitCountsTimer = new float[DISTANCE_INDEXES][VELOCITY_INDEXES][ACCEL_INDEXES][TIMER_INDEXES][getFactors()];
		hitCountsDistanceVelocityWalls = new float[DISTANCE_INDEXES][VELOCITY_INDEXES][ACCEL_INDEXES][WALL_INDEXES][getFactors()];
		hitCountsDVA = new float[DISTANCE_INDEXES][VELOCITY_INDEXES][ACCEL_INDEXES][getFactors()];
		hitCountsWalls = new float[VELOCITY_INDEXES][ACCEL_INDEXES][WALL_INDEXES][getFactors()];
		hitCountsVelocityAccel = new float[VELOCITY_INDEXES][ACCEL_INDEXES][getFactors()];
		hitCountsVelocityApproach = new float[VELOCITY_INDEXES][APPROACH_INDEXES][getFactors()];
		hitCountsDistanceVelocity = new float[DISTANCE_INDEXES][VELOCITY_INDEXES][getFactors()];
		hitCountsVelocity = new float[VELOCITY_INDEXES][getFactors()];
		fastHitCounts = new float[getFactors()];
		fastHitCounts[getMiddleFactor()] = 50;
		randomCounts = new float[getFactors()];
	}

	static void init() {
		if (fastHitCounts == null) {
			initStatBuffers();
		}
		waves = new ArrayList<MovementWave>();
		bullets = new ArrayList<MovementWave>();
		surfables = new ArrayList<MovementWave>();
	}

	static void reset() {
		dangerForward = 0;
		dangerReverse = 0;
		dangerStop = 0;
	}

	public MovementWave(AdvancedRobot robot, Butterfly floater) {
		init(robot, getFactors());
		this.floater = floater;
	}

	static void updateWaves(AdvancedRobot robot) {
		List<MovementWave> reap = new ArrayList<MovementWave>();
		for (int i = 0, n = waves.size(); i < n; i++) {
			MovementWave wave = (MovementWave)waves.get(i);
			wave.setDistanceFromGun((robot.getTime() - wave.startTime) * wave.getBulletVelocity());
			if (wave.passed(2)) {
				if (!wave.visitRegistered) {
					if (wave.isSurfable) {
						wave.registerVisit(2, 0.7);
					}
					else {
						wave.registerVisit(0.4, 3);						
					}
					wave.visitRegistered = true;
				}
			}
			if (wave.passed(wave.getBulletVelocity() * 2)) {
				surfables.remove(wave);
				if (wave.grapher != null) { // GL
					wave.grapher.remove(); // GL
				} // GL
			}
			if (wave.passed(-15)) {
				reap.add(wave);
				bullets.remove(wave);
			}
			if (wave.grapher != null) { // GL
				wave.grapher.drawWave(); // GL
			} // GL
		}
		for (int i = 0, n = reap.size(); i < n; i++) {
			waves.remove(reap.get(i));
			if (reap.get(i).grapher != null) { // GL
				reap.get(i).grapher.remove(); // GL
			} // GL

		}
	}

	void registerVisit(double weight, double depth) {
		int index = visitingIndex();
		float[] visits = visitCounts[distanceIndex][velocityIndex][accelIndex][vChangeIndex][wallIndex];
		float[] visitsFast = visitCountsFast;
		float[] visitsTimerWalls = visitCountsTimerWalls[distanceIndex][velocityIndex][vChangeIndex][wallIndex];
		float[] visitsTimer = visitCountsTimer[distanceIndex][velocityIndex][accelIndex][vChangeIndex];
		float[] visitsDistanceVelocityWalls = visitCountsDistanceVelocityWalls[distanceIndex][velocityIndex][accelIndex][wallIndex];
		float[] visitsWalls = visitCountsWalls[velocityIndex][accelIndex][wallIndex];
		float[] visitsDVA = visitCountsDVA[distanceIndex][velocityIndex][accelIndex];
		float[] visitsVelocityAccel = visitCountsVelocityAccel[velocityIndex][accelIndex];
		float[] visitsVelocityApproach = visitCountsVelocityApproach[velocityIndex][approachIndex];
		float[] visitsDistanceVelocity = visitCountsDistanceVelocity[distanceIndex][velocityIndex];
		float[] visitsVelocity = visitCountsVelocity[velocityIndex];
		registerHit(visits, index, weight, depth);
		registerHit(visitsFast, index, weight, depth);
		registerHit(visitsTimerWalls, index, weight, depth);
		registerHit(visitsTimer, index, weight, depth);
		registerHit(visitsDistanceVelocityWalls, index, weight, depth);
		registerHit(visitsWalls, index, weight, depth);
		registerHit(visitsDVA, index, weight, depth);
		registerHit(visitsVelocityAccel, index, weight, depth);
		registerHit(visitsVelocityApproach, index, weight, depth);
		registerHit(visitsDistanceVelocity, index, weight, depth);
		registerHit(visitsVelocity, index, weight, depth);
		registerHit(randomCounts, (int)(Math.random() * (getFactors() - 1) + 1), PUtils.minMax(Math.pow(hitRate() * 2.1, 2), 0, 60), 10.0);
	}

	static void registerHit(Bullet bullet) {
		Point2D bulletLocation = new Point2D.Double(bullet.getX(), bullet.getY());
		MovementWave wave = (MovementWave)Wave.findClosest(bullets, bulletLocation, bullet.getVelocity());
		if (wave != null) {
			wave.registerHit(bullet.getHeadingRadians());
		}
	}

	void registerHit(double bearing) {
		registerHit(visitingIndex(bearing));
	}

	void registerHit(Point2D hitLocation) {
		registerHit(visitingIndex(hitLocation));
	}

	void registerHit(float[] buffer, int index, double weight, double depth) {
		for (int i = 0; i < getFactors(); i++) {
			buffer[i] =  (float)PUtils.rollingAvg(buffer[i], weight / Math.pow(Math.abs(i - index) + 1, 2.2), depth);
			//buffer[i] =  (float)PUtils.rollingAvg(buffer[i], Math.pow(Math.abs(i - index) + 1, 1.5), depth);
		}
	}

	void registerHit(int index) {
		float[] hitsTimerWalls = hitCountsTimerWalls[distanceIndex][velocityIndex][vChangeIndex][wallIndex];
		float[] hitsTimer = hitCountsTimer[distanceIndex][velocityIndex][accelIndex][vChangeIndex];
		float[] hitsDistanceVelocityWalls = hitCountsDistanceVelocityWalls[distanceIndex][velocityIndex][accelIndex][wallIndex];
		float[] hitsWalls = hitCountsWalls[velocityIndex][accelIndex][wallIndex];
		float[] hitsDVA = hitCountsDVA[distanceIndex][velocityIndex][accelIndex];
		float[] hitsVelocityAccel = hitCountsVelocityAccel[velocityIndex][accelIndex];
		float[] hitsVelocityApproach = hitCountsVelocityApproach[velocityIndex][approachIndex];
		float[] hitsDistanceVelocity = hitCountsDistanceVelocity[distanceIndex][velocityIndex];
		float[] hitsVelocity = hitCountsVelocity[velocityIndex];
		float[] fastHits = fastHitCounts;
		registerHit(hitsTimerWalls, index, 10, 0.7);
		registerHit(hitsTimer, index, 10, 0.7);
		registerHit(hitsDistanceVelocityWalls, index, 10, 0.7);
		registerHit(hitsWalls, index, 10, 0.7);
		registerHit(hitsDVA, index, 10, 0.7);
		registerHit(hitsVelocityAccel, index, 10, 0.7);
		registerHit(hitsVelocityApproach, index, 10, 0.7);
		registerHit(hitsDistanceVelocity, index, 10, 0.7);
		registerHit(hitsVelocity, index, 8, 0.7);
		registerHit(fastHits, index, 7, 0.7);
	}

	double danger(Point2D destination) {
		return danger(visitingIndex(destination));
	}

	double danger(int index) {
		return dangerUnWeighted(index) * dangerWeight();
	}

	public double dangerUnWeighted(int index) {
		float[] visits = visitCounts[distanceIndex][velocityIndex][accelIndex][vChangeIndex][wallIndex];
		float[] visitsFast = visitCountsFast;
		float[] visitsTimerWalls = visitCountsTimerWalls[distanceIndex][velocityIndex][vChangeIndex][wallIndex];
		float[] visitsTimer = visitCountsTimer[distanceIndex][velocityIndex][accelIndex][vChangeIndex];
		float[] visitsDistanceVelocityWalls = visitCountsDistanceVelocityWalls[distanceIndex][velocityIndex][accelIndex][wallIndex];
		float[] visitsWalls = visitCountsWalls[velocityIndex][accelIndex][wallIndex];
		float[] visitsDVA = visitCountsDVA[distanceIndex][velocityIndex][accelIndex];
		float[] visitsVelocityAccel = visitCountsVelocityAccel[velocityIndex][accelIndex];
		float[] visitsVelocityApproach = visitCountsVelocityApproach[velocityIndex][approachIndex];
		float[] visitsDistanceVelocity = visitCountsDistanceVelocity[distanceIndex][velocityIndex];
		float[] visitsVelocity = visitCountsVelocity[velocityIndex];
		float[] hitsTimerWalls = hitCountsTimerWalls[distanceIndex][velocityIndex][vChangeIndex][wallIndex];
		float[] hitsTimer = hitCountsTimer[distanceIndex][velocityIndex][accelIndex][vChangeIndex];
		float[] hitsDistanceVelocityWalls = hitCountsDistanceVelocityWalls[distanceIndex][velocityIndex][accelIndex][wallIndex];
		float[] hitsDVA = hitCountsDVA[distanceIndex][velocityIndex][accelIndex];
		float[] hitsWalls = hitCountsWalls[velocityIndex][accelIndex][wallIndex];
		float[] hitsVelocityAccel = hitCountsVelocityAccel[velocityIndex][accelIndex];
		float[] hitsVelocityApproach = hitCountsVelocityApproach[velocityIndex][approachIndex];
		float[] hitsDistanceVelocity = hitCountsDistanceVelocity[distanceIndex][velocityIndex];
		float[] hitsVelocity = hitCountsVelocity[velocityIndex];
		float[] fastHits = fastHitCounts;
		//TODO: Consider randomCounts
		double danger = 0;
		for (int i = 1; i < getFactors(); i++) {
			danger += ((isHighHitRate() ? visitsFast[i] + visits[i] + visitsTimerWalls[i] + visitsTimer[i] + visitsDistanceVelocityWalls[i] + visitsWalls[i] + visitsDistanceVelocity[i] + visitsVelocityAccel[i] + visitsVelocityApproach[i] + visitsDVA[i] + visitsVelocity[i] : 0) +
					hitsTimerWalls[i] + hitsTimer[i] + hitsDistanceVelocityWalls[i] + hitsWalls[i] + hitsDistanceVelocity[i] + hitsVelocityAccel[i] + hitsVelocityApproach[i] + hitsDVA[i] + hitsVelocity[i] + fastHits[i]) / roots[Math.abs(index - i)];
			//danger += ((isHighHitRate() ? visitsFast[i] + visits[i] + hitsTimerWalls[i] + hitsTimer[i] + hitsDistanceVelocityWalls[i] + hitsVelocityApproach[i] : 0) + hitsDVA[i] + hitsWalls[i] + hitsDistanceVelocity[i] + hitsVelocityAccel[i] + hitsVelocity[i] + fastHits[i]) / roots[Math.abs(index - i)];
		}
		return danger;
	}

	double dangerWeight() {
		double t = travelTime(Math.abs(distanceFromTarget(0)));
		return bulletPower / t;
	}

	static boolean isHighHitRate() {
		return Butterfly.roundNum > 3 && hitRate() > 2.5;
	}

	static boolean isLowHitRate() {
		return hitRate() < 1.0;
	}

	static double hitRate() {
		return rangeHits / (Butterfly.roundNum + 1);
	}

	public static int getMiddleFactor() {
		return MIDDLE_FACTOR;
	}

	public static int getFactors() {
		return FACTORS;
	}
}
