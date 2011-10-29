package pez.rumble.pmove;
import pez.rumble.utils.*;
import robocode.*;
import robocode.util.Utils;
import java.util.*;
import java.util.zip.*;
import java.io.*;
import java.awt.geom.*;
import java.awt.Color; // GL

// Butterfly, a movement by PEZ. For CassiusClay - Float like a butterfly!
// http://robowiki.net/?CassiusClay
//
// This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
// http://robowiki.net/?RWPCL
// (Basically it means you must keep the code public if you base any bot on it.)
//
// $Id: Butterfly.java,v 1.25 2004/09/27 23:00:14 peter Exp $


public class Butterfly {
	public static boolean isMC;

	public static boolean doGL; // GL

	static final double MAX_VELOCITY = 8;
	static final double MAX_TURN_RATE = 10;

	static final double MAX_WALL_SMOOTH_TRIES = 175;
	static final double WALL_MARGIN = 20;
	static final double DEFAULT_BLIND_MANS_STICK = 120;

	static Rectangle2D fieldRectangle;
	static Point2D robotLocation = new Point2D.Double();
	static Point2D enemyLocation = new Point2D.Double();
	static double enemyAbsoluteBearing;
	static double enemyApproachVelocity;
	static double enemyDistance;
	static int distanceIndex;
	static double enemyEnergy;
	static double enemyVelocity;
	static double enemyFirePower = 2.5;
	static int lastVelocityIndex;
	static double approachVelocity;
	static double velocity;
	static int timeSinceVChange;
	static double lastForwardSmoothing;
	static double roundNum;
	static long lastScanTime;
	static long time;
	static long scans;
	static int bulletsThisRound;

	double roundsLeft;
	AdvancedRobot robot;

	public Butterfly(AdvancedRobot robot) {
		this.robot = robot;
		MovementWave.init();
		MovementWave.reset();
		enemyEnergy = 100;
		fieldRectangle = PUtils.fieldRectangle(robot, WALL_MARGIN);

		if (roundNum > 0) {
			System.out.println("range hits taken: " + (int)MovementWave.rangeHits + " (average / round: " + PUtils.formatNumber(MovementWave.rangeHits / roundNum) + ")");
		}

		roundsLeft = robot.getNumRounds() - roundNum - 1;
		roundNum++;
		bulletsThisRound = 0;
		if (doGL) { // GL
			WaveGrapher.initDangerGraph(); // GL
		} // GL
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		scans++;
		time = robot.getTime();
		MovementWave wave = new MovementWave(robot, this);
		wave.startTime = robot.getTime() - 2;

		double wallDamage = 0;
		if (Math.abs(e.getVelocity()) == 0 && Math.abs(enemyVelocity) > 2.0) {
			wallDamage = Math.max(0, Math.abs(enemyVelocity) / 2 - 1);
		}
		enemyVelocity = e.getVelocity();

		if (robot.getOthers() > 0 && scans > 0) {
			enemyApproachVelocity = PUtils.rollingAvg(enemyApproachVelocity, enemyVelocity * -Math.cos(e.getHeadingRadians() - enemyAbsoluteBearing), Math.min(scans, 5000));
		}

		wave.setGunLocation(new Point2D.Double(enemyLocation.getX(), enemyLocation.getY()));
		wave.setStartBearing(wave.gunBearing(robotLocation));

		double enemyDeltaEnergy = enemyEnergy - e.getEnergy() - wallDamage;
		if (enemyDeltaEnergy >= 0.1 && enemyDeltaEnergy <= 3.0) {
			enemyFirePower = enemyDeltaEnergy;
			MovementWave.bullets.add(wave);
			MovementWave.surfables.add(wave);
			bulletsThisRound++;
			if (doGL) { // GL
				wave.grapher = new WaveGrapher(wave); // GL
			} // GL
		}
		enemyEnergy = e.getEnergy();
		double bulletVelocity = PUtils.bulletVelocity(enemyFirePower);
		wave.setBulletVelocity(bulletVelocity);

		double orbitDirection = robotOrbitDirection(wave.gunBearing(robotLocation));
		wave.setOrbitDirection(wave.maxEscapeAngle() * orbitDirection / (double)MovementWave.MIDDLE_FACTOR);

		approachVelocity = velocity * -Math.cos(robot.getHeadingRadians() - (enemyAbsoluteBearing + 180));
		wave.approachIndex = PUtils.index(MovementWave.APPROACH_SLICES, approachVelocity);

		distanceIndex = PUtils.index(MovementWave.DISTANCE_SLICES, enemyDistance);
		wave.bulletPower = enemyFirePower;
		wave.distanceIndex = distanceIndex;
		int velocityIndex = PUtils.index(MovementWave.VELOCITY_SLICES, Math.abs(velocity));
		velocity = robot.getVelocity();
		wave.accelIndex = 0;
		if (velocityIndex != lastVelocityIndex) {
			timeSinceVChange = 0;
			wave.accelIndex = velocityIndex < lastVelocityIndex ? 1 : 2;
		}
		wave.velocityIndex = velocityIndex;
		wave.lastVelocityIndex = lastVelocityIndex;
		lastVelocityIndex = velocityIndex;

		wave.setTargetLocation(robotLocation);

		wave.vChangeIndex = PUtils.index(MovementWave.TIMER_SLICES, timeSinceVChange++ / wave.travelTime());
		double wallDistance = wave.wallDistance(1, fieldRectangle);
		//wave.wallIndex = PUtils.index(MovementWave.WALL_SLICES, lastForwardSmoothing);
		wave.wallIndex = PUtils.index(MovementWave.WALL_SLICES, wallDistance);
		double wallDistanceReverse = wave.wallDistance(-1, fieldRectangle);
		wave.wallIndexReverse = PUtils.index(MovementWave.WALL_SLICES_REVERSE, wallDistanceReverse);


		robotLocation.setLocation(new Point2D.Double(robot.getX(), robot.getY()));
		enemyAbsoluteBearing = robot.getHeadingRadians() + e.getBearingRadians();
		enemyLocation.setLocation(PUtils.project(robotLocation, enemyAbsoluteBearing, enemyDistance));
		enemyDistance = e.getDistance();

		MovementWave.waves.add(wave);

		move(wave, orbitDirection);

		MovementWave.reset();
		lastScanTime = robot.getTime();
		//System.out.print(wave.distanceIndex);
		//System.out.print(", " + wave.velocityIndex);
		//System.out.print(", " + wave.accelIndex);
		//System.out.print(", " + wave.lastVelocityIndex);
		//System.out.print(", " + wave.vChangeIndex);
		//System.out.print(", " + wave.wallIndex);
		//System.out.println(", " + wave.approachIndex);
	}

