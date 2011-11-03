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
		if (acceleration > 0.5) {
			timeSinceAccel = 0;
		}
		if (acceleration < -0.5) {
			timeSinceDeccel = 0;
		}
		wave.timeSinceVChange = Math.min(timeSinceAccel, timeSinceDeccel);
		wave.timeSinceAccel = timeSinceAccel;
		wave.timeSinceDeccel = timeSinceDeccel;

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
				Math.abs(robot.getGunTurnRemainingRadians()) < PUtils.botWidthAngle(distance) / 2 &&
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
		//System.out.println(BeeWave.guessors.toString());
		//System.out.println("Using: " + BeeWave.currentGuessor.echoStats());
	}

	void saveStats() {
		BeeWave.saveStats(robot);
	}
	
	
	public void roundOver() {
		if (PUtils.isLastRound(robot)) {
			if (!isTC) {
				saveStats();
			}
			System.out.println("Bzzz bzzz. Over and out!");
		}
	}

	void bulletHit(BulletHitEvent e) {
		Bullet b = e.getBullet();
		BeeWave wave = (BeeWave)Wave.findClosest(BeeWave.bullets, new Point2D.Double(b.getX(), b.getY()), b.getVelocity());
		if (wave != null) {
			wave.currentGuessor().registerHit(b.getPower(), distance);
			BeeWave.BeeReplacor.registerHit(wave);
		}
	}
}

class BeeWave extends GunWave {
	static final int BINS = 115;
	static final int MIDDLE_BIN = (BINS - 1) / 2;

	static List<BeeWave> waves;
	static List<BeeWave> bullets;

