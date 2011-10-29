package pez;

import pez.RoboGrapher;
import robocode.*;
import robocode.util.Utils;
import java.awt.geom.*;
import java.awt.Color;
import java.util.*;
import java.util.zip.*;
import java.io.*;

// This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
// http://robowiki.dyndns.org/?RWPCL
//
// RoboGrapherBot - Collecting data for RoboGrapher
// $Id: RoboGrapherBot.java,v 1.3 2004/01/31 15:56:32 peter Exp $

public class RoboGrapherBot extends AdvancedRobot {
    static RoboGrapher roboGrapher = new RoboGrapher(); // Just to make Robocode package it together with the bot
    static final double MAX_VELOCITY = 8;
    static final double WALL_MARGIN = 35;
    static final int ACCEL_SEGMENTS = 3;
    static final int AIM_DISTANCE_SEGMENTS = 5;
    static final int AIM_POWER_SEGMENTS = 5;
    static final int AIM_FACTOR_VISITS = 27;

    static String enemyName = "";
    static Rectangle2D fieldRectangle;
    static Rectangle2D fluffedFieldRectangle;
    static int[][][][][] aimFactorVisits;
    static int[][] currentAimFactorVisits;

    Point2D robotLocation = new Point2D.Double();
    Point2D robotOldLocation = new Point2D.Double();
    Point2D enemyOldLocation = new Point2D.Double();
    Point2D enemyLocation = new Point2D.Double();
    double bulletPower;
    double robotEnergy;
    double enemyDistance;
    double enemyEnergy;
    double enemyAbsoluteBearing;
    double enemyFirePower = 3;
    double enemyDeltaBearing;
    double enemyOldDeltaBearing;
    double robotAbsoluteBearing;
    boolean factorsAreSaved = false;
    double defaultDistance = 200 + Math.random() * 800;
    private double direction = 1;

