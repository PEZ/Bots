package pez.mini;
import robocode.*;
import java.awt.geom.*;
import java.awt.Color;
import java.util.*;

// Gouldingi, by PEZ. Small, hard to catch and sharp teeth.
// $Id: Gouldingi.java,v 1.16 2003/08/17 18:11:02 peter Exp $

public class Gouldingi extends AdvancedRobot {
    private static final double DEFAULT_DISTANCE = 600;
    private static final double WALL_MARGIN = 32;
    private static Point2D robotLocation = new Point2D.Double();
    private static Point2D oldLocation = new Point2D.Double();
    private static Point2D enemyLocation = new Point2D.Double();
    private static Point2D oldEnemyLocation = new Point2D.Double();
    private static Rectangle2D fieldRectangle;
    private static double enemyDistance;
    private static double enemyEnergy;
    private static double enemyAbsoluteBearing;
    private static double deltaBearing;
    private static double meanOffsetFactor;
    private static double meanAimFactor[] = new double[3];
    private static int segment;
    private static double shots;
    private double enemyFirePower;
    private double maxRobotVelocity = 8;
    private int direction = 1;
    private long nextTime;
    private boolean doRam = false;

    public void run() {
        fieldRectangle = new Rectangle2D.Double(0, 0 , getBattleFieldWidth(), getBattleFieldHeight());
        setColors(Color.gray, Color.yellow, Color.black);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        turnRadarRightRadians(Double.POSITIVE_INFINITY); 
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        oldLocation.setLocation(robotLocation);
        robotLocation.setLocation(getX(), getY());
        oldEnemyLocation.setLocation(enemyLocation);
        enemyAbsoluteBearing = getHeading() + e.getBearing();
        enemyDistance = e.getDistance();
        toLocation(enemyAbsoluteBearing, enemyDistance, robotLocation, enemyLocation);

        // <movement>
        double enemyEnergyLost = enemyEnergy - e.getEnergy();
        enemyEnergy = e.getEnergy();
        if (enemyEnergyLost >= 0.1 && enemyEnergyLost <= 3.0) {
            enemyFirePower = enemyEnergyLost;
        }
        // <flattener>
        double bulletTravelTime = enemyDistance / (20 - 3 * enemyFirePower);
        if (Math.random() < 1.0 / maxRobotVelocity) {
            maxRobotVelocity = Math.random() * 24;
        }
        if (getTime() > nextTime && Math.random() < 0.5) {
            if (Math.random() < 0.5) {
                direction *= -1;
            }
            nextTime = getTime() + (long)(bulletTravelTime * Math.random() * (1.582 - enemyFirePower / 4.2397));
        }
        // </flattener>
        doRam = enemyEnergy <= 0.25 && getEnergy() > enemyEnergy * 5 && getOthers() == 1;
        goTo(relativeDestination(5 * direction));
        setMaxVelocity(Math.abs(getTurnRemaining()) < 40 ? maxRobotVelocity : 0.1);
        // </movement>

        deltaBearing = normalRelativeAngle(absoluteBearing(oldLocation, enemyLocation) -
            absoluteBearing(oldLocation, oldEnemyLocation));
        segment = getSegment(deltaBearing);
        aim();
        if (enemyEnergy > 0 && (getEnergy() > 0.2 || enemyDistance < 150)) {
            Bullet bullet = setFireBullet(Math.min(getEnergy() / 3, Math.min(enemyEnergy / 4, 3)));
            if (bullet != null) {
                addCustomEvent(new CheckUpdateFactors(bullet));
                shots++;
            }
        }
        setTurnRadarLeftRadians(getRadarTurnRemaining()); 
    }

    public void onHitByBullet(BulletHitEvent e) {
        nextTime -= (nextTime - getTime()) * Math.random();
    }

    Point2D relativeDestination(double relativeAngle) {
        Point2D destination = new Point2D.Double();
        double distanceFactor = 1.05;
        if (doRam) {
            distanceFactor = -1.10;
        }
        else if (enemyDistance > DEFAULT_DISTANCE) {
            distanceFactor = 1.0;
        }
        toLocation(enemyAbsoluteBearing + 180 + relativeAngle, enemyDistance * distanceFactor, enemyLocation, destination);
        double wantedTravelDistance = robotLocation.distance(destination);
        translateInsideField(destination, WALL_MARGIN);
        toLocation(absoluteBearing(robotLocation, destination), wantedTravelDistance, robotLocation, destination);
        translateInsideField(destination, WALL_MARGIN);
        if (robotLocation.distance(destination) < 0.6 * wantedTravelDistance) {
            direction *= -1;
        }
        return destination;
    }

