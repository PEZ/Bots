package pez.movement;

import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import pez.Marshmallow;
import pez.Enemy;
import pez.Rutils;


// $Id: RandomMovement.java,v 1.25 2004/02/20 23:30:23 peter Exp $
public class RandomMovement extends MovementStrategy implements pez.MarshmallowConstants {
    private static final double WALL_MARGIN = 18;
    private double accumulatedAngle;
    private double velocityChangeFactor = 0.07;
    private double velocityMaxFactor = 53;
    public RandomMovement() {
        super();
        m_name = "RandomMovement";
    }
    
    public MovementData getMovementData(Enemy enemy, Marshmallow robot) {
        MovementData movementData = new MovementData();

        if (robot.moveFinished()) {
            Point2D destination = new Point2D.Double();
            double enemyBulletVelocity = 20 - 3 * enemy.getEFirePower();
            double maxRelativeAngle = Math.abs(Math.toDegrees(Math.asin(8 / enemyBulletVelocity)));
            double deltaAngle = Rutils.pointsToAngle(oldEnemyLocation, robot.getLocation()) -
                Rutils.pointsToAngle(oldEnemyLocation, oldRobotLocation);
            accumulatedAngle += deltaAngle;
            double distanceFactor = distanceFactor(enemy.getDistance(), enemy.getEFirePower());
            double relativeAngle = distanceFactor * (maxRelativeAngle * 2 * Math.random() - maxRelativeAngle);
            double guessFactor = accumulatedAngle / maxRelativeAngle;
            if (Math.abs(guessFactor) > 1.0) {
                relativeAngle *= -1.7 * Rutils.sign(accumulatedAngle) * Math.abs(relativeAngle);
            }
            double distanceExtra = distanceExtra(enemy, robot);
            distanceExtra *= Math.abs(relativeAngle);
	    double wantedDistance = enemy.getDistance() + distanceExtra;
	    double tries = 0;
	    RoundRectangle2D fieldRectangle = new RoundRectangle2D.Double(WALL_MARGIN, WALL_MARGIN,
		    robot.getBattleFieldWidth() - WALL_MARGIN * 2, robot.getBattleFieldHeight() - WALL_MARGIN * 2, 75, 75);
	    do {
		Rutils.toLocation(enemy.getAbsoluteBearing() + 180 + relativeAngle,
		    wantedDistance * (1 - tries / 100), enemy.getLocation(), destination);
		tries++;
	    } while (tries < 40 && !fieldRectangle.contains(destination));
            if (tries >= 40) {
		tries = 0;
		do {
		    Rutils.toLocation(enemy.getAbsoluteBearing() + 180 - relativeAngle,
			wantedDistance * (1 - tries / 100), enemy.getLocation(), destination);
		    tries++;
		} while (tries < 100 && !fieldRectangle.contains(destination));
            }
            robot.translateInsideField(destination, MC_WALL_MARGIN);
            oldEnemyLocation.setLocation(enemy.getLocation());
            oldRobotLocation.setLocation(robot.getLocation());
            movementData.setDestination(destination); 
        }

        considerNewVelocity(enemy);

        return movementData;
    }

    private double distanceFactor(double distance, double bulletPower) {
	double bulletVelocity = 20 - 3 * bulletPower;
	double bulletTravelTime = distance / bulletVelocity;
	double factor = 0.555 + bulletTravelTime / 600;
	if (bulletPower > 1.9) {
	    factor -= 0.1 / bulletVelocity;
	}
	return factor;
    }

    double distanceExtra(Enemy enemy, Marshmallow robot) {
        double distanceExtra = 5;
        if (doRam(enemy, robot)) {
            distanceExtra = -12;
        }
        else if (enemy.getDistance() < 200) {
            distanceExtra = 12;
        }
        else if (enemy.getDistance() > MC_WANTED_DISTANCE) {
            distanceExtra = -1;
        }
        return distanceExtra;
    }

    private void considerNewVelocity(Enemy enemy) {
        if (enemy.getDistance() < 200) {
            velocity = MC_MAX_ROBOT_VELOCITY;
        }
        else if (Math.random() < velocityChangeFactor) {
            setNewVelocity();
        }
    }

    public void setNewVelocity() {
        velocity = Math.min(MC_MAX_ROBOT_VELOCITY, Math.random() * velocityMaxFactor);
    }
}
