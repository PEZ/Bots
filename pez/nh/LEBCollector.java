package pez.nh;
import robocode.*;
import robocode.util.Utils;
import java.awt.geom.*;
import java.util.zip.*;
import java.io.*;

// LEBCollector, by PEZ.
// $Id: LEBCollector.java,v 1.2 2004/02/27 15:21:43 peter Exp $

public class LEBCollector extends AdvancedRobot {
    static final double BATTLE_FIELD_WIDTH = 800;
    static final double BATTLE_FIELD_HEIGHT = 600;

    static final double MAX_DISTANCE = 900;
    static final double MAX_BULLET_POWER = 3.0;
    static final double WALL_MARGIN = 18;
    static final double MAX_TRIES = 125;
    static final double REVERSE_TUNER = 0.421075;
    static final double WALL_BOUNCE_TUNER = 0.699484;

    static final int DISTANCE_INDEXES = 5;
    static final int VELOCITY_INDEXES = 9;
    static final int LAST_VELOCITY_INDEXES = 9;
    static final int WALL_INDEXES = 2;
    static final int DECCEL_TIME_INDEXES = 6;
    static final int AIM_FACTORS = 31;
    static final int MIDDLE_FACTOR = (AIM_FACTORS - 1) / 2;

    static Point2D enemyLocation;
    static double enemyVelocity;
    static double bearingDirection;
    static int[][][][] aimFactorVisits = new int[DISTANCE_INDEXES][VELOCITY_INDEXES][LAST_VELOCITY_INDEXES][AIM_FACTORS];
    static double[][][] aimFactors = new double[DISTANCE_INDEXES][VELOCITY_INDEXES][LAST_VELOCITY_INDEXES];
    static double direction = 0.4;
    static double enemyFirePower;
    static double tries;

    static String enemyName;

    public void run() {
        setAdjustRadarForGunTurn(true);
        setAdjustGunForRobotTurn(true);

	do {
	    turnRadarRightRadians(Double.POSITIVE_INFINITY); 
	} while (true);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
	enemyName = e.getName();
	Wave wave = new Wave();
        double enemyAbsoluteBearing = getHeadingRadians() + e.getBearingRadians();
	double enemyDistance;
        enemyLocation = project(wave.wGunLocation = new Point2D.Double(getX(), getY()), enemyAbsoluteBearing, enemyDistance = e.getDistance());

	// <movement>
	Point2D robotDestination;
	Rectangle2D fieldRectangle = new Rectangle2D.Double(WALL_MARGIN, WALL_MARGIN,
	    BATTLE_FIELD_WIDTH - WALL_MARGIN * 2, BATTLE_FIELD_HEIGHT - WALL_MARGIN * 2);
	tries = 0;
	while (!fieldRectangle.contains(robotDestination = project(enemyLocation, enemyAbsoluteBearing + Math.PI + direction,
		enemyDistance * (0.95 - tries / 100.0))) && tries < MAX_TRIES) {
	    tries++;
	}
	if ((Math.random() < (bulletVelocity(enemyFirePower) / REVERSE_TUNER) / enemyDistance ||
		tries > (enemyDistance / bulletVelocity(enemyFirePower) / WALL_BOUNCE_TUNER))) {
	    direction = -direction;
	}
	// Jamougha's cool way
	double angle;
	setAhead(Math.cos(angle = absoluteBearing(wave.wGunLocation, robotDestination) - getHeadingRadians()) * 100);
	setTurnRightRadians(Math.tan(angle));
	// </movement>

	// <gun>
	int distanceIndex = (int)(e.getDistance() / (MAX_DISTANCE / DISTANCE_INDEXES));
	int lastVelocityIndex = (int)Math.abs(enemyVelocity);
	int velocityIndex = (int)Math.abs((enemyVelocity = e.getVelocity()));

	wave.wBearingDirection = enemyVelocity * Math.sin(e.getHeadingRadians() - enemyAbsoluteBearing) > 0 ? 0.7 / (double)MIDDLE_FACTOR : -0.7 / (double)MIDDLE_FACTOR;
	
	wave.wBulletPower = MAX_BULLET_POWER;

	wave.wAimFactors = aimFactorVisits[distanceIndex][velocityIndex][lastVelocityIndex];

	wave.wBearing = enemyAbsoluteBearing;

	int mostVisited = MIDDLE_FACTOR, i = AIM_FACTORS;
	do  {
	    if (wave.wAimFactors[--i] > wave.wAimFactors[mostVisited]) {
		mostVisited = i;
	    }
	} while (i > 0);


	aimFactors[distanceIndex][lastVelocityIndex][velocityIndex] = Math.abs(wave.wBearingDirection) * (mostVisited - MIDDLE_FACTOR);
	
	setTurnGunRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getGunHeadingRadians() +
	    wave.wBearingDirection * aimFactors[distanceIndex][lastVelocityIndex][velocityIndex]));

	setFire(wave.wBulletPower);
	if (getEnergy() >= MAX_BULLET_POWER) {
	    addCustomEvent(wave);
	}
	// </gun>

        setTurnRadarRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getRadarHeadingRadians()) * 2);
    }

    public void onHitByBullet(HitByBulletEvent e) {
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

    public void onDeath(DeathEvent e) {
	saveFactors();
    }

    public void onWin(WinEvent e) {
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

    class Wave extends Condition {
	double wBulletPower;
	Point2D wGunLocation;
	double wBearing;
	double wBearingDirection;
	int[] wAimFactors;
	double wDistance;

	public boolean test() {
	    if ((wDistance += bulletVelocity(wBulletPower)) > wGunLocation.distance(enemyLocation) - 18) {
		try {
		    wAimFactors[(int)Math.round(((Utils.normalRelativeAngle(absoluteBearing(wGunLocation, enemyLocation) - wBearing)) /
				wBearingDirection) + MIDDLE_FACTOR)]++;
		}
		catch (Exception e) {
		}
		removeCustomEvent(this);
	    }
	    return false;
	}
    }
}
