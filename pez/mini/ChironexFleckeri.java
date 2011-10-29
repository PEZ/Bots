// ChironexFleckeri, by PEZ. Small deadly jelly fish. http://robowiki.net/?ChironexFleckeri
//
// This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
// http://robowiki.net/?RWPCL - Read the before you read this code.
//
// ChironexFleckeri explores two major concepts:
//    1. Guess factor targeting, invented by Paul Evans. http://robowiki.net/?GuessFacorTargeting
//    2. Wave surfing movement, invented by ABC. http://robowiki.net/?WaveSurfing
// 
// $Id:  Exp $

package pez.mini;
import robocode.*;
import robocode.util.Utils;
import java.util.*;
import java.awt.geom.*;

public class ChironexFleckeri extends AdvancedRobot {
    static final double MAX_VELOCITY = 8;

    static final double MAX_WALL_SMOOTH_TRIES = 125;
    static final double WALL_MARGIN = 20;
    static final double BULLET_POWER = 2.0;
    static final int FACTORS = 31;
    static final int MIDDLE_FACTOR = (FACTORS - 1) / 2;
    static final int VELOCITY_INDEXES = 9;
    static final int OTHERS_INDEXES = 2;


    static Point2D robotLocation = new Point2D.Double();
    static HashMap enemies = new HashMap();
    static int scannedEnemies;
    static Enemy currentEnemy;
    static Point2D reverseDestination;
    static Point2D forwardDestination;
    static double dangerForward;
    static double dangerReverse;
    static double lastVelocity;
    
