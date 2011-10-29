package pez.mini;
import robocode.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.awt.Color;

// Leach - Try to get rid of it
// Home page of this bot is: http://robowiki.dyndns.org/?Leach
// $Id: Leach.java,v 1.3 2003/06/13 15:50:11 peter Exp $

public class Leach extends AdvancedRobot {
    private static Rectangle2D fieldRectangle;
    private static Rectangle2D fluffedFieldRectangle;
    private static double velocityChangeFactor = 0.21;
    private static double velocityMaxFactor = 64;

    private static StringBuffer pattern = new StringBuffer(7000);
    private static ArrayList deltas = new ArrayList(7000);
    private static boolean movieIsFull = false;
    private static int movieSize = 0;

    private Point2D location = new Point2D.Double();
    private Point2D oldLocation;
    private Point2D enemyLocation = new Point2D.Double();
    private Point2D oldEnemyLocation;
    private double accumulatedAngle;
    private Point2D destination;
    private double bulletPower = 3;
    private double enemyBulletPower = 3;
    private double enemyDistance;
    private double enemyEnergy;
    private double enemyBearing;
    private long timeSinceLastScan;
    private double velocity = 8;

    public void run() {
        setColors(Color.black, Color.yellow.brighter().brighter(), Color.yellow.brighter());
        fieldRectangle = new Rectangle2D.Double(0, 0 , getBattleFieldWidth(), getBattleFieldHeight());
        fluffedFieldRectangle = new Rectangle2D.Double(-125, -125 , getBattleFieldWidth() + 125, getBattleFieldHeight() + 125);
        setAdjustGunForRobotTurn(true);

        do {
            double radarOffset = 6.28;
            if(getOthers() == 1 && timeSinceLastScan < 4) {
                radarOffset = normalRelativeAngle(getRadarHeadingRadians() - 0.05 - enemyBearing);
                radarOffset += sign(radarOffset) * 0.07;
            }
            setTurnRadarLeftRadians(radarOffset);
            timeSinceLastScan++;
            if (Math.random() < velocityChangeFactor) {
                velocity = Math.min(8, Math.random() * velocityMaxFactor);
            }
            setMaxVelocity(Math.abs(getTurnRemaining()) > 35 ? 0.1 : velocity);
            if (getEnergy() > 0.2) {
                setFire(bulletPower);
            }
            execute();
        } while (true);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        Point2D prevLocation = new Point2D.Double(location.getX(), location.getY());
        Point2D prevEnemyLocation = new Point2D.Double(enemyLocation.getX(), enemyLocation.getY());
        location.setLocation(getX(), getY());
        enemyDistance = e.getDistance();
        enemyBearing = getHeadingRadians() + e.getBearingRadians();
        double enemyEnergyDelta = enemyEnergy - e.getEnergy();
        if (enemyEnergyDelta >= 0.1 && enemyEnergyDelta <= 3.0) {
            enemyBulletPower = enemyEnergyDelta;
        }
        enemyEnergy = e.getEnergy();
        toLocation(getHeadingRadians() + e.getBearingRadians(), enemyDistance, location, enemyLocation);
        aim();
        record(normalRelativeAngle(e.getHeadingRadians() - enemyBearing), e.getVelocity(),
            absoluteBearing(prevLocation, enemyLocation) - absoluteBearing(prevLocation, prevEnemyLocation));
        decideMove(20 - 3 * enemyBulletPower);
        moveRobot();
        timeSinceLastScan = 0;
    }

