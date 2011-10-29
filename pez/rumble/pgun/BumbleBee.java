package pez.rumble.pgun;
import pez.rumble.utils.*;
import robocode.*;
import robocode.util.Utils;
import java.util.*;
import java.awt.geom.*;

// BumbleBee, a gun by PEZ. For CassiusClay - Sting like a bee! Even if it is a bumble-bee this time... =)
// An attempt to do GF targeting with fuzzy segmentation macthes, using a log of all collected waves.
// http://robowiki.net/?Ali/BumbleBee
//
// This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
// http://robowiki.net/?RWPCL
// (Basically it means you must keep the code public if you base any bot on it.)
//
// $Id: Exp $

public class BumbleBee {
    public static boolean isTC = false; // TargetingChallenge
    public static boolean doGL = false;

    static final double MAX_VELOCITY = 8.0;
    static final double MAX_BULLET_POWER = 3.0;
    static final double WALL_MARGIN = 18;
    static final double MAX_DISTANCE = 900;
    static final double BULLET_POWER = 1.91;

    static Point2D enemyLocation = new Point2D.Double();
    static double distance;
    static double lastVelocity;
    static int timeSinceVChange;
    static double lastBearingDirection = 0.73;
    static int tick;
    static long lastScanTime;

    static double roundNum;

    Rectangle2D fieldRectangle;
    AdvancedRobot robot;
    boolean isAiming;
    ArrayList targetLocations = new ArrayList();

    public BumbleBee(AdvancedRobot robot) {
	this.robot = robot;
	BumbleWave.init();
	fieldRectangle = PUtils.fieldRectangle(robot, WALL_MARGIN);
	if (roundNum > 0) {
	    System.out.println("range hits given: " + (int)BumbleWave.rangeHits + " (average / round: " + PUtils.formatNumber(BumbleWave.rangeHits / roundNum) + ")");
	}
	roundNum++;
    }

    public void onScannedRobot(ScannedRobotEvent e) {
	if (robot.getTime() != lastScanTime) {
	    lastScanTime = robot.getTime();
	    BumbleWave wave = new BumbleWave(robot);
	    wave.logEntry.birthTick = tick++;
	    wave.setStartTime(robot.getTime());
	    if (robot.getOthers() > 0 && robot.getEnergy() > 0) {
		BumbleWave.waves.add(wave);
	    }

	    distance = e.getDistance();

	    double wantedBulletPower = isTC ? MAX_BULLET_POWER : distance > 140 ? BULLET_POWER : MAX_BULLET_POWER;
	    double bulletPower = wantedBulletPower;
	    if (!isTC) {
		bulletPower = Math.min(Math.min(e.getEnergy() / 4, robot.getEnergy() / (distance > 140 ? 7 : 2)), wantedBulletPower);
	    }
	    Point2D robotLocation = new Point2D.Double(robot.getX(), robot.getY());
	    wave.setGunLocation(robotLocation);
	    double bearing = robot.getHeadingRadians() + e.getBearingRadians();
	    wave.setStartBearing(bearing);
	    wave.setTargetLocation(enemyLocation);
	    targetLocations.add(new Point2D.Double(enemyLocation.getX(), enemyLocation.getY()));
	    enemyLocation.setLocation(PUtils.project(robotLocation, bearing, distance));

	    int lastIndex = targetLocations.size() - 1;
	    if (lastIndex > 15) {
		wave.logEntry.distance15 = 100 * enemyLocation.distance((Point2D)targetLocations.get(lastIndex - 15)) / (MAX_VELOCITY * 15);
	    }
	    if (lastIndex > 60) {
		wave.logEntry.distance60 = 100 * enemyLocation.distance((Point2D)targetLocations.get(lastIndex - 60)) / (MAX_VELOCITY * 60);
	    }
	    if (lastIndex > 120) {
		wave.logEntry.distance120 = 1100 * enemyLocation.distance((Point2D)targetLocations.get(lastIndex - 120)) / (MAX_VELOCITY * 120);
	    }
	    if (Math.abs(lastVelocity - e.getVelocity()) > 0.1) {
		timeSinceVChange = 0;
	    }
	    double dV = Math.abs(e.getVelocity()) - Math.abs(lastVelocity);
	    wave.logEntry.accel = 90 * (dV + MAX_VELOCITY) / MAX_VELOCITY;
	    lastVelocity = e.getVelocity();

	    double bulletVelocity = PUtils.bulletVelocity(bulletPower);
	    wave.setBulletVelocity(bulletVelocity);

	    double lateralVelocity = e.getVelocity() * Math.sin(e.getHeadingRadians() - bearing);
	    if (e.getVelocity() != 0) {
		lastBearingDirection = wave.maxEscapeAngle() * PUtils.sign(lateralVelocity);
	    }
	    double orbitDirection = lastBearingDirection / (double)BumbleWave.MIDDLE_FACTOR;
	    wave.setOrbitDirection(orbitDirection);

	    for (int i = 1; i < 101; i += BumbleWave.WALL_INDEX_WIDTH) {
		wave.logEntry.wallDistance = i;
		if (!fieldRectangle.contains(PUtils.project(robotLocation,
				bearing + orbitDirection * (double)(wave.logEntry.wallDistance * BumbleWave.WALL_INDEX_WIDTH), distance))) {
		    break;
		}
	    }

	    wave.logEntry.velocityChangedTimer = 100 * timeSinceVChange / (double)wave.travelTime();
	    timeSinceVChange++;

	    BumbleWave.updateWaves();

	    Point2D nextRobotLocation = PUtils.project(robotLocation, robot.getHeadingRadians(), robot.getVelocity());
	    Point2D nextEnemyLocation = PUtils.project(enemyLocation, e.getHeadingRadians(), e.getVelocity());
	    double nextBearing = PUtils.absoluteBearing(nextRobotLocation, nextEnemyLocation);
	    double guessedBearing = nextBearing;
	    boolean shouldFire = shouldFire(e);
	    if (robot.getGunHeat() > robot.getGunCoolingRate()) {
		isAiming = false;
	    }
	    else {
		if (isAiming && shouldFire && robot.getGunTurnRemaining() == 0) {
		    robot.setFire(bulletPower);
		    isAiming = false;
		}
		else {
		    guessedBearing += orbitDirection * (wave.mostVisited() - BumbleWave.MIDDLE_FACTOR);
		    isAiming = true;
		}
	    }
	    robot.setTurnGunRightRadians(Utils.normalRelativeAngle(guessedBearing - robot.getGunHeadingRadians()));
	}
    }