	public void onHitByBullet(HitByBulletEvent e) {
		Bullet b = e.getBullet();
		if (false) {
			Hit hit = new Hit(b.getPower(), enemyDistance, robotLocation, enemyLocation);
			MovementWave wave = (MovementWave)Wave.findClosest(MovementWave.bullets, new Point2D.Double(b.getX(), b.getY()), b.getVelocity());
			if (wave != null) {
				hit.gf = wave.getGF(new Point2D.Double(b.getX(), b.getY()));
			}
			Hit.hits.add(hit);
			hit.print();
		}
		MovementWave.hitsTaken++;
		if (b.getPower() > 1.2 && enemyDistance > 150) {
			MovementWave.rangeHits++;
		}
		MovementWave.registerHit(e.getBullet());
		enemyEnergy += 3 * e.getBullet().getPower();
	}

	public void onBulletHit(BulletHitEvent e){
		double power = e.getBullet().getPower();
		enemyEnergy -= 4 * power + Math.max(2 * power - 1, 0);
	}

	public void onBulletHitBullet(BulletHitBulletEvent e) {
		MovementWave.registerHit(e.getHitBullet());
	}

	void move(MovementWave wave, double direction) {
		MovementWave.updateWaves();
		MovementWave closest = (MovementWave)Wave.findClosest(MovementWave.surfables, robotLocation);
		Point2D orbitCenter = orbitCenter(closest);
		if (closest != null) {
			updateDirectionStats(MovementWave.surfables, closest);
		}
		Move forward = wallSmoothedDestination(robotLocation, orbitCenter, direction);
		double forwardSmoothingDanger = forward.smoothingDanger();
		lastForwardSmoothing = forward.normalizedSmoothing();
		Move reverse = wallSmoothedDestination(robotLocation, orbitCenter, -direction);
		double reverseSmoothingDanger = reverse.smoothingDanger();
		if (!(forward.normalizedSmoothing() > 20 && reverse.normalizedSmoothing() > 20)) {
			MovementWave.dangerForward += forwardSmoothingDanger;
			MovementWave.dangerReverse += reverseSmoothingDanger;
		}
		if (forwardSmoothingDanger > 0 && reverseSmoothingDanger > 0) {
			MovementWave.dangerStop = MovementWave.dangerForward + MovementWave.dangerReverse;
		}
		Point2D destination = forward.location;
		double wantedVelocity = MAX_VELOCITY;
		if (MovementWave.hitsTaken == 0 && robot.getEnergy() > 25 && ((roundsLeft < 6 && enemyFirePower < 0.3) || (roundsLeft < 3 && enemyFirePower < (3.01 - roundsLeft)))) {
			if (!isMC) {
				wantedVelocity = 0;
			}
		}
		else if (enemyEnergy > 0 && !enemyIsRammer() && MovementWave.bullets.size() == 0) {
			if (enemyLocation.distance(reverse.location) / enemyLocation.distance(forward.location) > 1.03) {
				destination = reverse.location;
			}
		}
		else if (!enemyIsRammer() && MovementWave.dangerStop < MovementWave.dangerReverse && MovementWave.dangerStop < MovementWave.dangerForward) {
			wantedVelocity = 0;
		}
		else if (MovementWave.dangerReverse < MovementWave.dangerForward) {
			destination = reverse.location;
		}
		double newHeading = PUtils.absoluteBearing(robotLocation, destination);
		double oldHeading = robot.getHeadingRadians();
		robot.setAhead(PUtils.backAsFrontDirection(newHeading, oldHeading) * 50);
		robot.setTurnRightRadians(PUtils.backAsFrontTurn(newHeading, oldHeading));
		robot.setMaxVelocity(wantedVelocity);
		if (doGL) { // GL
			WaveGrapher.drawDangerGraph(MovementWave.dangerForward, MovementWave.dangerStop, MovementWave.dangerReverse); // GL
		} // GL
	}

