package pez.clean.pgun;
import robocode.*;
import robocode.util.Utils;
import java.util.*;
import java.awt.geom.*;

// PugilistSting, by PEZ. Sting like a bee!
// The Pugilist gun in a almost pluggable package.
// http://robowiki.net/?PugilistPunch
//
// This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
// http://robowiki.net/?RWPCL
// (Basically it means you must keep the code public if you base any bot on it.)
//
// $Id: PugilistPunch.java,v 1.3 2004/05/19 14:44:58 peter Exp $

public class PugilistSting {
    public static boolean isTC = false; // TargetingChallenge

    static final double MAX_VELOCITY = 8;
    static final double WALL_MARGIN = 25;
    static final double MAX_DISTANCE = 900;
    static final double MAX_BULLET_POWER = 3.0;
    static final double BULLET_POWER = 2.0;

    static Point2D enemyLocation = new Point2D.Double();
    static double lastEnemyVelocity;
    static int enemyTimeSinceDeccel;
    static double lastEnemyBearingDirection = 0.73;

    Rectangle2D fieldRectangle;
    AdvancedRobot robot;

    public PugilistSting(AdvancedRobot robot) {
	this.robot = robot;
	fieldRectangle = new Rectangle2D.Double(WALL_MARGIN, WALL_MARGIN,
	    robot.getBattleFieldWidth() - WALL_MARGIN * 2, robot.getBattleFieldHeight() - WALL_MARGIN * 2);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
	double enemyDistance = e.getDistance();
	int distanceIndex = (int)Math.min(Wave.DISTANCE_INDEXES - 1, (enemyDistance / (MAX_DISTANCE / Wave.DISTANCE_INDEXES)));

	double bulletPower;
	if (isTC) {
	    bulletPower = MAX_BULLET_POWER;
	}
	else {
	    bulletPower = Math.min(robot.getEnergy() / 4, distanceIndex > 0 ? BULLET_POWER : MAX_BULLET_POWER);
	}

	Wave wave = new Wave(robot);
	wave.gunLocation = new Point2D.Double(robot.getX(), robot.getY());
	wave.startBearing = robot.getHeadingRadians() + e.getBearingRadians();
	wave.targetLocation = enemyLocation;
	enemyLocation.setLocation(PUtils.project(wave.gunLocation, wave.startBearing, enemyDistance));

	int velocityIndex = (int)Math.abs(e.getVelocity() / 2);
	int lastVelocityIndex = (int)Math.abs(lastEnemyVelocity / 2);
	lastEnemyVelocity = e.getVelocity();
	if (velocityIndex < lastVelocityIndex) {
	    enemyTimeSinceDeccel = 0;
	}

	wave.bulletVelocity = 20 - 3 * bulletPower;

	if (e.getVelocity() != 0) {
	    lastEnemyBearingDirection = 0.73 * PUtils.sign(e.getVelocity() * Math.sin(e.getHeadingRadians() - wave.startBearing));
	}
	wave.bearingDirection = lastEnemyBearingDirection / (double)Wave.MIDDLE_FACTOR;

	int wallIndex = 0;
	do {
	} while (++wallIndex < (Wave.WALL_INDEXES) &&
	    fieldRectangle.contains(PUtils.project(wave.gunLocation, wave.startBearing + wave.bearingDirection * (double)(wallIndex * 10), enemyDistance)));
	wallIndex -= 1;

	wave.visits = Wave.factors[distanceIndex][velocityIndex][lastVelocityIndex]
	    [(int)PUtils.minMax(Math.pow(enemyTimeSinceDeccel++, 0.45) - 1, 0, Wave.DECCEL_TIME_INDEXES - 1)][wallIndex];

	robot.setTurnGunRightRadians(Utils.normalRelativeAngle(wave.startBearing - robot.getGunHeadingRadians() +
		    wave.bearingDirection * (wave.mostVisited() - Wave.MIDDLE_FACTOR)));

	if (isTC || robot.getEnergy() >= BULLET_POWER) {
	    robot.setFire(bulletPower);
	    robot.addCustomEvent(wave);
	}
    }
}

class PUtils {
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
    static final int WALL_INDEXES = 3;
    static final int DECCEL_TIME_INDEXES = 6;
    static final int FACTORS = 27;
    static final int MIDDLE_FACTOR = (FACTORS - 1) / 2;

    static int[][][][][][] factors = new int[DISTANCE_INDEXES][VELOCITY_INDEXES][VELOCITY_INDEXES]
	[DECCEL_TIME_INDEXES][WALL_INDEXES][FACTORS];

    AdvancedRobot robot;
    double bulletVelocity;
    Point2D gunLocation;
    Point2D targetLocation;
    double startBearing;
    double bearingDirection;
    int[] visits;
    double distanceFromGun;

    public Wave(AdvancedRobot robot) {
	this.robot = robot;
    }
    
    public boolean test() {
	advance(1);
	if (passed(-18)) {
	    if (robot.getOthers() > 0) {
		registerVisits(1);
	    }
	    robot.removeCustomEvent(this);
	}
	return false;
    }

    public boolean passed(double distanceOffset) {
	return distanceFromGun > gunLocation.distance(targetLocation) + distanceOffset;
    }

    void advance(int ticks) {
	distanceFromGun += ticks * bulletVelocity;
    }

    int visitingIndex() {
	return (int)PUtils.minMax(
	    Math.round(((Utils.normalRelativeAngle(PUtils.absoluteBearing(gunLocation, targetLocation) - startBearing)) / bearingDirection) + (FACTORS - 1) / 2), 0, FACTORS - 1);
    }

    void registerVisits(int count) {
	visits[visitingIndex()] += count;
    }

    int mostVisited() {
	int mostVisited = MIDDLE_FACTOR, i = FACTORS - 1;
	do  {
	    if (visits[--i] > visits[mostVisited]) {
		mostVisited = i;
	    }
	} while (i > 0);
	return mostVisited;
    }
}
