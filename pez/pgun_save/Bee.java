package pez.rumble.pgun;
import pez.rumble.utils.*;
import robocode.*;
import robocode.util.Utils;
import java.util.*;
import java.awt.geom.*;

// Bee, a gun by PEZ. For CassiusClay - Sting like a bee!
// http://robowiki.net/?CassiusClay
//
// This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
// http://robowiki.net/?RWPCL
// (Basically it means you must keep the code public if you base any bot on it.)
//
// $Id: Bee.java,v 1.19 2004/09/22 21:16:25 peter Exp $

public class Bee {
    public static boolean isTC = false; // TargetingChallenge

    static final double WALL_MARGIN = 18;
    static final double MAX_DISTANCE = 900;
    static final double MAX_BULLET_POWER = 3.0;
    static final double BULLET_POWER = 1.86;

    static Point2D enemyLocation = new Point2D.Double();
    static double enemyDistance;
    static double lastVelocity;
    static int timeSinceVChange;
    static double lastEnemyBearingDirection = 0.73;

    static double roundNum;

    Rectangle2D fieldRectangle;
    AdvancedRobot robot;

    public Bee(AdvancedRobot robot) {
	this.robot = robot;
	GunWave.init();
	fieldRectangle = PUtils.fieldRectangle(robot, WALL_MARGIN);

	if (roundNum > 0) {
	    System.out.println("range hits given: " + (int)GunWave.rangeHits + " (average / round: " + java.text.NumberFormat.getNumberInstance().format(GunWave.rangeHits / roundNum) + ")");
	}
	roundNum++;
    }

    public void onScannedRobot(ScannedRobotEvent e) {
	GunWave wave = new GunWave(robot);
	wave.setStartTime(robot.getTime());
	if (robot.getOthers() > 0 && robot.getEnergy() > 0) {
	    GunWave.waves.add(wave);
	}

	enemyDistance = e.getDistance();
	wave.distanceIndexFaster = (int)Math.min(GunWave.DISTANCE_INDEXES_FASTER - 1, (enemyDistance / (MAX_DISTANCE / GunWave.DISTANCE_INDEXES_FASTER)));
	wave.distanceIndex = (int)Math.min(GunWave.DISTANCE_INDEXES - 1, (enemyDistance / (MAX_DISTANCE / GunWave.DISTANCE_INDEXES)));

	double wantedBulletPower = isTC ? MAX_BULLET_POWER : wave.distanceIndex > 0 ? BULLET_POWER : MAX_BULLET_POWER;
	double bulletPower = wantedBulletPower;
	if (!isTC) {
	    bulletPower = Math.min(Math.min(e.getEnergy() / 4, robot.getEnergy() / 7), wantedBulletPower);
	}
	wave.bulletPowerIndex = (int)(bulletPower / 0.65);
	Point2D robotLocation = new Point2D.Double(robot.getX(), robot.getY());
	wave.setGunLocation(robotLocation);
	double enemyBearing = robot.getHeadingRadians() + e.getBearingRadians();
	wave.setStartBearing(enemyBearing);
	wave.setTargetLocation(enemyLocation);
	enemyLocation.setLocation(PUtils.project(robotLocation, enemyBearing, enemyDistance));

	wave.velocityIndex = PUtils.getVelocityIndex(e.getVelocity());
	wave.lastVelocityIndex = PUtils.getVelocityIndex(lastVelocity);
	if (Math.abs(lastVelocity - e.getVelocity()) > 0.1) {
	    timeSinceVChange = 0;
	}
	lastVelocity = e.getVelocity();
	double approachVelocity = e.getVelocity() * -Math.cos(e.getHeadingRadians() - enemyBearing);
	wave.approachSignIndex = 1 + (Math.abs(approachVelocity) < 1.5 ? 0 : PUtils.sign(approachVelocity));

	double bulletVelocity = PUtils.bulletVelocity(bulletPower);
	double bulletFlightTime = enemyDistance / bulletVelocity;
	wave.setBulletVelocity(bulletVelocity);

	if (e.getVelocity() != 0) {
	    lastEnemyBearingDirection = wave.maxEscapeAngle() * PUtils.sign(e.getVelocity() * Math.sin(e.getHeadingRadians() - enemyBearing));
	}
	double orbitDirection = lastEnemyBearingDirection / (double)GunWave.MIDDLE_FACTOR;
	wave.setOrbitDirection(orbitDirection);

	wave.wallIndex = 0;
	do {
	} while (++wave.wallIndex < (GunWave.WALL_INDEXES) &&
	    fieldRectangle.contains(PUtils.project(robotLocation,
		enemyBearing + orbitDirection * (double)(wave.wallIndex * GunWave.WALL_INDEX_WIDTH), enemyDistance)));
	wave.wallIndex -= 1;

	wave.wallIndexFaster = 0;
	do {
	} while (++wave.wallIndexFaster < (GunWave.WALL_INDEXES_FASTER) &&
	    fieldRectangle.contains(PUtils.project(robotLocation,
		enemyBearing + orbitDirection * (double)(wave.wallIndexFaster * GunWave.WALL_INDEX_WIDTH_FASTER), enemyDistance)));
	wave.wallIndexFaster -= 1;

	wave.velocityChangedIndex = (int)PUtils.minMax(Math.pow((bulletVelocity * timeSinceVChange) / (enemyDistance / timeSinceVChange), 0.35), 0, GunWave.TIMER_INDEXES - 1);
	wave.velocityChangedIndexFaster = (int)PUtils.minMax(Math.pow((bulletVelocity * timeSinceVChange) / (enemyDistance / timeSinceVChange), 0.35), 0, GunWave.TIMER_INDEXES_FASTER - 1);

	timeSinceVChange += 2;

	GunWave.updateWaves();

	robot.setTurnGunRightRadians(Utils.normalRelativeAngle(enemyBearing - robot.getGunHeadingRadians() +
		    orbitDirection * (wave.mostVisited() - GunWave.MIDDLE_FACTOR)));

	if (isTC || robot.getEnergy() >= BULLET_POWER || e.getEnergy() < robot.getEnergy() / 3.0 || wave.distanceIndex < 1) {
	    if (robot.setFireBullet(bulletPower) != null) {
		wave.weight = 2.5;
	    }
	}
    }