	static BeeAccumulator BeeAccumulator;
	static BeeReplacor BeeReplacor;
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
	double wallDistance;
	double reverseWallDistance;
	int accelSegment;
	int distanceSegment;
	int distanceSegmentFaster;
	int velocitySegment;
	int velocitySegmentFaster;
	int vChangeSegment;
	int vChangeSegmentFaster;
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
		if (BeeAccumulator.rounds > Guessor.RATING_UPDATE_START && BeeAccumulator.rounds <= Guessor.RATING_UPDATE_STOP &&  BeeAccumulator.rounds % Guessor.RATING_UPDATE_ROUNDS == 0) {
			if (BeeAccumulator.vRating() > Guessor.BeeAccumulator_REWARD_TRIGGER_VRATING) {
				BeeAccumulator.incrementRating();
			}
			Collections.sort(guessors, Guessor.getVirtualStatsComparator());
			double firstVR = ((Guessor)guessors.get(0)).vRating();
			double secondVR = ((Guessor)guessors.get(1)).vRating();
			double vDiff =  firstVR - secondVR;
			double vDiffSize = vDiff / (firstVR + secondVR);
			if (vDiffSize > Guessor.RATING_INCREMENT_THRESHOLD) {
				((Guessor)guessors.get(0)).incrementRating();
			}
		}
		Collections.sort(guessors);
		currentGuessor = BeeAccumulator; //(Guessor)guessors.get(0);
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
		if (!Bee.isTC) {
			Map<String, List<Guessor>> enemies = readEnemies(robot);
			guessors = (ArrayList<Guessor>)enemies.get(Bee.enemyName);
		}
		if (guessors != null) {
			BeeAccumulator = (BeeAccumulator)guessors.get(0);
			BeeReplacor = (BeeReplacor)guessors.get(1);
			System.out.println("Fetched Guessor data for enemy: " + Bee.enemyName + "\n\t" + BeeWave.guessors.toString());
		}
		else {
			System.out.println("No vgstats for " + Bee.enemyName + " on file yet.");
			guessors = new ArrayList<Guessor>();
			guessors.add(BeeAccumulator = new BeeAccumulator());
			guessors.add(BeeReplacor = new BeeReplacor());
		}
	}

	static void saveStats(AdvancedRobot robot) {
		Map<String, List<Guessor>> enemies = readEnemies(robot);
		List<Guessor> orderedGuessors = new ArrayList<Guessor>();
		orderedGuessors.add(BeeAccumulator);
		orderedGuessors.add(BeeReplacor);
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
	static final long serialVersionUID = 4;
	static final int ACCEL_INDEXES = 3;
	static final double[] DISTANCE_SLICES = { 150, 300, 450, 600 };
	static final double[] DISTANCE_SLICES_FASTER = { 300, 500 };
	static final double[] VELOCITY_SLICES = { 1, 3, 5, 7 };
	static final double[] VELOCITY_SLICES_FASTER = { 2, 4, 6 };
	static final double[] WALL_SLICES = { 0.15, 0.35, 0.55, 0.75 };
	static final double[] WALL_SLICES_FASTER = { 0.25, 0.5, 0.75 };
	static final double[] WALL_SLICES_REVERSE = { 0.35, 0.7 };
	static final double[] TIMER_SLICES = { 0.1, 0.3, 0.7, 1.2 };
	static final double[] TIMER_SLICES_FASTER = { 0.1, 0.3, 0.7 };
	static final int RATING_UPDATE_ROUNDS = 1;
	static final int RATING_UPDATE_START = 5;
	static final int RATING_UPDATE_STOP = 50;
	static final double RATING_INCREMENT_THRESHOLD = 0.02;
	static final double VRATING_START = 80;
	static final double VRATING_ROLL_DEPTH = 300;
	static final double BeeAccumulator_REWARD_TRIGGER_VRATING = 90;
	static final double BeeAccumulator_VRATING_BONUS = 10;
	static final double BeeAccumulator_RATING_INCREMENT = 1;
	static final double BeeReplacor_RATING_INCREMENT = 2;
	double rating;
	private long rBulletsFired;
	private double rRating;
	protected double vRating;
	int rounds;
	transient private int guess;

	abstract void registerVisit(int index, BeeWave w);
	abstract double[][] buffers(BeeWave w);
	abstract void incrementRating();

	void registerVisit(BeeWave w, Map<Guessor, Integer> guesses) {
		int index = Math.max(1, w.visitingIndex());
		if (w.weight > 2.0) {
			updateVRating(index, ((Integer)guesses.get(this)).intValue(), w);
		}
		for (int i = Math.max(1, index - w.botWidth()), n = Math.min(BeeWave.BINS, index + w.botWidth()); i < n; i++) {
			registerVisit(i, w);
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
				visits += uses * buffers[b][i] / Math.max(1, buffers[b][0]);
			}
			visitRanks.add(new VisitsIndex(visits, i));
		}
		Collections.sort(visitRanks);
		return ((VisitsIndex)visitRanks.get(0)).index;
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
		rRating += reward(1, distance);
	}

	double reward(double reward, double distance) {
		return reward * distance / 100;
	}

	double hit(double diff, BeeWave w) {
		return w.hit(diff) ? 100 : 0;
	}

	void updateVRating(int index, int guess, BeeWave w) {
		vRating = PUtils.rollingAvg(vRating, reward(hit(guess - index, w), w.distance), VRATING_ROLL_DEPTH);
	}

	double rRating() {
		return rRating / rBulletsFired;
	}

	double vRating() {
		return vRating;
	}

	public int compareTo(Object o) {
		double ratingA = this.vRating();
		double ratingB = ((Guessor)o).vRating();
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
		return name() + " vr" + logNum(vRating) +
		" rr" + logNum(rRating());
	}

	static String logHeader(String tag) {
		return "Rounds " + tag + "\t" + 
		"vRating " + tag + "\t" + 
		"rRating " + tag;
	}

	static String logNum(double num) {
		return java.text.NumberFormat.getNumberInstance().format(num);
	}

	String logRow() {
		return rounds + "\t" +
		logNum(vRating) + "\t" +
		logNum(rRating);
	}

	public String toString() {
		return echoStats();
	}
}

class BeeAccumulator extends Guessor {
	static final long serialVersionUID = 4;
	private static double[][][][][][] faster = new double[DISTANCE_SLICES_FASTER.length + 1][VELOCITY_SLICES_FASTER.length + 1][ACCEL_INDEXES][TIMER_SLICES_FASTER.length + 1][WALL_SLICES_FASTER.length + 1][BeeWave.BINS];
	private static double[][][][][][][] slower = new double[DISTANCE_SLICES.length + 1][VELOCITY_SLICES.length + 1][ACCEL_INDEXES][TIMER_SLICES.length + 1][WALL_SLICES.length + 1][WALL_SLICES_REVERSE.length + 1][BeeWave.BINS];

