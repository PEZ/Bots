package pez.mini;
import robocode.*;
import robocode.util.Utils;
import java.awt.geom.*;
import java.awt.Color;
import java.util.*;
import java.util.zip.*;
import java.io.*;

// HypoLeach, by PEZ. Stays close to you and follows your diagomal movements
//
// This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
// http://robowiki.dyndns.org/?RWPCL
//
// Code home page: http://robowiki.dyndns.org/?HypoLeach/Code
//
// $Id: HypoLeach.java,v 1.2 2003/11/03 22:44:23 peter Exp $

public class HypoLeach extends AdvancedRobot {
    private static final double MAX_VELOCITY = 8;
    private static final double DEFAULT_DISTANCE = 250;
    private static final double DEFAULT_BULLET_POWER = 3.0;
    private static final double WALL_MARGIN = 35;
    private static final int MAX_FACTOR_VISITS = 255;
    private static final int DIRECTION_SEGMENTS = 3;
    private static final int DIAGONAL_SEGMENTS = 9;
    private static final int AIM_FACTORS = 15;
    private static Point2D robotLocation = new Point2D.Double();
    private static Point2D oldRobotLocation = new Point2D.Double();
    private static Point2D oldEnemyLocation = new Point2D.Double();
    private static Point2D enemyLocation = new Point2D.Double();
    private static Rectangle2D fieldRectangle;
    private static Point2D center;
    private static double robotEnergy;
    private static String enemyName = "";
    private static double enemyDistance;
    private static double enemyEnergy;
    private static double enemyAbsoluteBearing;
    private static double maxEnemyBearing;
    private static double deltaBearing;
    private static int aimDiagonalSegment;
    private static int oldAimDiagonalSegment;
    private static double enemyFirePower = DEFAULT_BULLET_POWER;
    private static double yOffset;
    private static int[][][] aimFactors;
    private int[] currentAimFactors;
    private Point2D robotDestination = null;
    private boolean shouldRam;

