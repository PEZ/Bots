package pez.rumble.pgun;
import pez.rumble.RumbleBot;
import pez.rumble.utils.*;
import robocode.*;
import robocode.util.Utils;

import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.awt.geom.*;

//Bee, a gun by PEZ. For CassiusClay - Sting like a bee!
//http://robowiki.net/?CassiusClay

//This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
//http://robowiki.net/?RWPCL
//(Basically it means you must keep the code public.)

//$Id: Bee.java,v 1.27 2007-02-27 05:49:05 peters Exp $

public class Bee extends Stinger {
	static final double WALL_MARGIN = 18;
	static final double MAX_BULLET_POWER = 3.0;
	static final double BULLET_POWER = 1.9;

	Point2D enemyLocation = new Point2D.Double();
	double lastVelocity;
	double timeSinceAccel;
	double timeSinceDeccel;
	double timeSinceStationary;
	double timeSinceMaxSpeed;
	double lastBearingDirection;
	double distance;
	long lastScanTime;
	BeeWave lastWave;

	public Bee(RumbleBot robot, RobotPredictor robotPredictor) {
		super(robot, robotPredictor);
	}

	void scannedRobot(ScannedRobotEvent e) {
		BeeWave wave = new BeeWave(robot);
		Point2D robotLocation = new Point2D.Double(robot.getX(), robot.getY());
		double bearing = robot.getHeadingRadians() + e.getBearingRadians();
		distance = e.getDistance();
		wave.distance = distance;
		enemyLocation.setLocation(PUtils.project(robotLocation, bearing, distance));
		double bulletPower = bulletPower(distance, e.getEnergy(), robot.getEnergy());
		wave.bulletPower = bulletPower;
		double lateralVelocity = e.getVelocity() * Math.sin(e.getHeadingRadians() - bearing);
		wave.absV = Math.abs(e.getVelocity());
		wave.absLatV = Math.abs(lateralVelocity);
		double acceleration = wave.absV - Math.abs(lastVelocity);

		timeSinceAccel++;
		timeSinceDeccel++;
		timeSinceStationary++;
		timeSinceMaxSpeed++;
		if (acceleration > 0.5) {
			timeSinceAccel = 0;
		}
		if (acceleration < -0.5) {
			timeSinceDeccel = 0;
		}
		if (wave.absV < 0.5) {
			timeSinceStationary = 0;
		}
		if (wave.absV > 7.5) {
			timeSinceMaxSpeed = 0;
		}
		wave.timeSinceVChange = Math.min(timeSinceAccel, timeSinceDeccel);
		wave.timeSinceAccel = timeSinceAccel;
		wave.timeSinceDeccel = timeSinceDeccel;
		wave.timeSinceStationary = timeSinceStationary;
		wave.timeSinceMaxSpeed = timeSinceMaxSpeed;

		wave.setStartTime(robot.getTime() + 1);
		wave.setBulletVelocity(PUtils.bulletVelocity(bulletPower));
		Point2D nextRobotLocation = robotPredictor.getNextLocation(robot);
		Point2D nextEnemyLocation = PUtils.project(enemyLocation, e.getHeadingRadians(), e.getVelocity());
		double nextBearing = PUtils.absoluteBearing(nextRobotLocation, nextEnemyLocation);
		wave.setGunLocation(nextRobotLocation);
		wave.setStartBearing(nextBearing);
		if (lastWave != null) {
			lastWave.setStartBearing(PUtils.absoluteBearing(robotLocation, enemyLocation));
			lastWave.setGunLocation(robotLocation);
		}
		wave.setTargetLocation(enemyLocation);
		if (wave.absV > 0) {
			lastBearingDirection = wave.maxEscapeAngle() * PUtils.sign(lateralVelocity);
		}
		double orbitDirection = lastBearingDirection / (double)BeeWave.MIDDLE_BIN;
		wave.setOrbitDirection(orbitDirection);

		wave.wallDistance = wave.wallDistance(1, fieldRectangle);
		wave.reverseWallDistance = wave.wallDistance(-1, fieldRectangle);

		wave.setSegmentation();

		if (robot.getOthers() > 0 && lastBearingDirection != 0) {
			BeeWave.waves.add(wave);
		}
		BeeWave.updateWaves();

		double guessedBearing = nextBearing + orbitDirection * (wave.mostVisited() - BeeWave.MIDDLE_BIN);
		robot.setTurnGunRightRadians(Utils.normalRelativeAngle(guessedBearing - robot.getGunHeadingRadians()));
		if (isTC || (robot.getEnergy() >= 0.3 || e.getEnergy() < robot.getEnergy() / 5 || distance < 120)) {
			if ((robot.getTime() > 50 || robot.enemyHasFired) &&
				Math.abs(robot.getGunTurnRemainingRadians()) < PUtils.botWidthAngle(distance) / 2 && !isMC &&
				robot.setFireBullet(bulletPower) != null &&
				lastWave != null) {
					lastWave.weight = 5;
					BeeWave.bullets.add(lastWave);
					lastWave.currentGuessor().registerFire();
			}
		}
		lastVelocity = e.getVelocity();
		lastWave = wave;
	}
	