	static Move wallSmoothedDestination(Point2D location, Point2D orbitCenter, double direction) {
		Point2D destination = new Point2D.Double();
		destination.setLocation(location);
		double distance = enemyLocation.distance(location);
		double evasion = evasion(distance);
		double blindStick = enemyIsRammer() ? PUtils.minMax(enemyDistance / 2, 60, DEFAULT_BLIND_MANS_STICK) : DEFAULT_BLIND_MANS_STICK;
		double smoothing = 0;
		while (!fieldRectangle.contains(destination = PUtils.project(location,
				PUtils.absoluteBearing(location, orbitCenter) - direction * ((evasion - smoothing / 100) * Math.PI / 2), blindStick)) && smoothing < MAX_WALL_SMOOTH_TRIES) {
			smoothing += 5;
		}
		return new Move(destination, smoothing, evasion, distance, destination.distance(enemyLocation));
	}

	static boolean enemyIsRammer() {
		return enemyApproachVelocity > 4.5;
	}

	static double evasion(double distance) {
		double evasion;
		if (time < 16) {
			evasion = PUtils.minMax(distance / 700, 1.3, 5.0);
		}
		else {
			if (enemyIsRammer()) {
				evasion = 1.6;
			}
			else if (time > 30 && bulletsThisRound == 0) {
				evasion = PUtils.minMax(300.0 / distance, 0.75, 1.5);
			}
			else if (MovementWave.isLowHitRate()) {
				evasion = PUtils.minMax(410.0 / distance, 0.95, 1.25);
			}
			else {
				evasion = PUtils.minMax((300 * Math.pow(MovementWave.hitRate(), 1.2)) / distance, 1.03, 1.3);
			}
		}
		return evasion;
	}

	void updateDirectionStats(List _waves, MovementWave closest) {
		Move move = waveImpactLocation(closest, 1.0, MAX_VELOCITY);
		MovementWave.dangerForward += impactDanger(_waves, move.location);
		if (closest.grapher != null) { // GL
			closest.grapher.drawForwardDestination(move.location, closest.danger(move.location)); // GL
		} // GL
		move = waveImpactLocation(closest, -1.0, MAX_VELOCITY);
		MovementWave.dangerReverse += impactDanger(_waves, move.location);
		if (closest.grapher != null) { // GL
			closest.grapher.drawReverseDestination(move.location, closest.danger(move.location)); // GL
		} // GL
		move = waveImpactLocation(closest, 1.0, 0);
		MovementWave.dangerStop += impactDanger(_waves, move.location);
		if (closest.grapher != null) { // GL
			closest.grapher.drawStopDestination(move.location, closest.danger(move.location)); // GL
		} // GL
	}

