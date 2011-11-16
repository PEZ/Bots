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
		double bulletPower = bulletPower(wave, distance, e.getEnergy(), robot.getEnergy());
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
		Guessor.registerVirtualFire();

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
					Guessor.registerFire();
			}
		}
		lastVelocity = e.getVelocity();
		lastWave = wave;
	}
	
	private double bulletPower(BeeWave wave, double distance, double eEnergy, double rEnergy) {
		if (isTC || distance < 100 || RumbleBot.enemyIsRammer()) {
			return MAX_BULLET_POWER;
		}
		double bulletPower = BULLET_POWER;
		if (wave.currentGuessor().realRating() > 0.12) {
			bulletPower += 0.1;
		}
		if (wave.currentGuessor().realRating() > 0.15) {
			bulletPower += 0.2;
		}
		if (wave.currentGuessor().realRating() > 0.22) {
			bulletPower += 0.4;
		}
		if (rEnergy < 10 && eEnergy > rEnergy) {
			bulletPower = 1.0;
		}
		else if (rEnergy < 20 && eEnergy > rEnergy) {
			bulletPower = 1.4;
		}
		else if (rEnergy < 10 && eEnergy > 3) {
			bulletPower = 1.4;
		}
		else if (rEnergy < 20 && eEnergy > 8) {
			bulletPower = 1.6;
		}
		if (robot.enemyHasFired) {
			if (robot.lastEnemyBulletPower < 0.4) {
				bulletPower = robot.lastEnemyBulletPower;
			}
		}
		return Math.min(bulletPower, eEnergy / 4.0);
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
			BeeWave.realForgettor.registerHit(wave.visitingIndex(), wave);
			BeeWave.virtualForgettor.registerHit(wave.visitingIndex(), wave);
		}
	}
}

class BeeWave extends GunWave {
	static final int BINS = 75;
	static final int MIDDLE_BIN = (BINS - 1) / 2;

	static List<BeeWave> waves;
	static List<BeeWave> bullets;

	static BeeRealisor realisor;
	static BeeVirtualisor virtualisor;
	static BeeRealForgettor realForgettor;
	static BeeVirtualForgettor virtualForgettor;
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
		Collections.sort(guessors);
		currentGuessor = (Guessor)guessors.get(0);
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
		for (Guessor guessor : guessors) {
			guessor.registerVisit(this, guesses);
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
		return PUtils.maxEscapeAngle(bulletVelocity) * 1.2;
	}

	static void initGuessors() {
		guessors = new ArrayList<Guessor>();
		guessors.add(realisor = new BeeRealisor());
		guessors.add(virtualisor = new BeeVirtualisor());
		guessors.add(realForgettor = new BeeRealForgettor());
		guessors.add(virtualForgettor = new BeeVirtualForgettor());
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
			realisor = (BeeRealisor)guessors.get(0);
			virtualisor = (BeeVirtualisor)guessors.get(1);
			realForgettor = (BeeRealForgettor)guessors.get(2);
			virtualForgettor = (BeeVirtualForgettor)guessors.get(3);
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
		orderedGuessors.add(realisor);
		orderedGuessors.add(virtualisor);
		orderedGuessors.add(realForgettor);
		orderedGuessors.add(virtualForgettor);
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


