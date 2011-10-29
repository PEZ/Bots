package pez.movement;

import robocode.*;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import pez.Marshmallow;
import pez.Enemy;
import pez.Rutils;


// $Id: AMovement.java,v 1.1 2004/02/20 22:29:12 peter Exp $
public class AMovement {
    static double WALL_MARGIN = 18;
    static final double MAX_TRIES = 125;
    static final double REVERSE_TUNER = 0.421075;
    static final double WALL_BOUNCE_TUNER = 0.699484;

    static double direction = 0.4;
    static double enemyFirePower = 100;
    static int GF1Hits;
    static double tries;
    public static boolean isFlattening;
    public static boolean isDeactivated;

    public static void onScannedRobot(ScannedRobotEvent e, Marshmallow robot) {
	if (!isDeactivated) {
	    double enemyAbsoluteBearing = robot.getHeadingRadians() + e.getBearingRadians();
	    double enemyDistance = e.getDistance();
	    Point2D robotLocation = new Point2D.Double(robot.getX(), robot.getY());
	    Point2D enemyLocation = project(robotLocation, enemyAbsoluteBearing, enemyDistance);

	    Point2D robotDestination;
	    RoundRectangle2D fieldRectangle = new RoundRectangle2D.Double(WALL_MARGIN, WALL_MARGIN,
		    robot.getBattleFieldWidth() - WALL_MARGIN * 2, robot.getBattleFieldHeight() - WALL_MARGIN * 2, 75, 75);
	    tries = 0;
	    while (!fieldRectangle.contains(robotDestination = project(enemyLocation, enemyAbsoluteBearing + Math.PI + direction, enemyDistance * (1.2 - tries / 100.0))) && tries < MAX_TRIES) {
		tries++;
	    }
	    if (isFlattening && (Math.random() < (Rutils.bulletVelocity(enemyFirePower) / REVERSE_TUNER) / enemyDistance ||
			tries > (enemyDistance / Rutils.bulletVelocity(enemyFirePower) / WALL_BOUNCE_TUNER))) {
		direction = -direction;
	    }
	    // Jamougha's cool way
	    double angle;
	    robot.setAhead(Math.cos(angle = absoluteBearing(robotLocation, robotDestination) - robot.getHeadingRadians()) * 100);
	    robot.setTurnRightRadians(Math.tan(angle));
	}
    }

    static Point2D project(Point2D sourceLocation, double angle, double length) {
        return new Point2D.Double(sourceLocation.getX() + Math.sin(angle) * length,
            sourceLocation.getY() + Math.cos(angle) * length);
    }

    static double absoluteBearing(Point2D source, Point2D target) {
        return Math.atan2(target.getX() - source.getX(), target.getY() - source.getY());
    }

    public static void onHitByBullet(HitByBulletEvent e) {
	if (tries < 20) {
	    GF1Hits++;
	    isFlattening = GF1Hits > 2;
	}
	enemyFirePower = e.getPower();
    }
}
