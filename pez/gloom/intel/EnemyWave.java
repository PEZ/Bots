package pez.gloom.intel;
import pez.gloom.Bot;
import java.awt.geom.*;

// This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
// http://robowiki.dyndns.org/?RWPCL
//
// $Id: EnemyWave.java,v 1.3 2003/12/06 23:27:41 peter Exp $

public class EnemyWave extends Wave {
    float[] moveFactor;

    public EnemyWave(Bot robot, double bulletPower, Point2D gunLocation, Point2D targetLocation,
                double targetDeltaBearing, double targetMaxBearing, double targetDirection, int[] currentFactorVisits) {

        super(robot, bulletPower, gunLocation, targetLocation, targetDeltaBearing, targetMaxBearing, targetDirection, currentFactorVisits);
        this.moveFactor = moveFactor;
        this.triggerDistanceDelta = 35;
    }

    long getTimeOffset() {
	return -2;
    }

    public boolean powerIsEqual(double power) {
        return (int)Math.round(power * 1000) == (int)Math.round(bulletPower * 1000);
    }

    public double shortestDistance() {
        return Math.abs(gunLocation.distance(currentTargetLocation) - distance());
    }

    public void updateMoveFactor() {
        double visitedFactor = Math.abs(Bot.visitIndexToFactor(visitedIndex, factorVisits.length));
        int mostVisitedIndex = Bot.mostVisitedIndex(factorVisits);
        double mostVisitedFactor = Math.abs(Bot.visitIndexToFactor(mostVisitedIndex, factorVisits.length));
        if (visitedFactor >= 0.9) {
            moveFactor[0] *= (float)(1.05);
        }
        else if (factorVisits[mostVisitedIndex] > Math.max(factorVisits[factorVisits.length - 3],
                Math.max(factorVisits[factorVisits.length - 2], factorVisits[factorVisits.length - 1])) / 2) {
            moveFactor[0] *= (float)(0.97);
        }
    }
}
