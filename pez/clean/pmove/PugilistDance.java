package pez.clean.pmove;
import robocode.*;
import robocode.util.Utils;
import java.util.*;
import java.awt.geom.*;

// PugilistDance, by PEZ. Dance like a butterfly!
// The Pugilist movement in a almost pluggable package.
// http://robowiki.net/?PugilistPunch
//
// This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
// http://robowiki.net/?RWPCL
// (Basically it means you must keep the code public if you base any bot on it.)
//
// $Id: PugilistPunch.java,v 1.3 2004/05/19 14:44:58 peter Exp $


public class PugilistDance {
    static final double MAX_VELOCITY = 8;

    static final double MAX_WALL_SMOOTH_TRIES = 100;
    static final double WALL_MARGIN = 25;

    static final double MAX_DISTANCE = 900;

    static Rectangle2D fieldRectangle;
    static Point2D robotLocation = new Point2D.Double();
    static Point2D enemyLocation = new Point2D.Double();
    static double enemyAbsoluteBearing;
    static double enemyDistance;
    static int distanceIndex;
    static double enemyVelocity;
    double enemyEnergy;
    static int enemyTimeSinceDeccel;
    static double enemyBearingDirection = 0.73;

    static double enemyFirePower = 2.5;
    static int lastRobotVelocityIndex;
    static double robotVelocity;
    static int enemyHits;

    double direction = 1.0;

    AdvancedRobot robot;

    public PugilistDance(AdvancedRobot robot) {
	this.robot = robot;

	fieldRectangle = new Rectangle2D.Double(WALL_MARGIN, WALL_MARGIN,
	    robot.getBattleFieldWidth() - WALL_MARGIN * 2, robot.getBattleFieldHeight() - WALL_MARGIN * 2);

	Wave.passingWave = null;
    }

    public void onScannedRobot(ScannedRobotEvent e) {
	Wave ew = new Wave(robot, this);
	ew.gunLocation = new Point2D.Double(enemyLocation.getX(), enemyLocation.getY());
	ew.startBearing = enemyAbsoluteBearing = ew.gunBearing(robotLocation);

	double enemyDeltaEnergy = enemyEnergy - e.getEnergy();
	if (enemyDeltaEnergy > 0 && enemyDeltaEnergy <= 3.0) {
	    enemyFirePower = enemyDeltaEnergy;
	    ew.surfable = true;
	}
	enemyEnergy = e.getEnergy();
	ew.bulletVelocity = 20 - 3 * enemyFirePower;

	direction = robotBearingDirection(ew.startBearing);
	ew.bearingDirection = Math.asin(MAX_VELOCITY / ew.bulletVelocity) * direction / (double)Wave.MIDDLE_FACTOR;

	ew.visits = Wave.factors
	    [distanceIndex = (int)Math.min(Wave.DISTANCE_INDEXES - 1, (enemyDistance / (MAX_DISTANCE / Wave.DISTANCE_INDEXES)))]
	    [lastRobotVelocityIndex]
	    [lastRobotVelocityIndex = (int)Math.abs(robotVelocity / 2)]
	    ;
	robotVelocity = robot.getVelocity();
	ew.targetLocation = robotLocation;

	robotLocation.setLocation(new Point2D.Double(robot.getX(), robot.getY()));
	enemyAbsoluteBearing = robot.getHeadingRadians() + e.getBearingRadians();
	enemyLocation.setLocation(project(robotLocation, enemyAbsoluteBearing, enemyDistance));
	enemyDistance = e.getDistance();

	ew.advance(2);
	robot.addCustomEvent(ew);

	if (Wave.visitsReverse < Wave.visitsForward) {
	    direction = -direction;
	}
	Wave.visitsForward = Wave.visitsReverse = 0;
    }

    public void onHitByBullet(HitByBulletEvent e) {
	Wave wave = Wave.passingWave;
	if (wave != null) {
	    wave.registerVisits(++enemyHits);
	}
    }

    public void doMove() {
	double angle;
	robot.setAhead(Math.cos(angle = absoluteBearing(robotLocation, wallSmoothedDestination(robotLocation, direction)) - robot.getHeadingRadians()) * 100);
	robot.setTurnRightRadians(Math.tan(angle));
    }

    static Point2D wallSmoothedDestination(Point2D location, double direction) {
	Point2D destination;
	double tries;
	int i = 1;
	do {
	    tries = 0;
	    while (!fieldRectangle.contains(destination = project(enemyLocation,
			    absoluteBearing(enemyLocation, location) + direction * Math.min(0.3, Math.atan2(120, enemyDistance)),
			    location.distance(enemyLocation) * (1.2 - tries / 100))) && tries < MAX_WALL_SMOOTH_TRIES) {
		tries++;
	    }
	    direction = -direction;
	} while (i-- > 0 && distanceIndex < 1 && tries > 27);
	return destination;
    }

