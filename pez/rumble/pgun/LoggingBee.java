package pez.rumble.pgun;
import pez.rumble.utils.*;
import robocode.*;
import robocode.util.Utils;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.awt.geom.*;

// Bee, a gun by PEZ. For CassiusClay - Sting like a bee!
// http://robowiki.net/?CassiusClay
//
// This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
// http://robowiki.net/?RWPCL
// (Basically it means you must keep the code public.)
//
// $Id: Bee.java,v 1.19 2004/09/22 21:16:25 peter Exp $

public class LoggingBee {
    public static boolean isTC = false; // TargetingChallenge

    static final double WALL_MARGIN = 18;
    static final double MAX_BULLET_POWER = 3.0;
    static final double BULLET_POWER = 1.9;

    static Point2D enemyLocation = new Point2D.Double();
    static double lastVelocity;
    static double timeSinceAccel;
    static double timeSinceDeccel;
    static double lastBearingDirection;
    static double roundNum;
    static Rectangle2D fieldRectangle;
    static long tick;
    static PrintStream logOut;

    double distance;
    AdvancedRobot robot;
    long lastScanTime;


    public LoggingBee(AdvancedRobot robot) {
	this.robot = robot;
	lastVelocity = timeSinceDeccel = timeSinceAccel = lastBearingDirection = 0;
	LoggingGunWave.init();
	BeeLogEntry.init();
	fieldRectangle = PUtils.fieldRectangle(robot, WALL_MARGIN);

	if (roundNum > 0) {
	    System.out.println("range hits given: " + (int)LoggingGunWave.rangeHits + " (average / round: " + java.text.NumberFormat.getNumberInstance().format(LoggingGunWave.rangeHits / roundNum) + ")");
	}
	roundNum++;
    }

    public void onScannedRobot(ScannedRobotEvent e) {
	if (logOut == null) {
	    try {
		logOut = new PrintStream(new RobocodeFileOutputStream(robot.getDataFile(e.getName() + ".txt")));
		BeeLogEntry.printLogHeader(logOut);
	    }
	    catch (IOException err) {
		System.out.println(err);
	    }
	}
	tick += robot.getTime() - lastScanTime;
	lastScanTime = robot.getTime();
	Point2D robotLocation = new Point2D.Double(robot.getX(), robot.getY());
	double bearing = robot.getHeadingRadians() + e.getBearingRadians();
	distance = e.getDistance();
	enemyLocation.setLocation(PUtils.project(robotLocation, bearing, distance));
	double bulletPower = bulletPower(distance, e.getEnergy(), robot.getEnergy());
	double lateralVelocity = e.getVelocity() * Math.sin(e.getHeadingRadians() - bearing);
	double absV = Math.abs(e.getVelocity());
	double absLatV = Math.abs(lateralVelocity);
	double acceleration = absV - Math.abs(lastVelocity);

	timeSinceAccel++;
	timeSinceDeccel++;
	if (acceleration > 0.5) {
	    timeSinceAccel = 0;
	}
	if (acceleration < -0.5) {
	    timeSinceDeccel = 0;
	}
	double timeSinceVChange = Math.min(timeSinceAccel, timeSinceDeccel);

	LoggingGunWave wave = new LoggingGunWave(robot);
	wave.setStartTime(robot.getTime());
	wave.setBulletVelocity(PUtils.bulletVelocity(bulletPower));
	wave.setGunLocation(robotLocation);
	wave.setStartBearing(bearing);
	wave.setTargetLocation(enemyLocation);
	if (absV > 0) {
	    lastBearingDirection = wave.maxEscapeAngle() * PUtils.sign(lateralVelocity);
	}
	double orbitDirection = lastBearingDirection / (double)LoggingGunWave.MIDDLE_BIN;
	wave.setOrbitDirection(orbitDirection);

	double wallDistance = wave.wallDistance(1, fieldRectangle);
	double reverseWallDistance = wave.wallDistance(-1, fieldRectangle);

	wave.distanceIndex = PUtils.index(LoggingGunWave.DISTANCE_SLICES, distance);
	wave.velocityIndex =  PUtils.index(LoggingGunWave.VELOCITY_SLICES, absV);
	wave.lateralVelocityIndex =  PUtils.index(LoggingGunWave.VELOCITY_SLICES, absLatV);
	wave.lastVelocityIndex = PUtils.index(LoggingGunWave.LAST_VELOCITY_SLICES, Math.abs(lastVelocity));
	wave.velocityChangedIndex = PUtils.index(LoggingGunWave.TIMER_SLICES, timeSinceVChange / wave.travelTime());
	wave.wallIndex = PUtils.index(LoggingGunWave.WALL_SLICES, wallDistance);
	wave.reverseWallIndex = PUtils.index(LoggingGunWave.WALL_SLICES_REVERSE, reverseWallDistance);

	if (robot.getOthers() > 0 && lastBearingDirection != 0) {
	    LoggingGunWave.waves.add(wave);
	    wave.logEntry = new BeeLogEntry(robot.getTime(), tick, distance, absV, Math.abs(lastVelocity), timeSinceDeccel / wave.travelTime(), timeSinceAccel / wave.travelTime(), wallDistance, reverseWallDistance);
	}
	LoggingGunWave.updateWaves();

	Point2D nextRobotLocation = PUtils.project(robotLocation, robot.getHeadingRadians(), robot.getVelocity());
	Point2D nextEnemyLocation = PUtils.project(enemyLocation, e.getHeadingRadians(), e.getVelocity());
	double nextBearing = PUtils.absoluteBearing(nextRobotLocation, nextEnemyLocation);
	double guessedBearing = nextBearing + orbitDirection * (wave.mostVisited() - LoggingGunWave.MIDDLE_BIN);
	robot.setTurnGunRightRadians(Utils.normalRelativeAngle(guessedBearing - robot.getGunHeadingRadians()));
	if (isTC || (robot.getEnergy() >= 0.3 || e.getEnergy() < robot.getEnergy() / 5 || distance < 120)) {
	    if (Math.abs(robot.getGunTurnRemainingRadians()) < PUtils.botWidthAngle(distance) / 2 && robot.setFireBullet(bulletPower) != null) {
		if (bulletPower > 1.2) {
		    LoggingGunWave.shots[wave.distanceIndex]++;
		}
		wave.weight = 5;
	    }
	}

	lastVelocity = e.getVelocity();
    }

