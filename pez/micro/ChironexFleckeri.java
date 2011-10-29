package pez.micro;
import robocode.*;
import robocode.util.Utils;
import java.awt.geom.*;

// This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
// http://robowiki.net/?RWPCL
// (Basically it means you must keep the code public if you base any bot on it.)
//
// ChironexFleckeri, by PEZ. Small deadly jelly fish.
//
// ChironexFleckeri explores two major concepts:
//    1. Guess factor targeting, invented by Paul Evans. http://robowiki.net/?GuessFacorTargeting
//    2. Wave surfing movement, invented by ABC. http://robowiki.net/?WaveSurfing
//
// $Id: ChironexFleckeri.java,v 1.1 2004/09/03 23:09:05 peter Exp $

public class ChironexFleckeri extends AdvancedRobot {
    static final double MAX_VELOCITY = 8;
    static final double BATTLE_FIELD_WIDTH = 800;
    static final double BATTLE_FIELD_HEIGHT = 600;

    static final double MAX_WALL_SMOOTH_TRIES = 150;
    static final double WALL_MARGIN = 25;

    static final double MAX_DISTANCE = 900;
    static final double MAX_BULLET_POWER = 3.0;
    static final double BULLET_POWER = 2.0;

    static Point2D robotLocation = new Point2D.Double();
    static Point2D enemyLocation = new Point2D.Double();
    static double enemyAbsoluteBearing;
    static double enemyBearingDirection;
    static double dangerForward;
    static double dangerReverse;
    double enemyEnergy;

    static double enemyFirePower;

    static final int FACTORS = 31;
    static final int MIDDLE_FACTOR = (FACTORS - 1) / 2;

    static double[] hits = new double[FACTORS];
    static double[] visits = new double[FACTORS];
    static ChiroWave passingWave;