    void updateDirectionStats(Wave wave) {
	Wave.visitsReverse += wave.smoothedVisits(waveImpactLocation(wave, -1.0, 5));
	Wave.visitsForward += wave.smoothedVisits(waveImpactLocation(wave, 1.0, 0));
    }

    Point2D waveImpactLocation(Wave wave, double direction, double timeOffset) {
	Point2D impactLocation = new Point2D.Double(robot.getX(), robot.getY());
	double time = timeOffset;
	do {
	    impactLocation = project(impactLocation, absoluteBearing(impactLocation,
		wallSmoothedDestination(impactLocation, direction * robotBearingDirection(wave.gunBearing(robotLocation)))), MAX_VELOCITY);
	    time++;
	} while (wave.distance(impactLocation, (int)time) > 18);
	return impactLocation;
    }

    double robotBearingDirection(double enemyBearing) {
	return sign(robot.getVelocity() * Math.sin(robot.getHeadingRadians() - enemyBearing));
    }

    static Point2D project(Point2D sourceLocation, double angle, double length) {
	return new Point2D.Double(sourceLocation.getX() + Math.sin(angle) * length,
		sourceLocation.getY() + Math.cos(angle) * length);
    }

    static double absoluteBearing(Point2D source, Point2D target) {
	return Math.atan2(target.getX() - source.getX(), target.getY() - source.getY());
    }

    static int sign(double v) {
        return v < 0 ? -1 : 1;
    }

    static double minMax(double v, double min, double max) {
	return Math.max(min, Math.min(max, v));
    }
}

class Wave extends Condition {
    static final int DISTANCE_INDEXES = 5;
    static final int VELOCITY_INDEXES = 5;
    static final int WALL_INDEXES = 2;
    static final int DECCEL_TIME_INDEXES = 6;
    static final int FACTORS = 27;
    static final int MIDDLE_FACTOR = (FACTORS - 1) / 2;

    static int[][][][] factors = new int[DISTANCE_INDEXES][VELOCITY_INDEXES][VELOCITY_INDEXES][FACTORS];
    static int[] fastVisits = new int[FACTORS];
    static double visitsForward;
    static double visitsReverse;
    static Wave passingWave;
    boolean surfable;

    AdvancedRobot robot;
    PugilistDance dancer;
    double bulletVelocity;
    Point2D gunLocation;
    Point2D targetLocation;
    double startBearing;
    double bearingDirection;
    int[] visits;
    double distanceFromGun;

    public Wave(AdvancedRobot robot, PugilistDance dancer) {
	this.robot = robot;
	this.dancer = dancer;
    }

    public boolean test() {
	advance(1);
	if (passed(-18)) {
	    surfable = false;
	    passingWave = this;
	}
	if (passed(18)) {
	    robot.removeCustomEvent(this);
	}
	if (surfable) {
	    dancer.updateDirectionStats(this);
	}
	return false;
    }

    public boolean passed(double distanceOffset) {
	return distanceFromGun > gunLocation.distance(targetLocation) + distanceOffset;
    }

    void advance(int ticks) {
	distanceFromGun += ticks * bulletVelocity;
    }

    int visitingIndex(Point2D target) {
	return (int)PugilistDance.minMax(
	    Math.round(((Utils.normalRelativeAngle(gunBearing(target) - startBearing)) / bearingDirection) + (FACTORS - 1) / 2), 0, FACTORS - 1);
    }

    void registerVisits(int count) {
	int index = visitingIndex(targetLocation);
	visits[index] += count;
	fastVisits[index] += count;
    }

    double gunBearing(Point2D target) {
	return PugilistDance.absoluteBearing(gunLocation, target);
    }

    double distance(Point2D location, int timeOffset) {
	return gunLocation.distance(location) - distanceFromGun - (double)timeOffset * bulletVelocity;
    }

    double smoothedVisits(Point2D destination) {
	return smoothedVisits(visitingIndex(destination));
    }

    double smoothedVisits(int index) {
	double smoothed = 0;
	int i = 1;
	do {
	    smoothed += ((double)(fastVisits[i] / (double)(DISTANCE_INDEXES * VELOCITY_INDEXES * VELOCITY_INDEXES)) +
		(double)visits[i]) / Math.sqrt((double)(Math.abs(index - i) + 1.0));
	    i++;
	} while (i < FACTORS);
	return smoothed / Math.sqrt(distance(targetLocation, 0) / bulletVelocity);
    }
}