    private void aim() {
        double guessedDistance = robotLocation.distance(enemyLocation);
        double guessedHeading = absoluteBearing(robotLocation, enemyLocation);
        if (Math.abs(deltaBearing) > 0.05) {
            guessedHeading += deltaBearing * meanAimFactor[segment];
        }
        else {
            guessedHeading += meanOffsetFactor;
        }
        Point2D impactLocation = new Point2D.Double();
        toLocation(guessedHeading, guessedDistance, robotLocation, impactLocation);
        guessedHeading = absoluteBearing(robotLocation, impactLocation);
        setTurnGunRight(normalRelativeAngle(guessedHeading - getGunHeading()));
    }

    private void goTo(Point2D point) {
        double distance = robotLocation.distance(point);
        double angle = normalRelativeAngle(absoluteBearing(robotLocation, point) - getHeading());
        if (Math.abs(angle) > 90) {
            distance *= -1;
            if (angle > 0) {
                angle -= 180;
            }
            else {
                angle += 180;
            }
        }
        setTurnRight(angle);
        setAhead(distance);
    }

    private void translateInsideField(Point2D point, double margin) {
        point.setLocation(Math.max(margin, Math.min(fieldRectangle.getWidth() - margin, point.getX())),
                          Math.max(margin, Math.min(fieldRectangle.getHeight() - margin, point.getY())));
    }

    private void toLocation(double angle, double length, Point2D sourceLocation, Point2D targetLocation) {
        targetLocation.setLocation(sourceLocation.getX() + Math.sin(Math.toRadians(angle)) * length,
                                   sourceLocation.getY() + Math.cos(Math.toRadians(angle)) * length);
    }

    private double absoluteBearing(Point2D source, Point2D target) {
        return Math.toDegrees(Math.atan2(target.getX() - source.getX(), target.getY() - source.getY()));
    }

    private double normalRelativeAngle(double angle) {
        angle = Math.toRadians(angle);
        return Math.toDegrees(Math.atan2(Math.sin(angle), Math.cos(angle))); 
    }

    public double rollingAvg(double value, double newEntry, double n, double weighting ) {
        return (value * Math.min(shots, n) + newEntry * weighting)/(Math.min(shots, n) + weighting);
    } 

    private int getSegment(double delta) {
        if (delta < -0.25) {
            return 0;
        }
        else if (delta > 0.25) {
            return 2;
        }
        return 1;
    }

    class CheckUpdateFactors extends Condition {
        private long time;
        private double bulletVelocity;
        private double bulletPower;
        private double bearingDelta;
        private Point2D oldRLocation = new Point2D.Double();
        private Point2D oldELocation = new Point2D.Double();
        private double oldBearing;
        private int segment;

        public CheckUpdateFactors(Bullet bullet) {
            this.time = getTime();
            this.bulletVelocity = bullet.getVelocity();
            this.bulletPower = bullet.getPower();
            this.bearingDelta = deltaBearing;
            this.oldRLocation.setLocation(robotLocation);
            this.oldELocation.setLocation(enemyLocation);
            this.oldBearing = absoluteBearing(oldRLocation, oldELocation);
            this.segment = getSegment(bearingDelta);
        }

        public boolean test() {
            if (bulletVelocity * (getTime() - time) > oldRLocation.distance(enemyLocation) - 10) {
                double impactBearing = absoluteBearing(oldRLocation, enemyLocation);
                double bearingDiff = normalRelativeAngle(impactBearing - oldBearing);
                meanOffsetFactor = rollingAvg(meanOffsetFactor, bearingDiff, 20, bulletPower);
                if (Math.abs(bearingDelta) > 0.05) {
                    meanAimFactor[this.segment] = rollingAvg(meanAimFactor[this.segment], bearingDiff / bearingDelta, 55, bulletPower);
                }
                removeCustomEvent(this);
            }
            return false;
        }
    }
}
