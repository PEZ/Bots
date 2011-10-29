package pez.gloom.movement;

import java.awt.geom.*;
import pez.gloom.Bot;


// This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
// http://robowiki.dyndns.org/?RWPCL
//
// $Id: MMMF.java,v 1.5 2003/12/06 23:27:42 peter Exp $
public class MMMF implements Movement {
    private static final double WALL_MARGIN = 38;
    private static final double DEFAULT_DISTANCE = 375;
    private Bot robot;
    private double accumulatedAngle;
    private double velocity = Bot.MAX_VELOCITY;
    private double velocityChangeFactor = 0.12;
    private double velocityMaxFactor = 64;
    private Point2D destination;
    private Point2D lastRobotLocation = new Point2D.Double();
    private Point2D lastEnemyLocation = new Point2D.Double();
    private RoundRectangle2D boundaryRectangle;
    
    public MMMF(Bot robot) {
	this.robot = robot;
        boundaryRectangle = new RoundRectangle2D.Double(WALL_MARGIN * 2, WALL_MARGIN * 2,
	    robot.getBattleFieldWidth() - WALL_MARGIN * 4, robot.getBattleFieldHeight() - WALL_MARGIN * 4, 75, 75);
	saveLocations();
    }

    public void doMove() {
        if (destination == null || Math.abs(robot.getDistanceRemaining()) < 20) {
	    destination = new Point2D.Double();
            double maxRelativeAngle = Bot.maxBearing(Bot.bulletVelocity(robot.enemyFirePower));
            double deltaAngle = Bot.absoluteBearing(lastEnemyLocation, robot.robotLocation) -
                Bot.absoluteBearing(lastEnemyLocation, lastRobotLocation);
            if (Math.abs(deltaAngle) > maxRelativeAngle) {
                deltaAngle = Bot.sign(deltaAngle) * Math.abs(maxRelativeAngle) / 2;
            }
            accumulatedAngle += deltaAngle;
            double distanceFactor = distanceFactor(robot.enemyDistance, robot.enemyFirePower);
            double relativeAngle = distanceFactor * (maxRelativeAngle * 2 * Math.random() - maxRelativeAngle);
            double guessFactor = accumulatedAngle / maxRelativeAngle;
            if (Math.abs(guessFactor) > 1.00) {
                relativeAngle *= -1.7 * Bot.sign(accumulatedAngle) * Math.abs(relativeAngle);
		accumulatedAngle = 0;
            }
            double distanceExtra = distanceExtra() * Math.abs(Math.toDegrees(relativeAngle));
	    double robotBearing = Bot.normalRelativeAngle(robot.enemyAbsoluteBearing + Math.PI);
	    double wantedDistance = robot.enemyDistance + distanceExtra;
            Bot.toLocation(robotBearing + relativeAngle,
                wantedDistance, robot.enemyLocation, destination);
            if (!robot.fluffedFieldRectangle.contains(destination)) {
                Bot.toLocation(robotBearing - relativeAngle,
                    wantedDistance, robot.enemyLocation, destination);
            }
            robot.translateInsideField(robot.fieldRectangle, destination, WALL_MARGIN);
            while (!robot.shouldRam() && robot.enemyLocation.distance(destination) <
                    robot.enemyLocation.distance(robot.robotLocation) - robot.enemyLocation.distance(robot.robotLocation) / 5) {
                Bot.toLocation(Bot.absoluteBearing(robot.robotLocation, destination),
                    robot.robotLocation.distance(destination) / 2, robot.robotLocation, destination);
            }
	    saveLocations();
        }
        considerNewVelocity();
	robot.goTo(destination); 
    }

    public double getDefaultDistance() {
	return DEFAULT_DISTANCE;
    }

    private void saveLocations() {
	lastRobotLocation.setLocation(robot.robotLocation);
	lastEnemyLocation.setLocation(robot.enemyLocation);
    }

    private double distanceFactor(double distance, double bulletPower) {
        double bulletTravelTime = distance / Bot.bulletVelocity(bulletPower);

        if (bulletPower > 2.5) {
            return Math.max(1, 0.33 + bulletTravelTime / 87);
        }
        else {
            return 0.738 + bulletTravelTime / 685;
        }
    }

    private double distanceExtra() {
        double extra = 3;
        if (robot.shouldRam()) {
            extra = -7;
        }
        else if (!boundaryRectangle.contains(robot.robotLocation)) {
            extra = -1;
        }
	else if (robot.enemyDistance < 200) {
            extra = 12;
        }
	else if (robot.enemyDistance > robot.getFightingDistance(DEFAULT_DISTANCE)) {
            extra = -2;
        }
        return extra;
    }

    private void considerNewVelocity() {
	if (robot.enemyDistance <= 200) {
	    velocity = Bot.MAX_VELOCITY;
	}
        else if (Math.random() < velocityChangeFactor) {
	    velocity = (Math.min(Bot.MAX_VELOCITY, Math.random() * velocityMaxFactor));
        }
        robot.setMaxVelocity(Math.abs(robot.getTurnRemaining()) < 40 ? velocity : 0.1);
    }
}
