package pez.micro;
import robocode.*;
import robocode.util.Utils;
import java.awt.Color;
import java.awt.geom.*;
import java.util.*;
import java.io.*;

// VertiLeech, by PEZ. Stays close to you and follows your vertical movements
// $Id: VertiLeach.java,v 1.3 2003/09/28 11:17:51 peter Exp $

public class VertiMicro extends AdvancedRobot {
    private static final int DIRECTION_SEGMENTS = 3;
    private static final int VERTICAL_SEGMENTS = 7;
    private static final int DISTANCE_SEGMENTS = 5;
    private static final int AIM_FACTORS = 17;
    private static Point2D robotLocation = new Point2D.Double();
    private static Point2D oldRobotLocation = new Point2D.Double();
    private static Point2D oldEnemyLocation = new Point2D.Double();
    private static Point2D enemyLocation = new Point2D.Double();
    private static double enemyDistance;
    private static double enemyAbsoluteBearing;
    private static double maxEnemyBearing = 0.814339942;
    private static double deltaBearing;
    private static int[][][][] aimFactors = new int[DISTANCE_SEGMENTS][DISTANCE_SEGMENTS][VERTICAL_SEGMENTS][AIM_FACTORS];
    private int[] currentAimFactors;

    public void run() {
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setColors(Color.red.darker(), Color.green.darker(), Color.gray.darker());
        setTurnLeft(getHeading());

        do {
            turnRadarRightRadians(Double.POSITIVE_INFINITY); 
        } while (true);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        oldRobotLocation.setLocation(robotLocation);
        robotLocation.setLocation(getX(), getY());
        enemyAbsoluteBearing = getHeadingRadians() + e.getBearingRadians();
        enemyDistance = e.getDistance();
        oldEnemyLocation.setLocation(enemyLocation);
        toLocation(enemyAbsoluteBearing, enemyDistance, robotLocation, enemyLocation);

        deltaBearing = Utils.normalRelativeAngle(absoluteBearing(oldRobotLocation, enemyLocation) -
            absoluteBearing(oldRobotLocation, oldEnemyLocation));

        currentAimFactors = aimFactors[aimDirectionSegment()]
            [Math.min((int)(enemyDistance / (getBattleFieldWidth() / DISTANCE_SEGMENTS)), DISTANCE_SEGMENTS - 1)]
            [Math.min((int)(enemyLocation.getY() / (getBattleFieldHeight() / VERTICAL_SEGMENTS)), VERTICAL_SEGMENTS - 1)];

        setTurnGunRightRadians(Utils.normalRelativeAngle(
            enemyAbsoluteBearing + maxEnemyBearing * sign(deltaBearing) * mostVisitedFactor() - getGunHeadingRadians()));

        if (getEnergy() > 3.1) {
            Bullet bullet = setFireBullet(3);
            if (bullet != null) {
                Wave wave = new Wave();
                wave.wTime = getTime();
                wave.bearingDelta = deltaBearing;
                wave.oldRLocation.setLocation(robotLocation);
                wave.oldELocation.setLocation(enemyLocation);
                wave.wAimFactors = currentAimFactors;
                addCustomEvent(wave);
            }
        }

        setAhead(getY() > enemyLocation.getY() ? -50 : 50);

        setTurnRadarRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getRadarHeadingRadians()) * 2);
    }

    double mostVisitedFactor() {
        int mostVisited = (AIM_FACTORS - 1) / 2;
        for (int i = 0; i < AIM_FACTORS; i++) {
            if (currentAimFactors[i] > currentAimFactors[mostVisited]) {
                mostVisited = i;
            }
        }
        return (mostVisited - (AIM_FACTORS - 1D) / 2D) / ((AIM_FACTORS - 1D) / 2D);
    }

    private int aimDirectionSegment() {
        double delta = enemyLocation.getY() - oldEnemyLocation.getY();
        if (delta < 0) {
            return 0;
        }
        else if (delta > 0) {
            return 2;
        }
        return 1;
    }

    private int sign(double v) {
        return v > 0 ? 1 : -1;
    }

    private void toLocation(double angle, double length, Point2D sourceLocation, Point2D targetLocation) {
        targetLocation.setLocation(sourceLocation.getX() + Math.sin(angle) * length,
            sourceLocation.getY() + Math.cos(angle) * length);
    }

    private double absoluteBearing(Point2D source, Point2D target) {
        return Math.atan2(target.getX() - source.getX(), target.getY() - source.getY());
    }

    class Wave extends Condition {
        long wTime;
        double bearingDelta;
        Point2D oldRLocation = new Point2D.Double();
        Point2D oldELocation = new Point2D.Double();
        int[] wAimFactors;

        public boolean test() {
            if (11 * (getTime() - wTime) > oldRLocation.distance(enemyLocation)) {
                double bearingDiff = Utils.normalRelativeAngle(absoluteBearing(oldRLocation, enemyLocation) -
                    absoluteBearing(oldRLocation, oldELocation));
                wAimFactors[(int)Math.round(Math.max(0D, Math.min(AIM_FACTORS - 1D,
                    ((sign(bearingDelta) * bearingDiff) / maxEnemyBearing) * (AIM_FACTORS - 1D) / 2D + (AIM_FACTORS - 1D) / 2D)))]++;
                removeCustomEvent(this);
            }
            return false;
        }
    }
}