	void initRound() {
		BeeWave.initRound(robot);
		System.out.println(BeeWave.guessors.toString());
		System.out.println("Using: " + BeeWave.currentGuessor.echoStats());
	}
	
	public void roundOver() {
		if (PUtils.isLastRound(robot)) {
			if (false && !isTC) {
				BeeWave.saveStats(robot);
			}
			System.out.println("Bzzz bzzz. Over and out!");
		}
	}

	void bulletHit(BulletHitEvent e) {
		Bullet b = e.getBullet();
		BeeWave wave = (BeeWave)Wave.findClosest(BeeWave.bullets, new Point2D.Double(b.getX(), b.getY()), b.getVelocity());
		if (wave != null) {
			BeeWave.replacor.registerHit(wave.visitingIndex(), wave);
		}
	}
}

class BeeWave extends GunWave {
	static final int BINS = 75;
	static final int MIDDLE_BIN = (BINS - 1) / 2;

	static List<BeeWave> waves;
	static List<BeeWave> bullets;

	static BeeRealisor accumulator;
	static BeeVirtualisor virtualisor;
	static BeeForgettor replacor;
	static List<Guessor> guessors;
	static Guessor currentGuessor;
	Map<Guessor, Integer> guesses = new HashMap<Guessor, Integer>();

	double bulletPower;
	double timeSinceAccel;
	double timeSinceDeccel;
	double distance;
	double absLatV;
	double absV;
	double timeSinceVChange;
	double timeSinceMaxSpeed;
	double timeSinceStationary;
	double wallDistance;
	double reverseWallDistance;
	int accelSegment;
	int distanceSegment;
	int distanceSegmentFaster;
	int velocitySegment;
	int velocitySegmentFaster;
	int vChangeSegment;
	int vChangeSegmentFaster;
	int sinceStationarySegment;
	int sinceMaxSpeedSegment;
	int reverseWallSegment;
	int wallSegment;
	int wallSegmentFaster;
	double weight = 1;

	static void initRound(AdvancedRobot robot) {
		waves = new ArrayList<BeeWave>();
		bullets = new ArrayList<BeeWave>();
		if (guessors == null) {
			readStats(robot);
		}
		for (int i = 0, n = guessors.size(); i < n; i++) {
			((Guessor)guessors.get(i)).rounds++;
		}
		Collections.sort(guessors);
		currentGuessor = (Guessor)guessors.get(0);
	}

	public BeeWave(AdvancedRobot robot) {
		init(robot, BINS);
	}