    public void run() {
        fieldRectangle = new Rectangle2D.Double(0, 0 , getBattleFieldWidth(), getBattleFieldHeight());
	center = new Point2D.Double(fieldRectangle.getCenterX(), fieldRectangle.getCenterY());
        setColors(Color.pink.darker().darker(), Color.yellow.brighter().brighter(), Color.black);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        do {
            turnRadarRightRadians(Double.POSITIVE_INFINITY); 
        } while (true);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        if (enemyName == "") {
            enemyName = e.getName();
            restoreFactors();
        }
        oldRobotLocation.setLocation(robotLocation);
        robotLocation.setLocation(getX(), getY());
        enemyAbsoluteBearing = getHeadingRadians() + e.getBearingRadians();
        enemyDistance = e.getDistance();
        oldEnemyLocation.setLocation(enemyLocation);
        toLocation(enemyAbsoluteBearing, enemyDistance, robotLocation, enemyLocation);

	double delta = enemyEnergy - e.getEnergy();
	if (delta >= 0.1 && delta <= 3.0) {
	    enemyFirePower = delta;
	    yOffset = 2 * Math.random() * Math.PI / 7 - Math.PI / 7;
	}
        enemyEnergy = e.getEnergy();
        robotEnergy = getEnergy();
        shouldRam = enemyEnergy == 0 || (enemyEnergy < 0.3 && robotEnergy > enemyEnergy * 11);

	Point2D dest1 = new Point2D.Double();
	Point2D dest2 = new Point2D.Double();
	toLocation((Math.PI / 2 + Math.PI / 4) + yOffset, (shouldRam ? 0 : DEFAULT_DISTANCE), enemyLocation, dest1);
	toLocation(-(Math.PI / 4) + yOffset, (shouldRam ? 0 : DEFAULT_DISTANCE), enemyLocation, dest2);
	if (center.distance(dest1) < robotLocation.distance(dest2)) {
	    robotDestination = dest1;
	}
	else {
	    robotDestination = dest2;
	}
        translateInsideField(robotDestination, WALL_MARGIN);
        goTo(robotDestination);

        deltaBearing = Utils.normalRelativeAngle(absoluteBearing(oldRobotLocation, enemyLocation) -
            absoluteBearing(oldRobotLocation, oldEnemyLocation));
        double bulletPower = Math.min(robotEnergy / 5, Math.min(enemyEnergy / 4, enemyFirePower));
        maxEnemyBearing = Math.abs(Math.asin(MAX_VELOCITY / (bulletVelocity(bulletPower))));

	aimDiagonalSegment = aimDiagonalSegment();
        currentAimFactors = aimFactors[aimDirectionSegment()][aimDiagonalSegment];
	oldAimDiagonalSegment = aimDiagonalSegment;

        setTurnGunRightRadians(Utils.normalRelativeAngle(
            enemyAbsoluteBearing + maxEnemyBearing * sign(deltaBearing) * mostVisitedFactor() - getGunHeadingRadians()));

        if (!shouldRam && (robotEnergy > 1.5 || enemyDistance < 200)) {
            if (setFireBullet(bulletPower) != null && bulletPower > 0.9) {
                addCustomEvent(new Wave(bulletVelocity(bulletPower)));
            }
        }

        setTurnRadarRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getRadarHeadingRadians()) * 2);
    }

    public void onWin(WinEvent e) {
        saveFactors();
    }

    public void onDeath(DeathEvent e) {
        saveFactors();
    }

    void goTo(Point2D destination) {
        double angle = Utils.normalRelativeAngle(absoluteBearing(robotLocation, destination) - getHeadingRadians());
        int direction = 1;
        if (Math.abs(angle) > Math.PI / 2) {
            angle += Math.acos(direction = -1);
        }
        setTurnRightRadians(Utils.normalRelativeAngle(angle));
        setAhead(robotLocation.distance(destination) * direction);
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

    private int aimDiagonalSegment() {
	Point2D corner = getX() < enemyLocation.getX() ?
	    new Point2D.Double(getBattleFieldHeight(), 0.0) :
	    new Point2D.Double(0.0, getBattleFieldWidth());
	double relativeAngle = Utils.normalRelativeAngle(absoluteBearing(center, enemyLocation) - absoluteBearing(center, corner));
        return Math.min((int)(Math.max(0.0, relativeAngle + Math.PI / 2) / ((Math.PI / 2) / DIAGONAL_SEGMENTS)), DIAGONAL_SEGMENTS - 1);
    }

    private int aimDirectionSegment() {
        double delta = aimDiagonalSegment - oldAimDiagonalSegment;
        if (delta < -0.005) {
            return 0;
        }
        else if (delta > 0.005) {
            return 2;
        }
        return 1;
    }

    private void translateInsideField(Point2D point, double margin) {
        point.setLocation(Math.max(margin, Math.min(fieldRectangle.getWidth() - margin, point.getX())),
                          Math.max(margin, Math.min(fieldRectangle.getHeight() - margin, point.getY())));
    }

    private double bulletVelocity(double power) {
        return 20 - 3 * power;
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

    void restoreFactors() {
        try {
            ZipInputStream zipin = new ZipInputStream(new
                FileInputStream(getDataFile(enemyName + ".zip")));
            zipin.getNextEntry();
            ObjectInputStream in = new ObjectInputStream(zipin);
            aimFactors = (int[][][])in.readObject();
            in.close();
        }
        catch (Exception e) {
            aimFactors = new int[DIRECTION_SEGMENTS][DIAGONAL_SEGMENTS][AIM_FACTORS];
        }
    }

    void saveFactors() {
        try {
            ZipOutputStream zipout = new ZipOutputStream(new RobocodeFileOutputStream(getDataFile(enemyName + ".zip")));
            zipout.putNextEntry(new ZipEntry("aimFactors"));
            ObjectOutputStream out = new ObjectOutputStream(zipout);
            out.writeObject(aimFactors);
            out.flush();
            zipout.closeEntry();
            out.close();
        }
        catch (IOException e) {
        }
    }

    class Wave extends Condition {
        private long wTime;
        private double bulletVelocity;
        private double bearingDelta;
        private Point2D oldRLocation = new Point2D.Double();
        private Point2D oldELocation = new Point2D.Double();
        private double maxBearing;
        int[] wAimFactors;

        public Wave(double bulletVelocity) {
            this.wTime = getTime();
            this.bulletVelocity = bulletVelocity;
            this.bearingDelta = deltaBearing;
            this.oldRLocation.setLocation(robotLocation);
            this.oldELocation.setLocation(enemyLocation);
            this.maxBearing = maxEnemyBearing;
            this.wAimFactors = currentAimFactors;
        }

        public boolean test() {
            if (bulletVelocity * (getTime() - wTime) > oldRLocation.distance(enemyLocation) - 10) {
                double bearingDiff = Utils.normalRelativeAngle(absoluteBearing(oldRLocation, enemyLocation) -
                    absoluteBearing(oldRLocation, oldELocation));
                wAimFactors[(int)Math.round(Math.max(0D, Math.min(AIM_FACTORS - 1D,
                    ((sign(bearingDelta) * bearingDiff) / maxBearing) * (AIM_FACTORS - 1D) / 2D + (AIM_FACTORS - 1D) / 2D)))]++;
                removeCustomEvent(this);
            }
            return false;
        }
    }
}