	BeeAccumulator() {
		vRating = Guessor.VRATING_START + Guessor.BeeAccumulator_VRATING_BONUS;
	}

	void incrementRating() {
		rating += Guessor.BeeAccumulator_RATING_INCREMENT;
	}

	double[][] buffers(BeeWave w) {
		return new double[][] {
				faster[w.distanceSegmentFaster][w.velocitySegmentFaster][w.accelSegment][w.vChangeSegmentFaster][w.wallSegmentFaster],
				slower[w.distanceSegment][w.velocitySegment][w.accelSegment][w.vChangeSegment][w.wallSegment][w.reverseWallSegment],
		};
	}

	void registerVisit(int index, BeeWave w) {
		double[][] buffers = buffers(w);
		for (int b = 0; b < buffers.length; b++) {
			buffers[b][0]++;
			for (int i = 1; i < BeeWave.BINS; i++) {
				//buffers[b][i] += w.weight / (Math.pow(Math.abs(i - index) + 1, 1.5));
				buffers[b][i] =  (float)PUtils.rollingAvg(buffers[b][i], index == i ? w.weight : 0.0, 50);
			}
		}
	}
}

class BeeReplacor extends Guessor {
	//stat array 1:latvelocity = {2, 4, 6}distance = {250, 500}walldistance = {0.25, 0.5, 0.75}vchangetime = {.1, .4, 1.0)accel = 0/1/2 (changes of over .2, effectively same as Bee I think)
	//stat array 2:advvelocity = {-2.5, 2.5}latvelocity = {1, 3, 5, 7}distance = {200, 400, 600}walldistance = {0.2, 0.5, 0.8}walldistancereverse = {0.5}accel = 0/1/2 ...vchangetime = {.1, .4, .7, 1.1, 1.5} 
	static final long serialVersionUID = 4;
	private static double[][][][][][] faster = new double[DISTANCE_SLICES_FASTER.length + 1][VELOCITY_SLICES_FASTER.length + 1][ACCEL_INDEXES][TIMER_SLICES_FASTER.length + 1][WALL_SLICES_FASTER.length + 1][BeeWave.BINS];
	private static double[][][][][][][] slower = new double[DISTANCE_SLICES.length + 1][VELOCITY_SLICES.length + 1][ACCEL_INDEXES][TIMER_SLICES.length + 1][WALL_SLICES.length + 1][WALL_SLICES_REVERSE.length + 1][BeeWave.BINS];

	BeeReplacor() {
		vRating = Guessor.VRATING_START;
	}

	void incrementRating() {
		rating += Guessor.BeeReplacor_RATING_INCREMENT;
	}

	double[][] buffers(BeeWave w) {
		return new double[][] {
				faster[w.distanceSegmentFaster][w.velocitySegmentFaster][w.accelSegment][w.vChangeSegmentFaster][w.wallSegmentFaster],
				slower[w.distanceSegment][w.velocitySegment][w.accelSegment][w.vChangeSegment][w.wallSegment][w.reverseWallSegment]
		};
	}

	void registerHit(BeeWave w) {
		double[][] buffers = buffers(w);
		for (int b = 0; b < buffers.length; b++) {
			buffers[b][0]++;
			for (int i = 1; i < BeeWave.BINS; i++) {
				buffers[b][i] = PUtils.rollingAvg(buffers[b][i], -buffers[b][i], 1);
			}
		}
	}
	
	void registerVisit(int index, BeeWave w) {
		double[][] buffers = buffers(w);
		for (int b = 0; b < buffers.length; b++) {
			buffers[b][0]++;
			for (int i = 1; i < BeeWave.BINS; i++) {
				buffers[b][i] = PUtils.rollingAvg(buffers[b][i], w.weight / (Math.pow(Math.abs(i - index) + 1, 1.5)), 3);
			}
		}
	}
}