    boolean shouldFire(ScannedRobotEvent e) {
	return (isTC || e.getEnergy() > 0 && (robot.getEnergy() >= BULLET_POWER || e.getDistance() < 140));
    }

    public void onBulletHit(BulletHitEvent e) {
	BumbleWave.hits++;
	if (e.getBullet().getPower() > 1.2 && distance > 150) {
	    BumbleWave.rangeHits++;
	}
    }

    public void roundOver() {
	System.out.println("log size: " + BumbleWave.log.size());
	BumbleWave.cleanLog();
    }
}

class BumbleWave extends Wave {
    static final double WALL_INDEX_WIDTH = 1.5;
    static final int FACTORS = 47;
    static final int MIDDLE_FACTOR = (FACTORS - 1) / 2;
    static final int LOG_SIZE = 7500;
    static final int MAX_MATCHES = 50;

    static List waves;
    static double hits;
    static double rangeHits;
    static List log = new ArrayList();

    LogEntry logEntry = new LogEntry();

    static void init() {
	waves = new ArrayList();
    }

    public BumbleWave(AdvancedRobot robot) {
	init(robot, FACTORS);
    }

    static void updateWaves() {
	List reap = new ArrayList();
	for (int i = 0, n = waves.size(); i < n; i++) {
	    BumbleWave wave = (BumbleWave)waves.get(i);
	    wave.setDistanceFromGun((robot.getTime() - wave.getStartTime()) * wave.getBulletVelocity());
	    if (wave.passed(10)) {
		if (wave.getRobot().getOthers() > 0) {
		    wave.registerVisits();
		}
		reap.add(wave);
	    }
	}
	for (int i = 0, n = reap.size(); i < n; i++) {
	    waves.remove(reap.get(i));
	}
    }

    void registerVisits() {
	logEntry.visitIndex = Math.max(1, visitingIndex());
	log.add(logEntry);
    }

    int mostVisited() {
	Collections.sort(log, new DistanceComparator(logEntry));
	int[] visits = new int[FACTORS];
	for (int i = 0, n = Math.min(MAX_MATCHES, log.size()); i < n; i++) {
	    visits[((LogEntry)log.get(i)).visitIndex]++;
	}
	int mostVisited = MIDDLE_FACTOR;
	for  (int i = 1; i < FACTORS; i++) {
	    if (visits[i] > visits[mostVisited]) {
		mostVisited = i;
	    }
	}
	return mostVisited;
    }

    static double hitRate() {
	return rangeHits / (BumbleBee.roundNum + 1);
    }

    static void cleanLog() {
	if (log.size() > (int)(LOG_SIZE)) {
	    Collections.sort(log, LogEntry.AGE_COMPARATOR);
	    log.subList(LOG_SIZE - 1, log.size() - 1).clear();
	}
    }
}

class LogEntry {
    static final AgeComparator AGE_COMPARATOR = new AgeComparator();

    int birthTick;
    int visitIndex;

    double distance15;
    double distance60;
    double distance120;
    double accel;
    double velocityChangedTimer;
    double wallDistance;

    double distance(LogEntry e) {
	double d1 = this.wallDistance - e.wallDistance;
	double d2 = this.distance15 - e.distance15;
	double d3 = this.distance60 - e.distance60;
	double d4 = this.distance120 - e.distance120;
	double d5 = this.velocityChangedTimer - e.velocityChangedTimer;
	double d6 = this.accel - e.accel;
	return d1 * d1 + d2 * d2 + d3 * d3 + d4 * d4 + d5 * d5 + d6 * d6;
	    
    }


    double age() {
	return BumbleBee.tick - this.birthTick;
    }

    static class AgeComparator implements Comparator {
	public int compare(Object a, Object b) {
	    return (int)(((LogEntry)b).age() - ((LogEntry)a).age());
	}
    }
}

class DistanceComparator implements Comparator {
    LogEntry logEntry;

    DistanceComparator(LogEntry e) {
	logEntry = e;
    }

    public int compare(Object a, Object b) {
	return (int)(((LogEntry)a).distance(logEntry) - ((LogEntry)b).distance(logEntry));
    }
}