    private void decideMove(double enemyBulletVelocity) {
        if (!(oldEnemyLocation == null) && Math.abs(getDistanceRemaining()) < 20) {
            destination = new Point2D.Double();
            double deltaAngle = absoluteBearing(oldEnemyLocation, location) - absoluteBearing(oldEnemyLocation, oldLocation);
            accumulatedAngle += deltaAngle;
            double maxRelativeAngle = Math.asin(8 / enemyBulletVelocity);
            
            double relativeAngle = (0.54 + enemyDistance / 8000) *
                (maxRelativeAngle * 2 * Math.random() - maxRelativeAngle);
            
            double absoluteDestinationAngle = Math.abs(accumulatedAngle + relativeAngle);
            if (absoluteDestinationAngle > maxRelativeAngle) {
                relativeAngle = sign(relativeAngle) * (maxRelativeAngle - Math.abs(accumulatedAngle));
            }
            else if (absoluteDestinationAngle < maxRelativeAngle / 3) {
                relativeAngle *= 1.7 * sign(deltaAngle) * (Math.random() < 0.5 ? -0.15 : 1);
            }
            double distanceExtra = Math.abs(relativeAngle) * 
                (((location.distance(getBattleFieldWidth() / 2, getBattleFieldHeight() / 2) > getBattleFieldHeight() / 2.5) ||
                enemyDistance > 500) ? -45 : 35);
            toLocation(enemyBearing + Math.PI + relativeAngle, enemyDistance + distanceExtra, enemyLocation, destination);
            if (!fluffedFieldRectangle.contains(destination)) {
                toLocation(enemyBearing + Math.PI - relativeAngle, enemyDistance + distanceExtra, enemyLocation, destination);
            }
            translateInsideField(destination, 35);
            oldEnemyLocation.setLocation(enemyLocation);
            oldLocation.setLocation(location);
        }
        else {
            oldEnemyLocation = new Point2D.Double();
            oldEnemyLocation.setLocation(enemyLocation);
            oldLocation = new Point2D.Double();
            oldLocation.setLocation(location);
        }
    }

    private void moveRobot() {
        if (destination != null) {
            double angle = normalRelativeAngle(absoluteBearing(location, destination) - getHeadingRadians());
            int direction = 1;
            if (Math.abs(angle) > Math.PI / 2) {
                angle += Math.acos(direction = -1);
            }
            setTurnRightRadians(normalRelativeAngle(angle));
            setAhead(location.distance(destination) * direction);
        }
    }

    private void aim() {
        bulletPower = Math.min(3.0, Math.min(Math.min(getEnergy() / 4, enemyEnergy / 4), 1200 / enemyDistance));
        double bearing = enemyBearing;
        if (getGunHeat() / getGunCoolingRate() < 1.4) {
            bearing = guessedBearing();
        }
        setTurnGunRightRadians(normalRelativeAngle(bearing - getGunHeadingRadians()));
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

    private double normalRelativeAngle(double angle) {
        return Math.atan2(Math.sin(angle), Math.cos(angle));
    }

    public int travelTime(double distance, double velocity) {
        return (int)Math.round(distance / velocity);
    }

    private void translateInsideField(Point2D point, double margin) {
        point.setLocation(Math.max(margin, Math.min(fieldRectangle.getWidth() - margin, point.getX())),
                          Math.max(margin, Math.min(fieldRectangle.getHeight() - margin, point.getY())));
    }

    void record(double eHeading, double eVelocity, double dBearing) {
        int key = 3;
        key = key * 7 + (int)(eHeading * 5);
        key = key * 7 + (int)(eVelocity * 5);
        pattern.append((char)(key));
        deltas.add(new Double(dBearing));
        if (movieIsFull) {
            pattern.deleteCharAt(0);
            deltas.remove(0);
        }
        else {
            movieSize++;
            movieIsFull = movieSize >= 10000;
        }
    }

    double guessedBearing() {
        int bulletTravelTime = travelTime(location.distance(enemyLocation), 20 - 3 * bulletPower);
        return projectLocation(similarPeriodEndIndex(bulletTravelTime), bulletTravelTime);
    }

    double projectLocation(int start, int bulletTravelTime) {
        double newBearing = enemyBearing;
        int travelTime = 0;
        if (start > 0) {
            if (movieSize > start + bulletTravelTime) {
                for (int i = start; i < movieSize && travelTime <= bulletTravelTime; i++, travelTime++) {
                    newBearing += ((Double)deltas.get(i)).doubleValue();
                }
            }
        }
        return newBearing;
    }

    private int similarPeriodEndIndex(int bulletTravelTime) {
        int index = -1;
        int matchLength = 30;
        if (movieSize > matchLength + bulletTravelTime + 2) {
            for (; matchLength > 2 && index < 0; matchLength -= 2) {
                index = pattern.substring(0, movieSize - matchLength - 2).indexOf(
                    pattern.substring(movieSize - matchLength - 1, movieSize - 1));
            }
        }
        if (index >= 0) {
            return index + matchLength;
        }
        return index;
    }
}
