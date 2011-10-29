package pez.micro;
import robocode.*;
import robocode.util.Utils;
import java.awt.geom.*;

// This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
// http://robowiki.net/?RWPCL
//
// Aristocles, by PEZ. What you see is always an imperfect copy of the form. 
// $Id: Aristocles.java,v 1.11 2004/02/22 20:10:06 peter Exp $

public class Aristocles extends AdvancedRobot {
    static final double BATTLE_FIELD_WIDTH = 800;
    static final double BATTLE_FIELD_HEIGHT = 600;

    static final double MAX_DISTANCE = 900;
    static final double MAX_BULLET_POWER = 3.0;
    static final double BULLET_POWER = 1.9;
    static final double WALL_MARGIN = 18;
    static final double MAX_TRIES = 125;
    static final double REVERSE_TUNER = 0.421075;
    static final double WALL_BOUNCE_TUNER = 0.699484;

    static final int DISTANCE_INDEXES = 5;
    static final int VELOCITY_INDEXES = 5;
    static final int LAST_VELOCITY_INDEXES = 5;
    static final int WALL_INDEXES = 2;
    static final int DECCEL_TIME_INDEXES = 6;
    static final int AIM_FACTORS = 25;
    static final int MIDDLE_FACTOR = (AIM_FACTORS - 1) / 2;

    static Point2D enemyLocation;
    static double enemyVelocity;
    static int timeSinceDeccel;
    static double bearingDirection;
    static int[][][][][][] aimFactors = new int[DISTANCE_INDEXES][VELOCITY_INDEXES][LAST_VELOCITY_INDEXES][DECCEL_TIME_INDEXES][WALL_INDEXES][AIM_FACTORS];
    static double direction = 0.4;
    static double enemyFirePower;
    static int GF1Hits;
    static double tries;

    public void run() {
        setAdjustRadarForGunTurn(true);
        setAdjustGunForRobotTurn(true);

	do {
	    turnRadarRightRadians(Double.POSITIVE_INFINITY); 
	} while (true);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
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
		enemyDistance * (1.2 - tries / 100.0))) && tries < MAX_TRIES) {
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
	int lastVelocityIndex = (int)Math.abs(enemyVelocity) / 2;
	int velocityIndex = (int)Math.abs((enemyVelocity = e.getVelocity()) / 2);
	if (velocityIndex < lastVelocityIndex) {
	    timeSinceDeccel = 0;
	}
	
	if (enemyVelocity != 0) {
	    bearingDirection = enemyVelocity * Math.sin(e.getHeadingRadians() - enemyAbsoluteBearing) > 0 ?
		0.7 / (double)MIDDLE_FACTOR : -0.7 / (double)MIDDLE_FACTOR;
	}
	wave.wBearingDirection = bearingDirection;
	
	int distanceIndex;
	wave.wBulletPower = Math.min(e.getEnergy() / 4,
	    (distanceIndex = (int)(enemyDistance / (MAX_DISTANCE / DISTANCE_INDEXES))) > 1 ? BULLET_POWER : MAX_BULLET_POWER);
	//wave.wBulletPower = MAX_BULLET_POWER; // TargetingChallenge

	wave.wAimFactors = aimFactors[distanceIndex][velocityIndex][lastVelocityIndex][Math.min(5, timeSinceDeccel++ / 13)]
	    [fieldRectangle.contains(project(wave.wGunLocation, enemyAbsoluteBearing + wave.wBearingDirection * 13, enemyDistance)) ? 1 : 0];

	wave.wBearing = enemyAbsoluteBearing;

	int mostVisited = MIDDLE_FACTOR, i = AIM_FACTORS;
	do  {
	    if (wave.wAimFactors[--i] > wave.wAimFactors[mostVisited]) {
		mostVisited = i;
	    }
	} while (i > 0);

	setTurnGunRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getGunHeadingRadians() +
	    wave.wBearingDirection * (mostVisited - MIDDLE_FACTOR)));

	setFire(wave.wBulletPower);
	if (getEnergy() >= BULLET_POWER) {
	    addCustomEvent(wave);
	}
	// </gun>

        setTurnRadarRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getRadarHeadingRadians()) * 2);
    }

    public void onHitByBullet(HitByBulletEvent e) {
	if (tries < 30) {
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
