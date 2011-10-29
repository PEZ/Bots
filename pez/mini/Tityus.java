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
// Tityus, by PEZ. Venomous, small and glows in black light.
// $Id: Tityus.java,v 1.33 2004/02/16 07:05:56 peter Exp $

public class Tityus extends AdvancedRobot {
    static final double MAX_VELOCITY = 8;
    static final double BATTLE_FIELD_WIDTH = 800;
    static final double BATTLE_FIELD_HEIGHT = 600;

    static final double MAX_DISTANCE = 1000;
    static final double MAX_BULLET_POWER = 3.0;
    static final double BULLET_POWER = 2.4;
    static final double WALL_MARGIN = 18;
    static final double MAX_TRIES = 125;
    static final double REVERSE_TUNER = 0.421075;
    static final double WALL_BOUNCE_TUNER = 0.699484;

    static final int DISTANCE_INDEXES = 7;
    static final int VELOCITY_INDEXES = 3;
    static final int LAST_VELOCITY_INDEXES = 3;
    static final int ACCEL_TIMER_INDEXES = 4;
    static final int WALL_INDEXES = 2;
    static final int AIM_FACTORS = 25;
    static final int MIDDLE_FACTOR = (AIM_FACTORS - 1) / 2;

    static Point2D enemyLocation;
    static String enemyName;
    static double enemyVelocity;
    static double lastEnemyVelocity;
    static double enemyDistance;
    static double bearingDirection = 1;
    static int[][][][][][] aimFactors = new int[DISTANCE_INDEXES][VELOCITY_INDEXES][VELOCITY_INDEXES][ACCEL_TIMER_INDEXES][WALL_INDEXES][AIM_FACTORS];
    static double direction = 0.4;
    static double enemyFirePower = 100;
    double enemyEnergy = 100;
    long accelTimer;
    static int GF1Hits;
    static double tries;
    static boolean hasSavedEnemyData = false;