	void setSegmentation() {
		accelSegment = timeSinceAccel == 0 ? 0 : timeSinceDeccel == 0 ? 1 : 2;
		distanceSegment = PUtils.index(Guessor.DISTANCE_SLICES, distance);
		distanceSegmentFaster = PUtils.index(Guessor.DISTANCE_SLICES_FASTER, distance);
		velocitySegment =  PUtils.index(Guessor.VELOCITY_SLICES, absLatV);
		velocitySegmentFaster =  PUtils.index(Guessor.VELOCITY_SLICES_FASTER, absV);
		vChangeSegment = PUtils.index(Guessor.TIMER_SLICES, timeSinceVChange / travelTime());
		vChangeSegmentFaster = PUtils.index(Guessor.TIMER_SLICES_FASTER, timeSinceVChange / travelTime());
		sinceStationarySegment = PUtils.index(Guessor.TIMER_SLICES, timeSinceStationary / travelTime());
		sinceMaxSpeedSegment = PUtils.index(Guessor.TIMER_SLICES, timeSinceMaxSpeed / travelTime());
		wallSegment = PUtils.index(Guessor.WALL_SLICES, wallDistance);
		wallSegmentFaster = PUtils.index(Guessor.WALL_SLICES_FASTER, wallDistance);
		reverseWallSegment = PUtils.index(Guessor.WALL_SLICES_REVERSE, reverseWallDistance);
	}

	static void updateWaves() {
		List<BeeWave> reap = new ArrayList<BeeWave>();
		for (int i = 0, n = waves.size(); i < n; i++) {
			BeeWave wave = (BeeWave)waves.get(i);
			wave.setDistanceFromGun((wave.robot.getTime() - wave.getStartTime()) * wave.getBulletVelocity());
			if (wave.passed(1.5 * wave.getBulletVelocity())) {
				if (wave.getRobot().getOthers() > 0) {
					wave.registerVisit();
				}
				reap.add(wave);
				bullets.remove(wave);
			}
		}
		for (int i = 0, n = reap.size(); i < n; i++) {
			waves.remove(reap.get(i));
		}
	}

	void registerVisit() {
		for (int i = 0, n = guessors.size(); i < n; i++) {
			Guessor gun = (Guessor)guessors.get(i);
			gun.registerVisit(this, guesses);
		}
	}

	int mostVisited() {
		for (int i = 0, n = guessors.size(); i < n; i++) {
			Guessor gun = (Guessor)guessors.get(i);
			gun.guess(this);
			guesses.put(gun, new Integer(gun.guessed()));
		}
		return currentGuessor.guessed();
	}

	Guessor currentGuessor() {
		return currentGuessor;
	}

	public double maxEscapeAngle() {
		return PUtils.maxEscapeAngle(bulletVelocity) * 1.4;
	}

	static void initGuessors() {
		guessors = new ArrayList<Guessor>();
		guessors.add(accumulator = new BeeRealisor());
		guessors.add(replacor = new BeeForgettor());
		guessors.add(virtualisor = new BeeVirtualisor());
	}
	
	static Map<String, List<Guessor>> readEnemies(AdvancedRobot robot) {
		Map<String, List<Guessor>> enemies;
		try {
			enemies = (HashMap<String, List<Guessor>>)(new ObjectInputStream(new GZIPInputStream(new FileInputStream(robot.getDataFile("vgstats.gz"))))).readObject();
			System.out.println("Read vgstats for " + enemies.size() + " enemies");
		} catch (Exception e) {
			enemies = new HashMap<String, List<Guessor>>();
			System.out.println("Couldn't read vgstats: " + e.getMessage());
		}
		return enemies;
	}

	static void writeEnemies(AdvancedRobot robot, Map<String, List<Guessor>> enemies, String enemyName) {
		try {
			ObjectOutputStream oos = new ObjectOutputStream(new GZIPOutputStream(new RobocodeFileOutputStream(robot.getDataFile("vgstats.gz"))));
			oos.writeObject(enemies);
			oos.close();
			System.out.println("Wrote vgstats for " + enemyName + " (" + enemies.size() + " enemies on file)");
		} catch (IOException e) {
			System.out.println("Couldn't write vgstats: " + e.getMessage());
		}
	}

	static void readStats(AdvancedRobot robot) {
		if (false && !Bee.isTC) { //TODO: Enable data restore
			Map<String, List<Guessor>> enemies = readEnemies(robot);
			guessors = (ArrayList<Guessor>)enemies.get(Bee.enemyName);
		}
		if (guessors != null) {
			accumulator = (BeeRealisor)guessors.get(0);
			replacor = (BeeForgettor)guessors.get(1);
			virtualisor = (BeeVirtualisor)guessors.get(2);
			System.out.println("Fetched Guessor data for enemy: " + Bee.enemyName + "\n\t" + BeeWave.guessors.toString());
		}
		else {
			System.out.println("No vgstats for " + Bee.enemyName + " on file yet.");
			initGuessors();
		}
	}