    public void onBulletHit(BulletHitEvent e) {
	GunWave.hits++;
	if (enemyDistance > 150) {
	    GunWave.rangeHits++;
	}
    }
}

class GunWave extends Wave {
    static final int BULLET_POWER_INDEXES = 5;
    static final int DISTANCE_INDEXES_FASTER = 3;
    static final int DISTANCE_INDEXES = 5;
    static final int VELOCITY_INDEXES = 5;
    static final int WALL_INDEXES_FASTER = 3;
    static final int WALL_INDEXES = 4;
    static final double WALL_INDEX_WIDTH_FASTER = 7.5;
    static final double WALL_INDEX_WIDTH = 5.5;
    static final int TIMER_INDEXES_FASTER = 3;
    static final int TIMER_INDEXES = 5;
    static final int SIGN_INDEXES = 3;
    static final int FACTORS = 27;
    static final int MIDDLE_FACTOR = (FACTORS - 1) / 2;

    static double[][][][][][][] factorsFaster = new double[BULLET_POWER_INDEXES][DISTANCE_INDEXES_FASTER][VELOCITY_INDEXES][VELOCITY_INDEXES][TIMER_INDEXES_FASTER][WALL_INDEXES_FASTER][FACTORS];
    static double[][][][][][][] factorsMedium = new double[BULLET_POWER_INDEXES][DISTANCE_INDEXES][VELOCITY_INDEXES][VELOCITY_INDEXES][TIMER_INDEXES][WALL_INDEXES][FACTORS];
    static double[][][][][][][][] factorsSlower = new double[BULLET_POWER_INDEXES][DISTANCE_INDEXES][VELOCITY_INDEXES][VELOCITY_INDEXES][TIMER_INDEXES][WALL_INDEXES][SIGN_INDEXES][FACTORS];

    static List waves;
    static double hits;
    static double rangeHits;

    int bulletPowerIndex;
    int distanceIndexFaster;
    int distanceIndex;
    int velocityIndex;
    int lastVelocityIndex;
    int velocityChangedIndex;
    int velocityChangedIndexFaster;
    int wallIndex;
    int wallIndexFaster;
    int approachSignIndex;
    double weight = 1;

    static void init() {
	waves = new ArrayList();
    }

    public GunWave(AdvancedRobot robot) {
	init(robot, FACTORS);
    }

    static void updateWaves() {
	List reaper = new ArrayList();
	for (int i = 0, n = waves.size(); i < n; i++) {
	    GunWave wave = (GunWave)waves.get(i);
	    wave.setDistanceFromGun((robot.getTime() - wave.getStartTime()) * wave.getBulletVelocity());
	    if (wave.passed(18)) {
		if (wave.getRobot().getOthers() > 0) {
		    wave.registerVisits();
		}
		reaper.add(wave);
	    }
	}
	for (int i = 0, n = reaper.size(); i < n; i++) {
	    waves.remove(reaper.get(i));
	}
    }

    void registerVisits() {
	double visitsFaster[] = factorsFaster[bulletPowerIndex][distanceIndexFaster][velocityIndex][lastVelocityIndex][velocityChangedIndexFaster][wallIndexFaster];
	double visitsMedium[] = factorsMedium[bulletPowerIndex][distanceIndex][velocityIndex][lastVelocityIndex][velocityChangedIndex][wallIndex];
	double visitsSlower[] = factorsSlower[bulletPowerIndex][distanceIndex][velocityIndex][lastVelocityIndex][velocityChangedIndex][wallIndex][approachSignIndex];
	int index = Math.max(1, visitingIndex());
	registerVisits(visitsFaster, index);
	registerVisits(visitsMedium, index);
	registerVisits(visitsSlower, index);
    }

    void registerVisits(double[] buffer, int index) {
	buffer[0]++;
	buffer[index] += weight;
    }

    int mostVisited() {
	double visitsFaster[] = factorsFaster[bulletPowerIndex][distanceIndexFaster][velocityIndex][lastVelocityIndex][velocityChangedIndexFaster][wallIndexFaster];
	double visitsMedium[] = factorsMedium[bulletPowerIndex][distanceIndex][velocityIndex][lastVelocityIndex][velocityChangedIndex][wallIndex];
	double visitsSlower[] = factorsSlower[bulletPowerIndex][distanceIndex][velocityIndex][lastVelocityIndex][velocityChangedIndex][wallIndex][approachSignIndex];
	int mostVisitedIndex = MIDDLE_FACTOR;
	double most = 0;
	for (int i = 1; i < FACTORS; i++) {
	    double visits = visitsFaster[i] / Math.max(1, visitsFaster[0]) + visitsMedium[i] / Math.max(1, visitsMedium[0]) + (Bee.roundNum > 20 ? visitsSlower[i] / Math.max(1, visitsSlower[0]) : 0.0);
	    if (visits > most) {
		mostVisitedIndex = i;
		most = visits;
	    }
	}
	return mostVisitedIndex;
    }

    static double hitRate() {
	return rangeHits / (Bee.roundNum + 1);
    }
}