    public void run() {
	setAdjustRadarForGunTurn(true);
	setAdjustGunForRobotTurn(true);

	do {
	    turnRadarRightRadians(Double.POSITIVE_INFINITY); 
	} while (true);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
	scannedEnemies++;
	Enemy enemy = new Enemy();
	if (enemies.containsKey(e.getName())) {
	    enemy = (Enemy)enemies.get(e.getName());
	}
	else {
	    enemies.put(e.getName(), enemy);
	}
	enemy.distance = e.getDistance();

	Wave wave = new Wave(enemy);
	Wave ew = new Wave(enemy);
	enemy.lastMovementWave = ew;
	enemy.lastGunWave = wave;

	ew.isMovementWave = true;
	ew.gunLocation = new Point2D.Double(enemy.location.getX(), enemy.location.getY());
	ew.startBearing = ew.bearing(robotLocation);

	double enemyDeltaEnergy = enemy.energy - e.getEnergy();
	if (enemyDeltaEnergy > 0 && enemyDeltaEnergy <= 3.0) {
	    enemy.firePower = enemyDeltaEnergy;
	    ew.surfable = true;
	}
	enemy.energy = e.getEnergy();
	ew.bulletVelocity = 20 - 3 * enemy.firePower;

	ew.bearingDirection = Math.asin(MAX_VELOCITY / ew.bulletVelocity) * robotBearingDirection(ew.startBearing) / (double)MIDDLE_FACTOR;

	ew.targetLocation = robotLocation;
	ew.velocityIndex = (int)(lastVelocity / 3);
	lastVelocity = Math.abs(getVelocity());

	robotLocation.setLocation(new Point2D.Double(getX(), getY()));

	ew.distanceFromGun = 2 * ew.bulletVelocity;
	addCustomEvent(ew);

	double enemyAbsoluteBearing = wave.startBearing = getHeadingRadians() + e.getBearingRadians();
	enemy.location.setLocation(project(wave.gunLocation = new Point2D.Double(getX(), getY()), enemyAbsoluteBearing, e.getDistance()));
	wave.targetLocation = enemy.location;
	wave.bulletVelocity = 20 - 3 * BULLET_POWER;

	if (e.getVelocity() != 0) {
	    enemy.bearingDirection = 0.7 * sign(e.getVelocity() * Math.sin(e.getHeadingRadians() - enemyAbsoluteBearing));
	}
	wave.bearingDirection = enemy.bearingDirection / (double)MIDDLE_FACTOR;

	wave.lastVelocityIndex = (int)(enemy.velocity);
	wave.velocityIndex = (int)(enemy.velocity = Math.abs(e.getVelocity()));

	addCustomEvent(wave);

	if (getOthers() > 1 && reverseDestination != null) {
	    dangerForward += 100.0 / forwardDestination.distance(enemy.location);
	    dangerReverse += 100.0 / reverseDestination.distance(enemy.location);
	}
	else {
	    setTurnRadarRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getRadarHeadingRadians()) * 2);
	}
	if (++scannedEnemies >= getOthers()) {
	    doGunAndMovement((currentEnemy = (Enemy)Collections.min(enemies.values())).lastGunWave, currentEnemy.lastMovementWave);
	    scannedEnemies = 0;
	}
    }

    void doGunAndMovement(Wave gunWave, Wave moveWave) {
	setTurnGunRightRadians(Utils.normalRelativeAngle(gunWave.startBearing -
		    getGunHeadingRadians() + gunWave.lastVisitedBearing()));
	if (getEnergy() > BULLET_POWER) {
	    setFire(BULLET_POWER);
	}

	double direction = robotBearingDirection(moveWave.startBearing);
	if (dangerReverse < dangerForward) {
	    direction = -direction;
	}
	dangerForward = dangerReverse = 0;
	double angle;
	setAhead(Math.cos(angle = gunWave.bearing(wallSmoothedDestination(moveWave, robotLocation, direction)) - getHeadingRadians()) * 100);
	setTurnRightRadians(Math.tan(angle));
    }

    public void onHitByBullet(HitByBulletEvent e) {
	try {
	    Wave wave = ((Enemy)enemies.get(e.getName())).passingWave;
	    wave.enemy.hits[wave.othersIndex][wave.velocityIndex] = wave.visitingIndex(wave.targetLocation);
	}
	catch (Exception x) {};
    }

    public void onRobotDeath(RobotDeathEvent e) {
	((Enemy)enemies.get(e.getName())).distance = Double.POSITIVE_INFINITY;
    }

    Point2D wallSmoothedDestination(Wave wave, Point2D location, double direction) {
	Point2D destination;
	double smoothing = 0;
	Rectangle2D bf = new Rectangle2D.Double(WALL_MARGIN, WALL_MARGIN,
			getBattleFieldWidth() - WALL_MARGIN * 2, getBattleFieldHeight() - WALL_MARGIN * 2);
	while (!bf.contains(destination = project(location, absoluteBearing(location,
	    wave.enemy.location) - direction * ((1.25 - smoothing / 100) * Math.PI / 2), 170)) && (smoothing += 5) < MAX_WALL_SMOOTH_TRIES) {
	}
	return destination;
    }

    Point2D waveImpactLocation(Wave wave, double direction, int time) {
	Point2D impactLocation = new Point2D.Double(getX(), getY());
	do {
	    impactLocation = project(impactLocation, absoluteBearing(impactLocation,
		wallSmoothedDestination(wave, impactLocation, direction * robotBearingDirection(wave.bearing(robotLocation)))), MAX_VELOCITY);
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

    class Wave extends Condition {
	Enemy enemy;
	Point2D gunLocation;
	Point2D targetLocation;
	double startBearing;
	double bulletVelocity;
	double bearingDirection;
	double distanceFromGun;
	boolean isMovementWave;
	boolean surfable;
	int othersIndex = getOthers() < 2 ? 0 : 1;
	int velocityIndex;
	int lastVelocityIndex;

	Wave(Enemy e) {
	    enemy = e;
	}

	public boolean test() {
	    distanceFromGun += bulletVelocity;
	    if (passed(-18)) {
		enemy.visits[othersIndex][velocityIndex][lastVelocityIndex] = visitingIndex(targetLocation);
		surfable = false;
		if (isMovementWave) {
		    enemy.passingWave = this;
		}
	    }
	    if (passed(18)) {
		removeCustomEvent(this);
	    }
	    if (surfable) {
		if (enemy == currentEnemy) {
		    reverseDestination = waveImpactLocation(this, -1, 5);
		    forwardDestination = waveImpactLocation(this, 1, 0);
		}
		try {
		    dangerReverse += danger(reverseDestination);
		    dangerForward += danger(forwardDestination);
		}
		catch (Exception e) {};
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

	double lastVisitedBearing() {
	    return bearingDirection * (enemy.visits[othersIndex][velocityIndex][lastVelocityIndex] - MIDDLE_FACTOR);
	}

	double danger(Point2D destination) {
	    return ((double)FACTORS / (Math.abs(visitingIndex(destination) - enemy.hits[othersIndex][velocityIndex]) + 1.0)) / distanceFromTarget(targetLocation, 0);
	}
    }

    class Enemy implements Comparable {
	Point2D location = new Point2D.Double();
	double[][] hits = new double[OTHERS_INDEXES][VELOCITY_INDEXES];
	double[][][] visits = new double[OTHERS_INDEXES][VELOCITY_INDEXES][VELOCITY_INDEXES];
	Wave lastMovementWave;
	Wave lastGunWave;
	Wave passingWave;
	double distance;
	double bearingDirection;
	double energy;
	double firePower;
	double velocity;

	public int compareTo(Object o) {
	    return (int)(distance - ((Enemy)o).distance);
	}
    }
}