    public void onBulletHit(BulletHitEvent e) {
	if (e.getBullet().getPower() > 1.2) {
	    LoggingGunWave.hits[PUtils.index(LoggingGunWave.DISTANCE_SLICES, distance)]++;
	    if (distance > 150) {
		LoggingGunWave.rangeHits++;
	    }
	}
    }

    public void roundOver() {
	if (logOut != null) {
	    BeeLogEntry.printLog(logOut);
	}
	if (PUtils.isLastRound(robot)) {
	    System.out.println("Bee says: All systems terminate!");
	    if (logOut != null) {
		logOut.close();
	    }
	}
    }

    double bulletPower(double distance, double eEnergy, double rEnergy) {
	double wantedBulletPower = (isTC || distance < 130) ? MAX_BULLET_POWER : BULLET_POWER;
	double bulletPower = wantedBulletPower;
	if (!isTC) {
	    bulletPower = Math.min(Math.min(eEnergy / 4, rEnergy / (distance >= 130 ? 5 : 1)), wantedBulletPower);
	}

	return bulletPower;
    }
}

class LoggingGunWave extends Wave {
    static final double[] DISTANCE_SLICES = { 150, 300, 450, 600 };
    static final double[] VELOCITY_SLICES = { 2, 5, 8 };
    static final double[] LAST_VELOCITY_SLICES = { 2, 4, 6, 8 };
    static final double[] WALL_SLICES = { 0.4, 0.7, 1.1 };
    static final double[] WALL_SLICES_REVERSE = { 0.5 };
    static final double[] TIMER_SLICES = { 0.07, 0.2, 0.7, 1.0 };
    static final int BINS = 27;
    static final int MIDDLE_BIN = (BINS - 1) / 2;

    static double[] shots = new double[DISTANCE_SLICES.length + 1];
    static double[] hits = new double[DISTANCE_SLICES.length + 1];

    static double[][][][][][][] factorsSlower = new double[DISTANCE_SLICES.length + 1][VELOCITY_SLICES.length + 1][LAST_VELOCITY_SLICES.length + 1][TIMER_SLICES.length + 1][WALL_SLICES.length + 1][WALL_SLICES_REVERSE.length + 1][BINS];

    static List waves;
    static double rangeHits;