	double impactDanger(List _waves, Point2D impact) {
		double danger = 0;
		for (int i = 0, n = _waves.size(); i < n; i++) {
			danger += ((MovementWave)_waves.get(i)).danger(impact);
		}
		return danger;
	}

	Move waveImpactLocation(MovementWave closest, double direction, double maxVelocity) {
		double currentDirection = robotOrbitDirection(closest.gunBearing(robotLocation));
		double v = Math.abs(robot.getVelocity()) * PUtils.sign(direction);
		double h = robot.getHeadingRadians();
		Point2D orbitCenter = orbitCenter(closest);
		Point2D impactLocation = new Point2D.Double(robot.getX(), robot.getY());
		Move smoothed = wallSmoothedDestination(impactLocation, orbitCenter, currentDirection * direction);
		double wantedHeading = PUtils.absoluteBearing(impactLocation, smoothed.location);
		h += PUtils.backAsFrontDirection(wantedHeading, h) < 0 ? Math.PI : 0.0;
		int time = 0;
		do {
			double maxTurn = Math.toRadians(MAX_TURN_RATE - 0.75 * Math.abs(v));
			h += PUtils.minMax(PUtils.backAsFrontTurn(wantedHeading, h), -maxTurn, maxTurn);
			if (v < maxVelocity) {
				v = Math.min(maxVelocity, v + (v < 0 ? 2 : 1));
			}
			else {
				v = Math.max(maxVelocity, v - 2);
			}
			impactLocation = PUtils.project(impactLocation, h, v);
			smoothed = wallSmoothedDestination(impactLocation, orbitCenter, currentDirection * direction);
			wantedHeading = PUtils.absoluteBearing(impactLocation, smoothed.location);
		} while (closest.distanceFromTarget(impactLocation, time++) > 18);
		return new Move(impactLocation, smoothed.smoothing, smoothed.wantedEvasion, smoothed.oldDistance, impactLocation.distance(enemyLocation));
	}

	Point2D orbitCenter(MovementWave wave) {
		return wave != null ? wave.getGunLocation() : enemyLocation;
	}

	double robotOrbitDirection(double bearing) {
		return PUtils.sign(robot.getVelocity() * Math.sin(robot.getHeadingRadians() - bearing));
	}
}

class MovementWave extends Wave {
	static final int FACTORS = 31;
	static final int ACCEL_INDEXES = 3;
	static final int MIDDLE_FACTOR = (FACTORS - 1) / 2;
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
	static List waves;
	static List bullets;
	static List surfables;
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
		waves = new ArrayList();
		bullets = new ArrayList();
		surfables = new ArrayList();
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
		List reap = new ArrayList();
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

