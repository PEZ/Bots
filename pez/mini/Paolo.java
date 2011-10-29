package pez.mini;
import robocode.*;
import robocode.util.Utils;
import java.awt.geom.*;
import java.awt.Color;
import java.util.*;
import java.util.zip.*;
import java.io.*;

// This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
// http://robowiki.net/?RWPCL
//
// Paolo Roberto is a Swedish, middle weight, boxing champion.
// Paolo the robot tries to honour his model by being hard to hit while delivering hard to dodge power punches itself.
// For this it uses a random, fluid and adaptive movement and a powerful GuessFactorTargeting gun
//
// http://robowiki.net/?Paolo

// $Id: Paolo.java,v 1.1 2004/01/11 15:21:00 peter Exp $

public class Paolo extends AdvancedRobot {
    static final double MAX_VELOCITY = 8;
    static final double WALL_MARGIN = 25;
    static final int ACCEL_INDEXES = 3;
    static final int DISTANCE_INDEXES = 5;
    static final int AIM_FACTORS = 27;
    static final int WALL_INDEXES = 2;
    static Point2D robotLocation = new Point2D.Double();
    static Point2D lastRobotLocation = new Point2D.Double();
    static Point2D enemyLocation = new Point2D.Double();
    static double enemyDistance;
    static double enemyEnergy;
    static double enemyAbsoluteBearing;
    static double lastEnemyAbsoluteBearing;
    static double enemyFirePower = 3;
    static double deltaBearing;
    static double lastDeltaBearing;
    static double enemyBearingDirection = 1;
    static int distanceIndex;
    static int[][][][] aimFactors = new int[ACCEL_INDEXES][WALL_INDEXES][DISTANCE_INDEXES][AIM_FACTORS];
    static String enemyName = "";
    int[] currentAimFactors;
    static double direction = 0.15;
    double timeSinceReverse;
    static int fullLeadHits;
    static boolean isFlattening;
    static float reverseFactors[] = { 0.4F, 1.0F, 1.0F, 1.0F, 0.8F };