    public void run() {
        setColors(Color.YELLOW, Color.YELLOW, Color.YELLOW);
        setAdjustRadarForGunTurn(true);
        setAdjustGunForRobotTurn(true);

	do {
	    turnRadarRightRadians(Double.POSITIVE_INFINITY); 
	} while (true);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        if (enemyName == null) {
            enemyName = e.getName();
            restoreFactors();
        }
	Wave wave = new Wave();
	wave.wGunLocation = new Point2D.Double(getX(), getY());
        double enemyAbsoluteBearing = getHeadingRadians() + e.getBearingRadians();
	double enemyDistance;
        enemyLocation = project(wave.wGunLocation, enemyAbsoluteBearing, enemyDistance = e.getDistance());

	if ((enemyVelocity = e.getVelocity()) != lastEnemyVelocity) {
	    accelTimer = getTime();
	    lastEnemyVelocity = enemyVelocity;
	}

        double enemyEnergyLost = enemyEnergy - e.getEnergy();
        enemyEnergy = e.getEnergy();
        if (enemyEnergyLost >= 0.1 && enemyEnergyLost <= 3.0) {
            enemyFirePower = enemyEnergyLost;
        }

	// <movement>
	Point2D robotDestination;
	Rectangle2D fieldRectangle = new Rectangle2D.Double(WALL_MARGIN, WALL_MARGIN,
	    BATTLE_FIELD_WIDTH - WALL_MARGIN * 2, BATTLE_FIELD_HEIGHT - WALL_MARGIN * 2);
	tries = 0;
	while (!fieldRectangle.contains(robotDestination = project(enemyLocation, enemyAbsoluteBearing + Math.PI + direction, enemyDistance * (1.2 - tries / 100.0))) && tries < MAX_TRIES) {
	    tries++;
	}
	if (GF1Hits > 2 && (Math.random() < (bulletVelocity(enemyFirePower) / REVERSE_TUNER) / enemyDistance ||
		tries > (enemyDistance / bulletVelocity(enemyFirePower) / WALL_BOUNCE_TUNER))) {
	    direction = -direction;
	}
	// Jamougha's cool way
	double angle;
	setAhead(Math.cos(angle = absoluteBearing(wave.wGunLocation, robotDestination) - getHeadingRadians()) * 100);
	setTurnRightRadians(Math.tan(angle));
	// </movement>

	// <gun>
	int lastVelocityIndex = (int)Math.abs(enemyVelocity / 3);
	bearingDirection = 1;
	if ((enemyVelocity = e.getVelocity()) != 0) {
	    if (enemyVelocity * Math.sin(e.getHeadingRadians() - enemyAbsoluteBearing) < 0) {
		bearingDirection = -1;
	    }
	}
	int distanceIndex = (int)(enemyDistance / (MAX_DISTANCE / DISTANCE_INDEXES));
        wave.wBulletPower = Math.min(getEnergy() / 5, bulletPower(distanceIndex));
	wave.wBearingDirection = bearingDirection * maxEscapeAngle(wave.wBulletPower) / (double)MIDDLE_FACTOR;
	wave.wAimFactors = aimFactors[distanceIndex][(int)Math.abs(enemyVelocity / 3)][lastVelocityIndex]
	    [accelTimerIndex(wave.wBulletPower)]
	    [fieldRectangle.contains(project(wave.wGunLocation, enemyAbsoluteBearing + bearingDirection * 13, enemyDistance)) ? 0 : 1];
	wave.wBearing = enemyAbsoluteBearing;

	int mostVisited = AIM_FACTORS - 1;
	for (int i = 0; i < AIM_FACTORS; i++) {
	    if (wave.wAimFactors[i] > wave.wAimFactors[mostVisited]) {
		mostVisited = i;
	    }
	}
	setTurnGunRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getGunHeadingRadians() +
	    wave.wBearingDirection * (mostVisited - MIDDLE_FACTOR)));

	Bullet bullet = setFireBullet(wave.wBulletPower);
	if (wave.wBulletPower == bulletPower(distanceIndex) && (!hasSavedEnemyData || bullet != null)) {
	    addCustomEvent(wave);
	}
	// </gun>

        setTurnRadarRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getRadarHeadingRadians()) * 2);
    }

    double bulletPower(int distanceIndex) {
	return distanceIndex > 1 ? 2.4 : 3.0;
    }

    static double maxEscapeAngle(double bulletPower) {
	return 1.1 * Math.asin(MAX_VELOCITY / bulletVelocity(bulletPower));
    }

    int accelTimerIndex(double bulletPower){
	double bulletTravelTime = enemyDistance / bulletVelocity(bulletPower);
	double t = (getTime() - accelTimer) / bulletTravelTime;
	if (t < .1) {
	    return 0;
	}
	if (t < .3) {
	    return 1;
	}
	if (t < 1) {
	    return 2;	
	}
	return 3;
    }

    public void onHitByBullet(HitByBulletEvent e) {
	if (tries < 20) {
	    GF1Hits++;
	}
	enemyFirePower = e.getPower();
    }

    static double bulletVelocity(double power) {
	return 20 - 3 * power;
    }

    static Point2D project(Point2D sourceLocation, double angle, double length) {
        return new Point2D.Double(sourceLocation.getX() + Math.sin(angle) * length,
            sourceLocation.getY() + Math.cos(angle) * length);
    }

    static double absoluteBearing(Point2D source, Point2D target) {
        return Math.atan2(target.getX() - source.getX(), target.getY() - source.getY());
    }

    void restoreFactors() {
	try {
	    ObjectInputStream ois = new ObjectInputStream(new GZIPInputStream(new FileInputStream(getDataFile(enemyName))));
            aimFactors = (int[][][][][][]) ois.readObject();
	    //hasSavedEnemyData = true;
	} catch (Exception e) {
	}
    }

    public void onWin(WinEvent e) {
        saveFactors();
    }

    public void onDeath(DeathEvent e) {
        saveFactors();
    }

    void saveFactors() {
	try {
	    ObjectOutputStream oos = new ObjectOutputStream(new GZIPOutputStream(new RobocodeFileOutputStream(getDataFile(enemyName))));
	    oos.writeObject(aimFactors);
	    oos.close();
	} catch (IOException e) {
	}
    }
    
    static int wallHits;
    public void onHitWall(HitWallEvent e) {
	out.println(++wallHits);
    }

    class Wave extends Condition {
	double wBulletPower;
	Point2D wGunLocation;
	double wBearing;
	double wBearingDirection;
	int[] wAimFactors;
	double wDistance;

	public boolean test() {
	    if ((wDistance += bulletVelocity(wBulletPower)) > wGunLocation.distance(enemyLocation)) {
		try {
		    wAimFactors[(int)Math.round(((Utils.normalRelativeAngle(absoluteBearing(wGunLocation, enemyLocation) - wBearing)) /
				wBearingDirection) + MIDDLE_FACTOR)]++;
		}
		catch (ArrayIndexOutOfBoundsException e) {
		}
		removeCustomEvent(this);
	    }
	    return false;
	}
    }
}