	static void saveStats(AdvancedRobot robot) {
		Map<String, List<Guessor>> enemies = readEnemies(robot);
		List<Guessor> orderedGuessors = new ArrayList<Guessor>();
		orderedGuessors.add(accumulator);
		orderedGuessors.add(replacor);
		orderedGuessors.add(virtualisor);
		enemies.put(Bee.enemyName, orderedGuessors);
		writeEnemies(robot, enemies, Bee.enemyName);
		logVGStats(enemies.keySet().toArray(), enemies.values().toArray());
	}

	static void logVGStats(Object[] names, Object[] data) {
		System.out.println("Name" + "\t" + Guessor.logHeader("A") + "\t" + Guessor.logHeader("B") + "\t" + "Selected");
		for (int i = 0, il = data.length; i < il; i++) {
			System.out.print(names[i] + "\t");
			List<Guessor> guessors = (ArrayList<Guessor>)data[i];
			for (int j = 0, jl = guessors.size(); j < jl; j++) {
				Guessor g = (Guessor)guessors.get(j);
				System.out.print(g.logRow() + "\t");
			}
			Collections.sort(guessors);
			System.out.println(((Guessor)guessors.get(0)).name());
		}
	}
}

abstract class Guessor implements Comparable<Object>, Serializable {
	static final long serialVersionUID = 7;
	transient static final int ACCEL_INDEXES = 3;
	transient static final double[] DISTANCE_SLICES = { 125, 300, 450, 600 };
	transient static final double[] DISTANCE_SLICES_FASTER = { 125, 300, 500 };
	transient static final double[] VELOCITY_SLICES = { 1, 3, 5, 7 };
	transient static final double[] VELOCITY_SLICES_FASTER = { 2, 4, 6 };
	transient static final double[] WALL_SLICES = { 0.15, 0.35, 0.65, 0.99 };
	transient static final double[] WALL_SLICES_FASTER = { 0.35, 0.99 };
	transient static final double[] WALL_SLICES_REVERSE = { 0.15, 0.35, 0.99 };
	transient static final double[] TIMER_SLICES = {.05, .15, .35, .45}; //{ 0.1, 0.3, 0.7, 1.2 };
	transient static final double[] TIMER_SLICES_FASTER = {.05, .15, .45}; //{ 0.1, 0.3, 0.7 };

	private long rBulletsFired;
	private double rRating;
	int rounds;
	transient private int guess;

	abstract double[][] buffers(BeeWave w);
	abstract double getRollingDepth();
	abstract double getWaveWeight(BeeWave wave);

	void registerVisit(BeeWave w, Map<Guessor, Integer> guesses) {
		int index = Math.max(1, w.visitingIndex());
		if (w.weight > 2.0) {
			updateRating(index, ((Integer)guesses.get(this)).intValue(), w);
		}
		registerVisit(index, w);
	}