    public void run() {
	fieldRectangle = new Rectangle2D.Double(0, 0 , getBattleFieldWidth(), getBattleFieldHeight());
	fluffedFieldRectangle = new Rectangle2D.Double(-150, -150, getBattleFieldWidth() + 150, getBattleFieldHeight() + 150);
	setColors(Color.pink.brighter(), Color.pink.brighter(), Color.pink.brighter());

	setAdjustGunForRobotTurn(true);
	setAdjustRadarForGunTurn(true);

        bulletPower = 0.5 + Math.random() * 3.0;
        bulletPower = Math.min(3.0, bulletPower);

	while (true) {
	    turnRadarRightRadians(Double.POSITIVE_INFINITY); 
	}
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        if (enemyName == "") {
            enemyName = e.getName();
            restoreFactors();
        }
        robotOldLocation.setLocation(robotLocation);
        robotLocation.setLocation(getX(), getY());
        enemyAbsoluteBearing = getHeadingRadians() + e.getBearingRadians();
        robotAbsoluteBearing = Math.PI + enemyAbsoluteBearing;
        enemyDistance = e.getDistance();
        enemyOldLocation.setLocation(enemyLocation);
        vectorToLocation(enemyAbsoluteBearing, enemyDistance, robotLocation, enemyLocation);

        enemyOldDeltaBearing = enemyDeltaBearing;
        enemyDeltaBearing = Utils.normalRelativeAngle(absoluteBearing(robotOldLocation, enemyLocation) -
            absoluteBearing(robotOldLocation, enemyOldLocation));

        enemyDeltaBearing = e.getVelocity() * Math.sin(e.getHeadingRadians() - enemyAbsoluteBearing);
	
	robotEnergy = getEnergy();
        bulletPower = Math.min(robotEnergy, bulletPower);

        double enemyEnergyLost = enemyEnergy - e.getEnergy();
        enemyEnergy = e.getEnergy();
        if (enemyEnergyLost >= 0.1 && enemyEnergyLost <= 3.0) {
            enemyFirePower = enemyEnergyLost;
        }

        move(bulletVelocity(enemyFirePower));

        gun();

        setTurnRadarRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getRadarHeadingRadians()) * 2);
    }

    public void onDeath(DeathEvent e) {
        finishRound();
    }

    public void onWin(WinEvent e) {
        finishRound();
    }

    public void onCustomEvent(CustomEvent e) {
        Condition condition = e.getCondition();
        if (condition instanceof GrapherWave) {
            ((GrapherWave)condition).updateStats();
        }
    }

    void move(double enemyBulletVelocity) {
	Point2D robotDestination = new Point2D.Double();
	double bulletTravelTime = enemyDistance / enemyBulletVelocity;
	if (Math.random() < Math.min(0.13, Math.pow(2.4 / bulletTravelTime, 1.08))) {
	    direction *= -1;
	}
	for (int i = 0; i < 2; i++) {
	    double tries = 0;
	    do {
		vectorToLocation(absoluteBearing(enemyLocation, robotLocation) + (direction * 0.2),
			enemyDistance * (1.1 - tries / 100.0), enemyLocation, robotDestination);
		tries++;
	    } while (tries < 40 && !fieldRectangle(WALL_MARGIN).contains(robotDestination));
	    if (!fieldRectangle(WALL_MARGIN).contains(robotDestination)) {
		direction *= -1;
	    }
	    else {
		break;
	    }
	}
	goTo(robotDestination);
	setMaxVelocity(Math.abs(getTurnRemaining()) > 25 ? 0 : MAX_VELOCITY);
    }

    private RoundRectangle2D fieldRectangle(double margin) {
        return new RoundRectangle2D.Double(margin, margin,
	    getBattleFieldWidth() - margin * 2, getBattleFieldHeight() - margin * 2, 75, 75);
    }

    private void goTo(Point2D destination) {
        double angle = Utils.normalRelativeAngle(absoluteBearing(robotLocation, destination) - getHeadingRadians());
	double turnAngle = Math.atan(Math.tan(angle));
        setTurnRightRadians(turnAngle);
        setAhead(robotLocation.distance(destination) * (angle == turnAngle ? 1 : -1));
    }

    void gun() {
        currentAimFactorVisits = aimFactorVisits[aimAccelSegment()][aimDistanceSegment()][aimPowerSegment()];
        if (setFireBullet(bulletPower) != null) {
            addCustomEvent(new GrapherWave(this, bulletPower, robotLocation, enemyLocation,
                    enemyDeltaBearing, currentAimFactorVisits[0]));
        }
        else if (robotEnergy > 0) {
            addCustomEvent(new GrapherWave(this, bulletPower, robotLocation, enemyLocation,
                    enemyDeltaBearing, currentAimFactorVisits[1]));
        }
        setTurnGunRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing +
	    maxEscapeAngle(bulletPower) * sign(enemyDeltaBearing) * aimFactor() - getGunHeadingRadians()));
    }

    static double visitIndexToFactor(int index, int numFactors) {
	return ((index + 0.5) / numFactors) * 2 - 1;
    }

    static int factorToVisitIndex(double factor, int numFactors) {
	int index = (int)(((factor + 1) / 2) * numFactors);
	index = Math.min(numFactors - 1, index);
	index = Math.max(0, index);
	return index;
    }

    static double mostVisitedFactor(int[] factors) {
        return visitIndexToFactor(mostVisitedIndex(factors), factors.length);
    }

    static int mostVisitedIndex(int[] factors) {
        int numFactors = factors.length;
        int mostVisited = (numFactors - 1) / 2;
        for (int i = 0; i < numFactors; i++) {
            if (factors[i] > factors[mostVisited]) {
                mostVisited = i;
            }
        }
        return mostVisited;
    }

    double aimFactor() {
        return mostVisitedFactor(currentAimFactorVisits[0]);
    }

    int aimAccelSegment() {
        return accelSegment(enemyDeltaBearing, enemyOldDeltaBearing);
    }

    int aimDistanceSegment() {
        return distanceSegment(enemyDistance, AIM_DISTANCE_SEGMENTS);
    }

    int aimPowerSegment() {
        return powerSegment(bulletPower, AIM_POWER_SEGMENTS);
    }

    int accelSegment(double deltaBearing, double oldDeltaBearing) {
        int delta = (int)(Math.round(5 * enemyDistance * (Math.abs(deltaBearing) - Math.abs(oldDeltaBearing))));
        if (delta < 0) {
            return 0;
        }
        else if (delta > 0) {
            return 2;
        }
        return 1;
    }

    int distanceSegment(double distance, int segments) {
        return Math.min((int)(distance / (getBattleFieldWidth() / segments)), segments - 1);
    }

    int powerSegment(double power, int segments) {
        return Math.min((int)(power / (3D / segments)), segments - 1);
    }

    void finishRound() {
        if (!factorsAreSaved) {
            factorsAreSaved = true;
            saveFactors();
        }
    }

    void restoreFactors() {
        try {
            ZipInputStream zipin = new ZipInputStream(new
                FileInputStream(getDataFile(enemyName + ".zip")));
            zipin.getNextEntry();
            ObjectInputStream in = new ObjectInputStream(zipin);
            aimFactorVisits = (int[][][][][])in.readObject();
            in.close();
        }
        catch (IOException e) {
            System.out.println("Ah! A new aquaintance. I'll be watching you " + enemyName + ".");
            aimFactorVisits = new int[ACCEL_SEGMENTS][AIM_DISTANCE_SEGMENTS][AIM_POWER_SEGMENTS][2][AIM_FACTOR_VISITS];
        }
        catch (ClassNotFoundException e) {
            System.out.println("Error reading enemy aim factors:" + e);
        }
    }

    void saveFactors() {
        try {
            ZipOutputStream zipout = new ZipOutputStream(new RobocodeFileOutputStream(getDataFile(enemyName + ".zip")));
            zipout.putNextEntry(new ZipEntry("aimFactors"));
            ObjectOutputStream out = new ObjectOutputStream(zipout);
            out.writeObject(aimFactorVisits);
            out.flush();
            zipout.closeEntry();
            out.close();
        }
        catch (IOException e) {
            System.out.println("Error writing factors:" + e);
        }
    }

    static void translateInsideField(Point2D point, double margin) {
        point.setLocation(Math.max(margin, Math.min(fieldRectangle.getWidth() - margin, point.getX())),
                          Math.max(margin, Math.min(fieldRectangle.getHeight() - margin, point.getY())));
    }

    static double bulletVelocity(double power) {
        return 20 - 3 * power;
    }

    static int sign(double v) {
        return v > 0 ? 1 : -1;
    }

    static void vectorToLocation(double angle, double length, Point2D sourceLocation, Point2D targetLocation) {
        targetLocation.setLocation(sourceLocation.getX() + Math.sin(angle) * length,
            sourceLocation.getY() + Math.cos(angle) * length);
    }

    static double absoluteBearing(Point2D source, Point2D target) {
        return Math.atan2(target.getX() - source.getX(), target.getY() - source.getY());
    }

    static double maxEscapeAngle(double power) {
        return Math.asin(MAX_VELOCITY / (bulletVelocity(power)));
    }
}

