package pez.gloom.intel;
import pez.gloom.Bot;
import robocode.util.Utils;
import robocode.*;
import java.awt.geom.*;

// $Id: Wave.java,v 1.4 2003/12/06 23:27:41 peter Exp $
public abstract class Wave extends Condition {
    Bot robot;
    double time;
    double bulletPower;
    double bulletVelocity;
    double deltaBearing;
    double maxBearing;
    double targetDirection;
    Point2D gunLocation = new Point2D.Double();
    Point2D targetLocation = new Point2D.Double();
    Point2D currentTargetLocation;
    int[] factorVisits;
    double triggerDistanceDelta;
    int visitedIndex = -1;

    public Wave(Bot robot, double bulletPower, Point2D gunLocation, Point2D targetLocation,
                double targetDeltaBearing, double targetMaxBearing, double targetDirection, int[] currentFactorVisits) {
        this.robot = robot;
        this.time = robot.getTime() + this.getTimeOffset();
        this.bulletPower = bulletPower;
        this.bulletVelocity = Bot.bulletVelocity(bulletPower);
        this.deltaBearing = targetDeltaBearing;
        this.gunLocation.setLocation(gunLocation);
        this.currentTargetLocation = targetLocation;
        this.targetLocation.setLocation(targetLocation);
        this.maxBearing = targetMaxBearing;
	this.targetDirection = targetDirection;
        this.factorVisits = currentFactorVisits;
    }

    abstract long getTimeOffset();

    public boolean test() {
	boolean result = false;
        if (triggerTest(triggerDistanceDelta)) {
            robot.removeCustomEvent(this);
            result = true;
        }
        if (triggerTest(15)) {
            robot.removeCustomEvent(this);
	}
        return result;
    }

    public void updateStats() {
	int index = visitedIndex(Utils.normalRelativeAngle(Bot.absoluteBearing(gunLocation, currentTargetLocation) -
            Bot.absoluteBearing(gunLocation, targetLocation)));
        if (index != visitedIndex) {
	    visitedIndex = index;
	    robot.registerFactorVisit(factorVisits, visitedIndex);
	}
    }

    double distance() {
        return bulletVelocity * (double)(robot.getTime() - time);
    }

    boolean triggerTest(double distanceDelta) {
        return distance() > gunLocation.distance(currentTargetLocation) + distanceDelta;
    }

    int visitedIndex(double bearingDiff) {
	double factor = targetDirection * bearingDiff / maxBearing;
        return Bot.factorToVisitIndex(factor, factorVisits.length);
    }
}