	void updateRating(int index, int guess, BeeWave w) {
		if (w.hit(guess - index)) {
			rRating++;
		}
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

	void registerVisit(int index, BeeWave w) {
		double[][] buffers = buffers(w);
		for (int b = 0; b < buffers.length; b++) {
			buffers[b][0]++;
			for (int i = 1; i < BeeWave.BINS; i++) {
				buffers[b][i] =  (float)PUtils.rollingAvg(buffers[b][i], getWaveWeight(w) / Math.pow(Math.abs(i - index) + 1, 2), getRollingDepth());
			}
		}
	}
	
	void guess(BeeWave w) {
		guess = mostVisited(w);
	}

	int guessed() {
		return guess;
	}

	void registerFire() {
		rBulletsFired++;
	}

	void registerHit(double power, double distance) {
		rRating++;
	}

	double rRating() {
		return rRating / rBulletsFired;
	}

	public int compareTo(Object o) {
		double ratingA = this.rRating;
		double ratingB = ((Guessor)o).rRating;
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
		return name() + " rr" + logNum(rRating);
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
		logNum(rRating);
	}

	public String toString() {
		return echoStats();
	}
}

class BeeRealisor extends Guessor {
	static final long serialVersionUID = 7;
	static double[][][][][][] faster = new double[DISTANCE_SLICES_FASTER.length + 1][VELOCITY_SLICES_FASTER.length + 1]
			[ACCEL_INDEXES][TIMER_SLICES_FASTER.length + 1][WALL_SLICES_FASTER.length + 1][BeeWave.BINS];
	static double[][][] distVel = new double[DISTANCE_SLICES.length + 1][VELOCITY_SLICES.length + 1][BeeWave.BINS];
	static double[][][][] distWall = new double[DISTANCE_SLICES.length + 1][WALL_SLICES.length + 1]
			[WALL_SLICES_REVERSE.length + 1][BeeWave.BINS];
	static double[][][][] accelWall = new double[ACCEL_INDEXES][WALL_SLICES.length + 1]
			[WALL_SLICES_REVERSE.length + 1][BeeWave.BINS];
	transient static double[][][][][][][] slower = new double[DISTANCE_SLICES.length + 1][VELOCITY_SLICES.length + 1]
			[ACCEL_INDEXES][TIMER_SLICES.length + 1][WALL_SLICES.length + 1][WALL_SLICES_REVERSE.length + 1][BeeWave.BINS];
	transient static double[][][][][][][] distVelWallTimers = new double[DISTANCE_SLICES.length + 1][VELOCITY_SLICES.length + 1]
			[TIMER_SLICES.length + 1][WALL_SLICES.length + 1][TIMER_SLICES.length + 1][TIMER_SLICES.length + 1][BeeWave.BINS];
	transient static double[][][][] velTimers = new double[VELOCITY_SLICES_FASTER.length + 1][TIMER_SLICES.length + 1]
			[TIMER_SLICES.length + 1][BeeWave.BINS];
	transient static double[][][][] accelTimers = new double[ACCEL_INDEXES][TIMER_SLICES.length + 1]
			[TIMER_SLICES.length + 1][BeeWave.BINS];
//	transient static double[][][][][][][][][] all = new double[DISTANCE_SLICES.length + 1][VELOCITY_SLICES.length + 1]
//			[ACCEL_INDEXES][TIMER_SLICES.length + 1][WALL_SLICES.length + 1][WALL_SLICES_REVERSE.length + 1]
//			[TIMER_SLICES.length + 1][TIMER_SLICES.length + 1][BeeWave.BINS];

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
}

class BeeVirtualisor extends Guessor {
	static final long serialVersionUID = 7;
	static double[][][][][][] faster = new double[DISTANCE_SLICES_FASTER.length + 1][VELOCITY_SLICES_FASTER.length + 1]
			[ACCEL_INDEXES][TIMER_SLICES_FASTER.length + 1][WALL_SLICES_FASTER.length + 1][BeeWave.BINS];
	static double[][][] distVel = new double[DISTANCE_SLICES.length + 1][VELOCITY_SLICES.length + 1][BeeWave.BINS];
	transient static double[][][][] distWall = new double[DISTANCE_SLICES.length + 1]
			[WALL_SLICES.length + 1][WALL_SLICES_REVERSE.length + 1][BeeWave.BINS];
	transient static double[][][][] accelWall = new double[ACCEL_INDEXES][WALL_SLICES.length + 1]
			[WALL_SLICES_REVERSE.length + 1][BeeWave.BINS];
	transient static double[][][][][][][] slower = new double[DISTANCE_SLICES.length + 1][VELOCITY_SLICES.length + 1]
			[ACCEL_INDEXES][TIMER_SLICES.length + 1][WALL_SLICES.length + 1][WALL_SLICES_REVERSE.length + 1][BeeWave.BINS];
	transient static double[][][][][][][] distVelWallTimers = new double[DISTANCE_SLICES.length + 1][VELOCITY_SLICES.length + 1]
			[TIMER_SLICES.length + 1][WALL_SLICES.length + 1][TIMER_SLICES.length + 1][TIMER_SLICES.length + 1][BeeWave.BINS];
	transient static double[][][][] velTimers = new double[VELOCITY_SLICES_FASTER.length + 1][TIMER_SLICES.length + 1]
			[TIMER_SLICES.length + 1][BeeWave.BINS];
	transient static double[][][][] accelTimers = new double[ACCEL_INDEXES][TIMER_SLICES.length + 1]
			[TIMER_SLICES.length + 1][BeeWave.BINS];
//	transient static double[][][][][][][][][] all = new double[DISTANCE_SLICES.length + 1][VELOCITY_SLICES.length + 1]
//			[ACCEL_INDEXES][TIMER_SLICES.length + 1][WALL_SLICES.length + 1][WALL_SLICES_REVERSE.length + 1]
//			[TIMER_SLICES.length + 1][TIMER_SLICES.length + 1][BeeWave.BINS];

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
//				all[w.distanceSegment][w.velocitySegment][w.accelSegment][w.vChangeSegment][w.wallSegment][w.reverseWallSegment][w.sinceStationarySegment][w.sinceMaxSpeedSegment],
		};
	}

	@Override
	double getRollingDepth() {
		return 2000;
	}

	@Override
	double getWaveWeight(BeeWave wave) {
		return 1.0;
	}
}

