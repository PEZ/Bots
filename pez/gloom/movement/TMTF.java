package pez.gloom.movement;
import pez.gloom.Bot;
import robocode.*;
import java.awt.geom.*;

// This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
// http://robowiki.dyndns.org/?RWPCL
//
// $Id: TMTF.java,v 1.4 2003/11/16 10:51:54 peter Exp $

public class TMTF implements Movement {
    private static final double DEFAULT_DISTANCE = 300;
    private static final double WALL_MARGIN = 25;
    private Bot robot;
    private Rectangle2D fieldRectangle;
    private RoundRectangle2D boundaryRectangle;
    private Flattener flattener;
    private double maxRobotVelocity;

    public TMTF(Bot robot) {
        this.robot = robot;
        fieldRectangle = new Rectangle2D.Double(0, 0 , robot.getBattleFieldWidth(), robot.getBattleFieldHeight());
        boundaryRectangle = new RoundRectangle2D.Double(WALL_MARGIN * 2, WALL_MARGIN * 2,
	    robot.getBattleFieldWidth() - WALL_MARGIN * 4, robot.getBattleFieldHeight() - WALL_MARGIN * 4, 75, 75);
        flattener = new TF(robot, this);
    }

    public void doMove() {
        flattener.doFlattening();
        robot.goTo(relativeDestination(0.3 * flattener.getDirection()));
        robot.setMaxVelocity(Math.abs(robot.getTurnRemaining()) < 40 ? flattener.getRobotVelocity() : 0.0);
    }

    public boolean canRam() {
        return true;
    }

    public boolean canEvade() {
        return true;
    }

    public double getDefaultDistance() {
	return DEFAULT_DISTANCE;
    }

    private Point2D relativeDestination(double relativeAngle) {
        Point2D destination = new Point2D.Double();
        double wantedEnemyDistance = robot.enemyDistance * distanceFactor();
        Bot.toLocation(robot.enemyAbsoluteBearing + Math.PI + relativeAngle, wantedEnemyDistance, robot.enemyLocation, destination);
        double wantedTravelDistance = robot.robotLocation.distance(destination);
        Bot.translateInsideField(fieldRectangle, destination, WALL_MARGIN);
        Bot.toLocation(Bot.absoluteBearing(robot.robotLocation, destination), wantedTravelDistance, robot.robotLocation, destination);
        Bot.translateInsideField(fieldRectangle, destination, WALL_MARGIN);

        return destination;
    }

    private double distanceFactor() {
	double fightingDistance = robot.getFightingDistance(DEFAULT_DISTANCE);
        double distanceFactor = 1.15;
        if (robot.shouldRam()) {
            distanceFactor = 0.50;
        }
        else if (robot.shouldEvade()) {
            distanceFactor = 1.35;
        }
        else if (!boundaryRectangle.contains(robot.robotLocation)) {
            distanceFactor = 0.98;
        }
        else if (robot.enemyDistance > fightingDistance) {
            distanceFactor = 1.0;
        }
        return distanceFactor;
    }
}

interface Flattener {
    void doFlattening();
    int getDirection();
    double getRobotVelocity();
}

class TF implements Flattener {
    private Bot robot;
    private Movement movement;
    private int direction = 1;
    private double nextTime = 0;
    private double robotVelocity = Bot.MAX_VELOCITY;

    TF (Bot robot, Movement movement) {
        this.robot = robot;
        this.movement = movement;
    }

    public void doFlattening() {
	double bulletTravelTime = robot.enemyDistance / Bot.bulletVelocity(robot.enemyFirePower);
	if (robot.getTime() > nextTime) {
	    if (Math.random() < reverseFactor(bulletTravelTime)) {
		direction *= -1;
	    }
	    nextTime = robot.getTime() + bulletTravelTime * Math.random();
	}
	setRobotVelocity();
    }

    public int getDirection() {
        return direction;
    }

    public double getRobotVelocity() {
        return robotVelocity;
    }

    private double reverseFactor(double bulletTravelTime) {
	/*
	double bulletsInTheAir = Math.max(1.0, bulletTravelTime / (robot.enemyFirePower / robot.getGunCoolingRate()));
        return 1.15 / bulletsInTheAir;
	*/
	return 0.92 - 7 / bulletTravelTime;
    }

    private void setRobotVelocity() {
	if (robotVelocity < 0.1 && Math.random() < 0.45) {
	    robotVelocity = Bot.MAX_VELOCITY;
	}
        else if (Math.random() < 0.06) {
            robotVelocity = 0.0;
        }
    }
}