    public void run() {
	setAdjustRadarForGunTurn(true);
	setAdjustGunForRobotTurn(true);

	passingWave = null;

	do {
	    turnRadarRightRadians(Double.POSITIVE_INFINITY); 
	} while (true);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
	ChiroWave wave = new ChiroWave();
	ChiroWave ew = new ChiroWave();
	ew.isMovementWave = true;
	ew.gunLocation = new Point2D.Double(enemyLocation.getX(), enemyLocation.getY());
	ew.startBearing = ew.bearing(robotLocation);

	double enemyDeltaEnergy = enemyEnergy - e.getEnergy();
	if (enemyDeltaEnergy > 0 && enemyDeltaEnergy <= 3.0) {
	    enemyFirePower = enemyDeltaEnergy;
	    ew.surfable = true;
	}
	enemyEnergy = e.getEnergy();
	ew.bulletVelocity = 20 - 3 * enemyFirePower;

	double direction = robotBearingDirection(ew.startBearing);
	ew.bearingDirection = Math.asin(MAX_VELOCITY / ew.bulletVelocity) * direction / (double)MIDDLE_FACTOR;

	ew.targetLocation = robotLocation;

	robotLocation.setLocation(new Point2D.Double(getX(), getY()));

	ew.distanceFromGun = 2 * ew.bulletVelocity;
	addCustomEvent(ew);

	// <gun>
	//double bulletPower = MAX_BULLET_POWER; // TargetingChallenge
	enemyAbsoluteBearing = getHeadingRadians() + e.getBearingRadians();
	enemyLocation.setLocation(project(wave.gunLocation = new Point2D.Double(getX(), getY()), enemyAbsoluteBearing, e.getDistance()));
	double bulletPower = e.getDistance() > 140 ? BULLET_POWER : MAX_BULLET_POWER;
	wave.targetLocation = enemyLocation;
	wave.bulletVelocity = 20 - 3 * bulletPower;

	if (e.getVelocity() != 0) {
	    enemyBearingDirection = 0.7 * sign(e.getVelocity() * Math.sin(e.getHeadingRadians() - enemyAbsoluteBearing));
	}
	wave.bearingDirection = enemyBearingDirection / (double)MIDDLE_FACTOR;

	wave.startBearing = enemyAbsoluteBearing;

	setTurnGunRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getGunHeadingRadians() +
		    wave.bearingDirection * (wave.mostVisited() - MIDDLE_FACTOR)));

	if (getEnergy() >= BULLET_POWER) {
	    setFire(bulletPower);
	    addCustomEvent(wave);
	}
	// </gun>

	if (dangerReverse < dangerForward) {
	    direction = -direction;
	}
	dangerForward = dangerReverse = 0;
	double angle;
	setAhead(Math.cos(angle = wave.bearing(wallSmoothedDestination(robotLocation, direction)) - getHeadingRadians()) * 100);
	setTurnRightRadians(Math.tan(angle));

	setTurnRadarRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getRadarHeadingRadians()) * 2);
    }

    public void onHitByBullet(HitByBulletEvent e) {
	hits[passingWave.visitingIndex(passingWave.targetLocation)]++;
    }

    static Point2D wallSmoothedDestination(Point2D location, double direction) {
	Point2D destination;
	double smoothing = 0;
	while (!(new Rectangle2D.Double(WALL_MARGIN, WALL_MARGIN,
	    BATTLE_FIELD_WIDTH - WALL_MARGIN * 2, BATTLE_FIELD_HEIGHT - WALL_MARGIN * 2)).contains(destination = project(location,
			absoluteBearing(location, enemyLocation) - direction * ((1.25 - smoothing / 100) * Math.PI / 2), 135)) &&
		smoothing < MAX_WALL_SMOOTH_TRIES) {
	    smoothing++;
	}
	return destination;
    }

    Point2D waveImpactLocation(ChiroWave wave, double direction, int time) {
	Point2D impactLocation = new Point2D.Double(getX(), getY());
	do {
	    impactLocation = project(impactLocation, absoluteBearing(impactLocation,
		wallSmoothedDestination(impactLocation, direction * robotBearingDirection(wave.bearing(robotLocation)))), MAX_VELOCITY);
	    time++;
	} while (wave.distanceFromTarget(impactLocation, time) > -10);
	return impactLocation;
    }


    double robotBearingDirection(double enemyBearing) {
	return sign(getVelocity() * Math.sin(getHeadingRadians() - enemyBearing));
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

    class ChiroWave extends Condition {
	Point2D gunLocation;
	Point2D targetLocation;
	double startBearing;
	double bulletVelocity;
	double bearingDirection;
	double distanceFromGun;
	boolean isMovementWave;
	boolean surfable;

	public boolean test() {
	    distanceFromGun += bulletVelocity;
	    if (passed(-18)) {
		visits[visitingIndex(targetLocation)]++;
		surfable = false;
		if (isMovementWave) {
		    passingWave = this;
		}
	    }
	    if (passed(18)) {
		removeCustomEvent(this);
	    }
	    if (surfable) {
		dangerReverse += danger(waveImpactLocation(this, -1.0, 5));
		dangerForward += danger(waveImpactLocation(this, 1.0, 0));
	    }
	    return false;
	}

	public boolean passed(double distanceOffset) {
	    return distanceFromGun > gunLocation.distance(targetLocation) + distanceOffset;
	}

	int visitingIndex(Point2D target) {
	    return (int)Math.max(0, Math.min(FACTORS - 1, (((Utils.normalRelativeAngle(bearing(target) - startBearing)) / bearingDirection) + MIDDLE_FACTOR)));
	}

	double bearing(Point2D target) {
	    return absoluteBearing(gunLocation, target);
	}

	double distanceFromTarget(Point2D location, int timeOffset) {
	    return gunLocation.distance(location) - distanceFromGun - (double)timeOffset * bulletVelocity;
	}

	int mostVisited() {
	    int mostVisited = MIDDLE_FACTOR, i = FACTORS;
	    do  {
		if (visits[--i] > visits[mostVisited]) {
		    mostVisited = i;
		}
	    } while (i > 0);
	    return mostVisited;
	}

	double danger(Point2D destination) {
	    double danger = 0;
	    int i = FACTORS;
	    do {
		danger += hits[--i] / Math.sqrt((Math.abs(visitingIndex(destination) - i) + 1.0));
	    } while (i > 0);
	    return danger / Math.abs(distanceFromTarget(targetLocation, 0));
	}
    }
}