class BeeForgettor extends Guessor {
	static final long serialVersionUID = 7;
	static double[][][][][][] faster = new double[DISTANCE_SLICES_FASTER.length + 1][VELOCITY_SLICES_FASTER.length + 1]
			[ACCEL_INDEXES][TIMER_SLICES_FASTER.length + 1][WALL_SLICES_FASTER.length + 1][BeeWave.BINS];
	static double[][][] distVel = new double[DISTANCE_SLICES.length + 1][VELOCITY_SLICES.length + 1][BeeWave.BINS];
	transient static double[][][][] distWall = new double[DISTANCE_SLICES.length + 1]
			[WALL_SLICES.length + 1][WALL_SLICES_REVERSE.length + 1][BeeWave.BINS];
	transient static double[][][][] accelWall = new double[ACCEL_INDEXES][WALL_SLICES.length + 1]
			[WALL_SLICES_REVERSE.length + 1][BeeWave.BINS];
	transient static double[][][][][][][] slower = new double[DISTANCE_SLICES.length + 1][VELOCITY_SLICES.length + 1]
			[ACCEL_INDEXES][TIMER_SLICES.length + 1][WALL_SLICES.length + 1][WALL_SLICES_REVERSE.length + 1][BeeWave.BINS];
	transient static double[][][][][][][] distVelWallTimers = new double[DISTANCE_SLICES.length + 1][VELOCITY_SLICES.length + 1]
			[TIMER_SLICES.length + 1][WALL_SLICES.length + 1][TIMER_SLICES.length + 1][TIMER_SLICES.length + 1][BeeWave.BINS];
	transient static double[][][][] velTimers = new double[VELOCITY_SLICES_FASTER.length + 1][TIMER_SLICES.length + 1]
			[TIMER_SLICES.length + 1][BeeWave.BINS];
	transient static double[][][][] accelTimers = new double[ACCEL_INDEXES][TIMER_SLICES.length + 1]
			[TIMER_SLICES.length + 1][BeeWave.BINS];
//	transient static double[][][][][][][][][] all = new double[DISTANCE_SLICES.length + 1][VELOCITY_SLICES.length + 1]
//			[ACCEL_INDEXES][TIMER_SLICES.length + 1][WALL_SLICES.length + 1][WALL_SLICES_REVERSE.length + 1]
//			[TIMER_SLICES.length + 1][TIMER_SLICES.length + 1][BeeWave.BINS];

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
//				all[w.distanceSegment][w.velocitySegment][w.accelSegment][w.vChangeSegment][w.wallSegment][w.reverseWallSegment][w.sinceStationarySegment][w.sinceMaxSpeedSegment],
		};
	}
	
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