class GrapherWave extends Condition {
    RoboGrapherBot robot;
    double time;
    double bulletPower;
    double bulletVelocity;
    double deltaBearing;
    Point2D oldGunLocation = new Point2D.Double();
    Point2D oldTargetLocation = new Point2D.Double();
    Point2D currentTargetLocation;
    int[] factorVisits;
    double triggerDistanceDelta = -10;
    int visitedIndex = -1;

    public GrapherWave(RoboGrapherBot robot, double bulletPower, Point2D gunLocation, Point2D targetLocation,
                double targetDeltaBearing, int[] currentFactorVisits) {

        this.robot = robot;
        this.time = robot.getTime() - 1;
        this.bulletPower = bulletPower;
        this.bulletVelocity = RoboGrapherBot.bulletVelocity(bulletPower);
        this.deltaBearing = targetDeltaBearing;
        this.oldGunLocation.setLocation(gunLocation);
        this.currentTargetLocation = targetLocation;
        this.oldTargetLocation.setLocation(targetLocation);
        this.factorVisits = currentFactorVisits;
    }

    public boolean test() {
        if (triggerTest(triggerDistanceDelta)) {
            robot.removeCustomEvent(this);
            return true;
        }
        return false;
    }

    double distance() {
        return bulletVelocity * (double)(robot.getTime() - time);
    }

    boolean triggerTest(double distanceDelta) {
        return distance() > oldGunLocation.distance(currentTargetLocation) + distanceDelta;
    }

    void updateStats() {
        double bearingDiff = Utils.normalRelativeAngle(RoboGrapherBot.absoluteBearing(oldGunLocation, currentTargetLocation) -
            RoboGrapherBot.absoluteBearing(oldGunLocation, oldTargetLocation));
	double factor = RoboGrapherBot.sign(deltaBearing) * bearingDiff / RoboGrapherBot.maxEscapeAngle(bulletPower);
        factorVisits[RoboGrapherBot.factorToVisitIndex(factor, factorVisits.length)]++;
    }
}
