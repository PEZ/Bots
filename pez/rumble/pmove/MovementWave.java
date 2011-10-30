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
	public static final int FACTORS = 31;
	static final int ACCEL_INDEXES = 3;
	public static final int MIDDLE_FACTOR = (FACTORS - 1) / 2;
	static final double[] APPROACH_SLICES = { -3, 1, 3};
	static final double[] DISTANCE_SLICES = { 300, 450, 550, 650 };
	static final double[] VELOCITY_SLICES = { 1, 3, 5, 7 };
	static final double[] WALL_SLICES = { 0.1, 0.2, 0.35, 0.55 };
	static final double[] WALL_SLICES_REVERSE = { 0.35, 0.7 };
	static final double[] TIMER_SLICES = { 0.15, 0.3, 0.7, 1.3 };
	static final int APPROACH_INDEXES = APPROACH_SLICES.length + 1;
	static final int DISTANCE_INDEXES = DISTANCE_SLICES.length + 1;
	static final int VELOCITY_INDEXES = VELOCITY_SLICES.length + 1;
	static final int TIMER_INDEXES = TIMER_SLICES.length + 1;
	static final int WALL_INDEXES = WALL_SLICES.length + 1;
	static final int WALL_INDEXES_REVERSE = WALL_SLICES_REVERSE.length + 1;

	static float[][][][][][] visitCounts;
	static float[] visitCountsFast;
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
		visitCounts = new float[DISTANCE_INDEXES][VELOCITY_INDEXES][ACCEL_INDEXES][TIMER_INDEXES][WALL_INDEXES][FACTORS];
		visitCountsFast = new float[FACTORS];
		hitCountsTimerWalls = new float[DISTANCE_INDEXES][VELOCITY_INDEXES][TIMER_INDEXES][WALL_INDEXES][FACTORS];
		hitCountsTimer = new float[DISTANCE_INDEXES][VELOCITY_INDEXES][ACCEL_INDEXES][TIMER_INDEXES][FACTORS];
		hitCountsDistanceVelocityWalls = new float[DISTANCE_INDEXES][VELOCITY_INDEXES][ACCEL_INDEXES][WALL_INDEXES][FACTORS];
		hitCountsDVA = new float[DISTANCE_INDEXES][VELOCITY_INDEXES][ACCEL_INDEXES][FACTORS];
		hitCountsWalls = new float[VELOCITY_INDEXES][ACCEL_INDEXES][WALL_INDEXES][FACTORS];
		hitCountsVelocityAccel = new float[VELOCITY_INDEXES][ACCEL_INDEXES][FACTORS];
		hitCountsVelocityApproach = new float[VELOCITY_INDEXES][APPROACH_INDEXES][FACTORS];
		hitCountsDistanceVelocity = new float[DISTANCE_INDEXES][VELOCITY_INDEXES][FACTORS];
		hitCountsVelocity = new float[VELOCITY_INDEXES][FACTORS];
		fastHitCounts = new float[FACTORS];
		fastHitCounts[MIDDLE_FACTOR] = 50;
		randomCounts = new float[FACTORS];
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
		init(robot, FACTORS);
		this.floater = floater;
	}

	static void updateWaves() {
		List<MovementWave> reap = new ArrayList<MovementWave>();
		for (int i = 0, n = waves.size(); i < n; i++) {
			MovementWave wave = (MovementWave)waves.get(i);
			wave.setDistanceFromGun((robot.getTime() - wave.startTime) * wave.getBulletVelocity());
			if (wave.passed(10)) {
				if (!wave.visitRegistered) {
					wave.registerVisit();
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
		}
	}

	void registerVisit() {
		int index = visitingIndex();
		float[] visits = visitCounts[distanceIndex][velocityIndex][accelIndex][vChangeIndex][wallIndex];
		float[] visitsFast = visitCountsFast;
		registerHit(visits, index, PUtils.minMax(Math.pow(hitRate() * 2.1, 2), 0, 75), 500.0);
		registerHit(visitsFast, index, PUtils.minMax(Math.pow(hitRate() * 2.1, 2), 0, 75), 500.0);
		registerHit(randomCounts, (int)(Math.random() * (FACTORS - 1) + 1), PUtils.minMax(Math.pow(hitRate() * 2.1, 2), 0, 60), 10.0);
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
		for (int i = 0; i < FACTORS; i++) {
			buffer[i] =  (float)PUtils.rollingAvg(buffer[i], index == i ? weight : 0.0, depth);
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
		registerHit(hitsTimerWalls, index, 100.0, 1.0);
		registerHit(hitsTimer, index, 100.0, 1.0);
		registerHit(hitsDistanceVelocityWalls, index, 100.0, 1.0);
		registerHit(hitsWalls, index, 90.0, 1.0);
		registerHit(hitsDVA, index, 90.0, 1.0);
		registerHit(hitsVelocityAccel, index, 80.0, 1.0);
		registerHit(hitsVelocityApproach, index, 80.0, 1.0);
		registerHit(hitsDistanceVelocity, index, 80.0, 1.0);
		registerHit(hitsVelocity, index, 75.0, 1.0);
		registerHit(fastHits, index, 50.0, 1.0);
	}

	double danger(Point2D destination) {
		return danger(visitingIndex(destination));
	}

	double danger(int index) {
		return dangerUnWeighed(index) * dangerWeight();
	}

	public double dangerUnWeighed(int index) {
		float[] visits = visitCounts[distanceIndex][velocityIndex][accelIndex][vChangeIndex][wallIndex];
		float[] visitsFast = visitCountsFast;
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
		double danger = 0;
		for (int i = 1; i < FACTORS; i++) {
			danger += ((hitRate() > 2.0 ? visitsFast[i] + visits[i] + hitRate() > 3.0 ? randomCounts[i] : 0 : 0) + hitsTimerWalls[i] + hitsTimer[i] + hitsDistanceVelocityWalls[i] + hitsWalls[i] + hitsDistanceVelocity[i] + hitsVelocityAccel[i] + hitsVelocityApproach[i] + hitsDVA[i] + hitsVelocity[i] + fastHits[i]) / roots[Math.abs(index - i)];
		}
		return danger;
	}

	double dangerWeight() {
		double t = travelTime(Math.abs(distanceFromTarget(0)));
		return bulletPower / t;
	}

	static boolean isLowHitRate() {
		return hitRate() < 1.0;
	}

	static double hitRate() {
		return rangeHits / (Butterfly.roundNum + 1);
	}
}