    int distanceIndex;
    int velocityIndex;
    int lateralVelocityIndex;
    int lastVelocityIndex;
    int velocityChangedIndex;
    int timeSinceAccelIndex;
    int timeSinceDeccelIndex;
    int reverseWallIndex;
    int wallIndex;
    double weight = 1;

    BeeLogEntry logEntry;

    static void init() {
	waves = new ArrayList();
    }

    public LoggingGunWave(AdvancedRobot robot) {
	init(robot, BINS);
    }

    static void updateWaves() {
	List reap = new ArrayList();
	for (int i = 0, n = waves.size(); i < n; i++) {
	    LoggingGunWave wave = (LoggingGunWave)waves.get(i);
	    wave.setDistanceFromGun((robot.getTime() - wave.getStartTime()) * wave.getBulletVelocity());
	    if (wave.passed(18)) {
		if (wave.getRobot().getOthers() > 0) {
		    wave.registerVisit();
		}
		reap.add(wave);
	    }
	}
	for (int i = 0, n = reap.size(); i < n; i++) {
	    waves.remove(reap.get(i));
	}
    }

    void registerVisit() {
	int index = Math.max(1, visitingIndex());
	double[][] buffers = statBuffers();
	for (int i = 0; i < buffers.length; i++) {
	    registerVisit(buffers[i], index);
	}
	logEntry.GF = getGF(targetLocation);
	logEntry.visitIndex = index;
	if (logEntry != null) {
	    BeeLogEntry.log.add(logEntry);
	}
    }

    void registerVisit(double[] buffer, int index) {
	buffer[0]++;
	buffer[index] += weight;
    }

    int mostVisited() {
	int mostVisitedIndex = MIDDLE_BIN;
	double most = 0;
	for (int i = 1; i < BINS; i++) {
	    double visits = 0;
	    double[][] buffers = statBuffers();
	    for (int b = 0; b < buffers.length; b++) {
		visits += buffers[b][i] / Math.max(1, buffers[b][0]);
	    }
	    if (visits > most) {
		mostVisitedIndex = i;
		most = visits;
	    }
	}
	return mostVisitedIndex;
    }

    double[][] statBuffers() {
	return new double[][] {
	    factorsSlower[distanceIndex][velocityIndex][lastVelocityIndex][velocityChangedIndex][wallIndex][reverseWallIndex]
	};
    }

    static double hitRate(int index) {
	return hits[index] / (shots[index] + 1);
    }
}

class BeeLogEntry {
    static List log;

    long time;
    long tick;
    double distance;
    double velocity;
    double lastVelocity;
    double timeSinceDeccel;
    double timeSinceAccel;
    double wallDistance;
    double reverseWallDistance;
    double GF;
    int visitIndex;

    public BeeLogEntry(long time, long tick, double distance, double velocity, double lastVelocity, double timeSinceDeccel, double timeSinceAccel, double wallDistance, double reverseWallDistance) {
	this.time = time;
	this.tick = tick;
	this.distance = distance;
	this.velocity = velocity;
	this.lastVelocity = lastVelocity;
	this.timeSinceDeccel = timeSinceDeccel;
	this.timeSinceAccel = timeSinceAccel;
	this.wallDistance = wallDistance;
	this.reverseWallDistance = reverseWallDistance;
    }

    static String roundedFormat(double v, double f) {
	return PUtils.formatNumber(Math.round(v / f) * f);
    }

    public String toString() {
	return time + "\t" +
	       roundedFormat(distance, 1) + "\t" +
	       roundedFormat(velocity, 1) + "\t" +
	       roundedFormat(lastVelocity, 1) + "\t" +
	       PUtils.formatNumber(Math.min(timeSinceDeccel, timeSinceAccel)) + "\t" +
	       PUtils.formatNumber(wallDistance) + "\t" +
	       PUtils.formatNumber(reverseWallDistance) + "\t" +
	       PUtils.formatNumber(GF) + "\t" +
	       visitIndex;
    }

    static void printLogHeader(PrintStream out) {
	out.println("Time\tDistance\tVelocity\tLastVelocity\tTimeSinceVChange\tWallDistance\tReverseWallDistance\tGF\tvisitIndex");
    }

    static void init() {
	log = new ArrayList();
    }

    static void printLog(PrintStream out) {
	for (int i = 0, n = log.size(); i < n; i++) {
	    out.println(log.get(i));
	}
    }
}
