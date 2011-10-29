package pez.etc;
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
// $Id: Tityus.java,v 1.17 2004/01/01 01:55:26 peter Exp $

public class GF1 extends AdvancedRobot {
    static final double MAX_VELOCITY = 8;
    static final double WALL_MARGIN = 25;
    static final int ACCEL_INDEXES = 3;
    static final int DISTANCE_INDEXES = 5;
    static final int AIM_FACTORS = 31;
    static final int WALL_INDEXES = 3;
    static Point2D robotLocation = new Point2D.Double();
    static Point2D lastRobotLocation = new Point2D.Double();
    static Point2D enemyLocation = new Point2D.Double();
    static double enemyDistance;
    static double enemyEnergy;
    static double enemyAbsoluteBearing;
    static double lastEnemyAbsoluteBearing;
    static double enemyFirePower = 3;
    static double deltaBearing;
    static double enemyBearingDirection = 1;
    static double direction = 0.15;
    static double bulletPower;

    public void run() {
        setColors(Color.yellow, Color.green, Color.black);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        bulletPower = Math.min(3.0, Math.random() * 9);

        do {
            turnRadarRightRadians(Double.POSITIVE_INFINITY); 
        } while (true);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        lastRobotLocation.setLocation(robotLocation);
        robotLocation.setLocation(getX(), getY());
	lastEnemyAbsoluteBearing = enemyAbsoluteBearing;
        enemyAbsoluteBearing = getHeadingRadians() + e.getBearingRadians();
        enemyDistance = e.getDistance();
        enemyLocation.setLocation(vectorToLocation(enemyAbsoluteBearing, enemyDistance, robotLocation));

        double enemyEnergyLost = enemyEnergy - e.getEnergy();
        enemyEnergy = e.getEnergy();
        if (enemyEnergyLost >= 0.1 && enemyEnergyLost <= 3.0) {
            enemyFirePower = enemyEnergyLost;
        }
        
	move();

        deltaBearing = Utils.normalRelativeAngle(absoluteBearing(lastRobotLocation, enemyLocation) - lastEnemyAbsoluteBearing);
	if (deltaBearing < 0) {
	    enemyBearingDirection = -1;
	}
	else if (deltaBearing > 0) {
	    enemyBearingDirection = 1;
	}

	if (getTime() > 16) {
	    setTurnGunRightRadians(Utils.normalRelativeAngle(
			enemyAbsoluteBearing + maxEscapeAngle(bulletPower) *
			enemyBearingDirection * 1.0 - getGunHeadingRadians()));
	    setFire(bulletPower);
	}

        setTurnRadarRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getRadarHeadingRadians()) * 2);
    }

    void move() {
	Point2D robotDestination = null;
	if (Math.random() < (0.0277 - (enemyDistance - 580) / 8500) / maxEscapeAngle(enemyFirePower)) {
	    changeDirection();
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
	    changeDirection();
	}
	goTo(robotDestination);
    }

    void changeDirection() {
	direction *= -1;
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
}