    public void run() {
	Color c = new Color(175, 125, 25);
        setColors(c, c, c);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        do {
            turnRadarRightRadians(Double.POSITIVE_INFINITY); 
        } while (true);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        if (enemyName == "") {
            enemyName = e.getName();
	    try {
		ZipInputStream zipin = new ZipInputStream(new
		    FileInputStream(getDataFile(enemyName)));
		zipin.getNextEntry();
		aimFactors = (int[][][][])(new ObjectInputStream(zipin)).readObject();
		zipin.getNextEntry();
		reverseFactors = (float[])(new ObjectInputStream(zipin)).readObject();
	    }
	    catch (Exception x) {
	    }
        }
        lastRobotLocation.setLocation(robotLocation);
        robotLocation.setLocation(getX(), getY());
	lastEnemyAbsoluteBearing = enemyAbsoluteBearing;
        enemyAbsoluteBearing = getHeadingRadians() + e.getBearingRadians();
        enemyDistance = e.getDistance();
        enemyLocation.setLocation(vectorToLocation(enemyAbsoluteBearing, enemyDistance, robotLocation));

	distanceIndex = Math.min((int)(enemyDistance / (800D / DISTANCE_INDEXES)), DISTANCE_INDEXES - 1);

        double enemyEnergyLost = enemyEnergy - e.getEnergy();
        enemyEnergy = e.getEnergy();
        if (enemyEnergyLost >= 0.1 && enemyEnergyLost <= 3.0) {
            enemyFirePower = enemyEnergyLost;
	    reverseFactors[distanceIndex] = rollingAvg(reverseFactors[distanceIndex], 1.9F, 100);
        }
        
	move();

        lastDeltaBearing = deltaBearing;
        deltaBearing = Utils.normalRelativeAngle(absoluteBearing(lastRobotLocation, enemyLocation) - lastEnemyAbsoluteBearing);
	if (deltaBearing < 0) {
	    enemyBearingDirection = -1;
	}
	else if (deltaBearing > 0) {
	    enemyBearingDirection = 1;
	}

        double bulletPower = Math.min(getEnergy() / 5, Math.min(enemyEnergy / 4, 3.0 - 1.3 * ((double)distanceIndex / (double)DISTANCE_INDEXES)));

	if (getTime() > 16) {
	    currentAimFactors = aimFactors[aimAccelIndex()][wallIndex()][distanceIndex];

	    setTurnGunRightRadians(Utils.normalRelativeAngle(
			enemyAbsoluteBearing + maxEscapeAngle(bulletPower) *
			enemyBearingDirection * (enemyEnergy > 0 ? mostVisitedFactor() : 0) - getGunHeadingRadians()));

	    if (getEnergy() > 0.3 || enemyEnergy == 0) {
		setFire(bulletPower);
		if (bulletPower >= 1.0) {
		    Wave wave = new Wave();
		    wave.wTime = getTime();
		    wave.wBulletPower = bulletPower;
		    wave.wGunLocation.setLocation(robotLocation);
		    wave.currentTargetLocation = enemyLocation;
		    wave.wAimFactors = currentAimFactors;
		    wave.wBearing = enemyAbsoluteBearing;
		    wave.wBearingDirection = enemyBearingDirection;
		    addCustomEvent(wave);
		}
	    }
	}

        setTurnRadarRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getRadarHeadingRadians()) * 2);
    }

    public void onWin(WinEvent e) {
        saveFactors();
    }

    public void onDeath(DeathEvent e) {
        saveFactors();
    }

    public void onCustomEvent(CustomEvent e) {
	Wave wave = (Wave)(e.getCondition());
	wave.wAimFactors[(int)Math.min(AIM_FACTORS - 1, Math.max(0, Math.round(((((
	    wave.wBearingDirection * Utils.normalRelativeAngle(absoluteBearing(wave.wGunLocation, wave.currentTargetLocation) - wave.wBearing)) /
	    maxEscapeAngle(wave.wBulletPower)) + 1) / 2) * AIM_FACTORS)))]++;
    }

    public void onHitByBullet(HitByBulletEvent e) {
	// Check if we are hit with full lead aim
	// Adapted from Axe's Musashi: http://robowiki.net/?Musashi
	if (timeSinceReverse > (enemyDistance + 50) / e.getVelocity()) {
	    isFlattening = (++fullLeadHits > 1);
	}
	else {
	    reverseFactors[distanceIndex] = rollingAvg(reverseFactors[distanceIndex], 0.1F, 9);
	}
    }

    void move() {
	timeSinceReverse++;
	Point2D robotDestination = null;
	if (isFlattening && Math.random() <
		reverseFactors[distanceIndex] * (0.0277 - (enemyDistance - 580) / 8500) / maxEscapeAngle(enemyFirePower)) {
	    reverse();
	}
	for (int i = 0; i < 2; i++) {
	    double tries = 0;
	    do {
		robotDestination = vectorToLocation(absoluteBearing(enemyLocation, robotLocation) + direction,
			enemyDistance * (1.1 - tries / 100.0), enemyLocation);
		tries++;
	    } while (tries < 25 + i * 75 && !fieldRectangle(WALL_MARGIN).contains(robotDestination));
	    if (fieldRectangle(WALL_MARGIN).contains(robotDestination)) {
		break;
	    }
	    reverse();
	}
	goTo(robotDestination);
    }

    void reverse() {
	direction *= -1;
	timeSinceReverse = 0;
    }

    RoundRectangle2D fieldRectangle(double margin) {
        return new RoundRectangle2D.Double(margin, margin,
	    getBattleFieldWidth() - margin * 2, getBattleFieldHeight() - margin * 2, 75, 75);
    }

    void goTo(Point2D destination) {
        double angle = Utils.normalRelativeAngle(absoluteBearing(robotLocation, destination) - getHeadingRadians());
	double turnAngle = Math.atan(Math.tan(angle));
        setTurnRightRadians(turnAngle);
        setAhead(robotLocation.distance(destination) * (angle == turnAngle ? 1 : -1));
	setMaxVelocity(Math.abs(getTurnRemaining()) > 30 ? 0 : MAX_VELOCITY);
    }

    double mostVisitedFactor() {
        int mostVisited = (AIM_FACTORS - 1) / 2;
        for (int i = 0; i < AIM_FACTORS; i++) {
            if (currentAimFactors[i] > currentAimFactors[mostVisited]) {
                mostVisited = i;
            }
        }
	return ((mostVisited + 0.5) / AIM_FACTORS) * 2 - 1;
    }

    int aimAccelIndex() {
        int delta = (int)(20 * (Math.abs(deltaBearing) - Math.abs(lastDeltaBearing)) / (MAX_VELOCITY / enemyDistance));
        if (delta < 0) {
            return 0;
        }
        else if (delta > 0) {
            return 2;
        }
        return 1;
    }

    int wallIndex() {
	return ((fieldRectangle(0)).contains(
	    vectorToLocation(enemyAbsoluteBearing + deltaBearing * 9, enemyDistance, robotLocation)) ? 0 : 1);
    }

    static double maxEscapeAngle(double bulletPower) {
	return Math.asin(MAX_VELOCITY / bulletVelocity(bulletPower));
    }

    static double bulletVelocity(double power) {
        return 20 - 3 * power;
    }

    static Point2D vectorToLocation(double angle, double length, Point2D sourceLocation) {
        return new Point2D.Double(sourceLocation.getX() + Math.sin(angle) * length,
            sourceLocation.getY() + Math.cos(angle) * length);
    }

    static double absoluteBearing(Point2D source, Point2D target) {
        return Math.atan2(target.getX() - source.getX(), target.getY() - source.getY());
    }

    // Paul Evan's classic
    private static float rollingAvg(float value, float newEntry, int n) {
        return (value * n + newEntry) / (float)(n + 1);
    } 

    void saveFactors() {
        try {
            ZipOutputStream zipout = new ZipOutputStream(new RobocodeFileOutputStream(getDataFile(enemyName)));
            zipout.putNextEntry(new ZipEntry("aimFactors"));
            ObjectOutputStream out = new ObjectOutputStream(zipout);
            out.writeObject(aimFactors);
            zipout.putNextEntry(new ZipEntry("reverseFactors"));
            out = new ObjectOutputStream(zipout);
            out.writeObject(reverseFactors);
            out.flush();
            zipout.closeEntry();
            out.close();
        }
        catch (IOException e) {
        }
    }

    class Wave extends Condition {
	long wTime;
	double wBulletPower;
	Point2D wGunLocation = new Point2D.Double();
	Point2D currentTargetLocation;
	double wBearing;
	double wBearingDirection;
	int[] wAimFactors;

	public boolean test() {
	    if (bulletVelocity(wBulletPower) * (getTime() - wTime) > wGunLocation.distance(currentTargetLocation)) {
		removeCustomEvent(this);
		return true;
	    }
	    return false;
	}
    }
}