	double dangerUnWeighed(int index) {
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

class Move {
	Point2D location;
	double smoothing;
	double wantedEvasion;
	double oldDistance;
	double newDistance;

	Move(Point2D location, double smoothing, double wantedEvasion, double oldDistance, double newDistance) {
		this.location = location;
		this.smoothing = smoothing;
		this.wantedEvasion = wantedEvasion;
		this.oldDistance = oldDistance;
		this.newDistance = newDistance;
	}

	double smoothingDanger() {
		if (normalizedSmoothing() > 80 || (oldDistance > 220 && newDistance < 250) && normalizedSmoothing() > 20) {
			return (1 + smoothing) * 50;
		}
		return 0;
	}

	double normalizedSmoothing() {
		return smoothing / wantedEvasion;
	}
}

class Hit {
	static List hits = new ArrayList();
	double bulletPower;
	int distance;
	int robotX;
	int robotY;
	int enemyX;
	int enemyY;
	double gf = -100;;

	Hit(double bulletPower, double distance, Point2D robotLocation, Point2D enemyLocation) {
		this.bulletPower = (double)((int)(bulletPower * 100) / 100.0);
		this.distance = (int)distance;
		this.robotX = (int)robotLocation.getX();
		this.robotY = (int)robotLocation.getY();
		this.enemyX = (int)enemyLocation.getX();
		this.enemyY = (int)enemyLocation.getY();
	}

	void print() {
		System.out.println("GF: " + PUtils.formatNumber(gf) + " - bp: " + bulletPower + " - distance: " + distance + " - robotLocation: " + robotX + ":" + robotY + " - enemyLocation: " + enemyX + ":" + enemyY);
	}

	static void printAll() {
		for (int i = 0, n = Hit.hits.size(); i < n; i++) {
			Hit hit = (Hit)Hit.hits.get(i);
			hit.print();
		}
	}
}

// GL
class WaveGrapher{
	static GLRenderer renderer = GLRenderer.getInstance();
	static int counter = 0;

	String id;
	MovementWave wave;
	PointGL[] dots;
	PointGL forwardDestination = new PointGL();
	PointGL reverseDestination = new PointGL();
	PointGL stopDestination = new PointGL();
	LabelGL forwardLabel = new LabelGL("");
	LabelGL reverseLabel = new LabelGL("");
	LabelGL stopLabel = new LabelGL("");

	WaveGrapher(MovementWave wave) {
		this.id = "" + counter++;
		this.wave = wave;
		this.dots = new PointGL[MovementWave.FACTORS];
		for (int i = 0; i < dots.length; i++) {
			dots[i] = new PointGL();
			if (i == MovementWave.MIDDLE_FACTOR) {
				dots[i].addLabel(new LabelGL(id));
			}
			renderer.addRenderElement(dots[i]);
		}
		forwardDestination.addLabel(forwardLabel);
		forwardDestination.setColor(Color.GREEN);
		forwardDestination.setSize(15);
		forwardDestination.setPosition(-100, -100);
		reverseDestination.addLabel(reverseLabel);
		reverseDestination.setColor(Color.RED);
		reverseDestination.setSize(15);
		reverseDestination.setPosition(-100, -100);
		stopDestination.addLabel(stopLabel);
		stopDestination.setColor(Color.YELLOW);
		stopDestination.setSize(15);
		stopDestination.setPosition(-100, -100);
		renderer.addRenderElement(forwardDestination);
		renderer.addRenderElement(reverseDestination);
		renderer.addRenderElement(stopDestination);
	}

	void drawWave() {
		float totalVisits = 0;
		for (int i = 0; i < dots.length; i++) {
			Point2D dot = PUtils.project(wave.getGunLocation(),
					wave.getStartBearing() + wave.getOrbitDirection() * (i - MovementWave.MIDDLE_FACTOR),
					wave.distanceFromGun());
			dots[i].setPosition(dot.getX(), dot.getY());
			dots[i].setColor(Color.BLUE);
			dots[i].setSize((float)wave.dangerUnWeighed(i) / 7.0f);
		}
	}

	void drawDestination(PointGL destination, LabelGL label, Point2D coords, double value) {
		destination.setPosition(coords.getX(), coords.getY());
		label.setString(id + " : " + (int)value);
	}

	void drawForwardDestination(Point2D coords, double value) {
		drawDestination(forwardDestination, forwardLabel, coords, value);
	}

	void drawReverseDestination(Point2D coords, double value) {
		drawDestination(reverseDestination, reverseLabel, coords, value);
	}

	void drawStopDestination(Point2D coords, double value) {
		drawDestination(stopDestination, stopLabel, coords, value);
	}

	void remove() {
		for (int i = 0; i < dots.length; i++) {
			dots[i].remove();
		}
		forwardDestination.remove();
		reverseDestination.remove();
		stopDestination.remove();
	}

	static RectangleGL forwardRect;
	static RectangleGL stopRect;
	static RectangleGL reverseRect;

	static void drawDangerGraph(double dangerForward, double dangerStop, double dangerReverse) {
		forwardRect.setSize(15, dangerForward);
		stopRect.setSize(15, dangerStop);
		reverseRect.setSize(15, dangerReverse);
	}

	static boolean initDangerGraph() {
		forwardRect = new RectangleGL(10, 0, 15, 0, Color.GREEN, 1);
		stopRect    = new RectangleGL(25, 0, 15, 0, Color.YELLOW, 1);
		reverseRect = new RectangleGL(40, 0, 15, 0, Color.RED, 1);
		forwardRect.setFilled(true);
		stopRect.setFilled(true);
		reverseRect.setFilled(true);
		renderer.addRenderElement(forwardRect);
		renderer.addRenderElement(stopRect);
		renderer.addRenderElement(reverseRect);
		return true;
	}
}
// GL
