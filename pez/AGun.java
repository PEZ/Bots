package pez;
import robocode.*;
import robocode.util.Utils;
import java.awt.geom.*;

// This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
// http://robowiki.net/?RWPCL
//
// Aristocles Gun forced into Marshmallow.
// $Id: AGun.java,v 1.1 2004/02/20 23:30:23 peter Exp $

public class AGun {
    static final double WALL_MARGIN = 18;
    static final double MAX_DISTANCE = 1000;
    static final double MAX_BULLET_POWER = 3.0;
    static final double BULLET_POWER = 2.4;

    static final int DISTANCE_INDEXES = 7;
    static final int VELOCITY_INDEXES = 3;
    static final int LAST_VELOCITY_INDEXES = 3;
    static final int WALL_INDEXES = 2;
    static final int AIM_FACTORS = 25;
    static final int MIDDLE_FACTOR = (AIM_FACTORS - 1) / 2;

    static Point2D enemyLocation;
    static double enemyVelocity;
    static double bearingDirection = 1;
    static int[][][][][] aimFactors = new int[DISTANCE_INDEXES][VELOCITY_INDEXES][LAST_VELOCITY_INDEXES][WALL_INDEXES][AIM_FACTORS];

    public void onScannedRobot(ScannedRobotEvent e, Marshmallow robot) {
	Wave wave = new Wave();
	wave.robot = robot;
	wave.wGunLocation = new Point2D.Double(robot.getX(), robot.getY());
        double enemyAbsoluteBearing = robot.getHeadingRadians() + e.getBearingRadians();
	double enemyDistance = e.getDistance();
        enemyLocation = project(wave.wGunLocation, enemyAbsoluteBearing, enemyDistance);

	Rectangle2D fieldRectangle = new Rectangle2D.Double(WALL_MARGIN, WALL_MARGIN,
	    robot.getBattleFieldWidth() - WALL_MARGIN * 2, robot.getBattleFieldHeight() - WALL_MARGIN * 2);

	int lastVelocityIndex = (int)Math.abs(enemyVelocity / 3);
	bearingDirection = 1;
	if ((enemyVelocity = e.getVelocity()) != 0) {
	    if (enemyVelocity * Math.sin(e.getHeadingRadians() - enemyAbsoluteBearing) < 0) {
		bearingDirection = -1;
	    }
	}
	int distanceIndex = (int)(enemyDistance / (MAX_DISTANCE / DISTANCE_INDEXES));
	wave.wBulletPower = Math.min(e.getEnergy() / 4, distanceIndex > 1 ? BULLET_POWER : MAX_BULLET_POWER);
	wave.wBearingDirection = bearingDirection * 0.8 / (double)MIDDLE_FACTOR;
	wave.wAimFactors = aimFactors[distanceIndex][(int)Math.abs(enemyVelocity / 3)][lastVelocityIndex]
	    [fieldRectangle.contains(project(wave.wGunLocation, enemyAbsoluteBearing + bearingDirection * 13, enemyDistance)) ? 0 : 1];
	wave.wBearing = enemyAbsoluteBearing;

	int mostVisited = AIM_FACTORS - 1;
	for (int i = 0; i < AIM_FACTORS; i++) {
	    if (wave.wAimFactors[i] > wave.wAimFactors[mostVisited]) {
		mostVisited = i;
	    }
	}
	robot.setTurnGunRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - robot.getGunHeadingRadians() +
	    wave.wBearingDirection * (mostVisited - MIDDLE_FACTOR)));

	if (robot.getEnergy() > 1) {
	    robot.setFire(wave.wBulletPower);
	}
	if (wave.wBulletPower >= BULLET_POWER) {
	    robot.addCustomEvent(wave);
	}
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
	Marshmallow robot;

	public boolean test() {
	    if ((wDistance += bulletVelocity(wBulletPower)) > wGunLocation.distance(enemyLocation)) {
		try {
		    wAimFactors[(int)Math.round(((Utils.normalRelativeAngle(absoluteBearing(wGunLocation, enemyLocation) - wBearing)) /
				wBearingDirection) + MIDDLE_FACTOR)]++;
		}
		catch (Exception e) {
		}
		robot.removeCustomEvent(this);
	    }
	    return false;
	}
    }
}
