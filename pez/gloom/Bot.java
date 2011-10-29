package pez.gloom;
import pez.gloom.intel.*;
import robocode.*;
import robocode.util.Utils;
import java.awt.geom.*;
import java.util.*;
import java.util.zip.*;
import java.io.*;

// GloomyDark
// This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
// http://robowiki.dyndns.org/?RWPCL
//
// $Id: Bot.java,v 1.6 2003/12/06 23:27:41 peter Exp $

public abstract class Bot extends AdvancedRobot {
    public static final double MAX_VELOCITY = 8;
    public static final double MAX_DISTANCE = 700;
    public static final double BOT_WIDTH = 36;
    public static final int ACCEL_SEGMENTS = 3;
    public static final int AIM_FACTORS = 37;
    public static final int MOVE_FACTORS = 15;
    public static final int MOVE_POWER_SEGMENTS = 5;

    static String enemyName = "";
    public static Rectangle2D fieldRectangle;
    public static Rectangle2D fluffedFieldRectangle;
    static double cornerDistance;
    static int[][][][] aimFactorVisits;
    static int[][][] moveFactorVisits;
    static float[][][] moveFactors;
    static float[] currentMoveFactor;
    static long enemyShots;

    int[] currentAimFactorVisits;
    int[] currentMoveFactorVisits;

    List enemyWaves = new ArrayList();

    public Point2D robotLocation = new Point2D.Double();
    public Point2D robotOldLocation = new Point2D.Double();
    public Point2D enemyOldLocation = new Point2D.Double();
    public Point2D enemyLocation = new Point2D.Double();
    public Point2D robotDestination = new Point2D.Double();
    public double robotEnergy;
    public double enemyDistance;
    public double enemyVelocity;
    public double enemyOldVelocity;
    public double enemyEnergy;
    public double enemyBearing;
    public double enemyAbsoluteBearing;
    public double enemyFirePower = 3;
    public int enemyMoveTime;
    public double enemyBulletTravelTime;
    public double enemyMaxBearing;
    public double enemyDeltaBearing;
    public double enemyOldDeltaBearing;
    public double enemyBearingDirection = 1;
    public double robotAbsoluteBearing;
    public double robotMaxBearing;
    public double robotDeltaBearing;
    public double robotOldDeltaBearing;
    public boolean enemyHasFired;

    public int timeSinceEnemyFired;
    boolean factorsAreSaved = false;
    int timeSinceLastScan = 10;

    public void run() {
        if (enemyName == "") {
            fieldRectangle = new Rectangle2D.Double(0, 0 , getBattleFieldWidth(), getBattleFieldHeight());
            fluffedFieldRectangle = new Rectangle2D.Double(-145, -145, getBattleFieldWidth() + 145, getBattleFieldHeight() + 145);
            cornerDistance = Point2D.distance(0, 0, fieldRectangle.getCenterX(), fieldRectangle.getCenterY());
        }

	setEventPriority("ScannedRobotEvent", 85);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        init();

        while (true) {
            turnRadarRightRadians(Double.POSITIVE_INFINITY); 
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        timeSinceLastScan = 0;
	if (enemyName == "") {
	    enemyName = e.getName();
	    moveFactorVisits = new int[getMovePowerSegments()][ACCEL_SEGMENTS][MOVE_FACTORS];
	    moveFactors = new float[getMovePowerSegments()][ACCEL_SEGMENTS][1];
	    /*
	    for (int p = 0; p < getMovePowerSegments(); p++) {
		for (int a = 0; a < ACCEL_SEGMENTS; a++) {
		    moveFactors[p][a][0] = initialMoveFactor();
		}
	    }
	    */
	    restoreFactors();
	}
        robotOldLocation.setLocation(robotLocation);
        robotLocation.setLocation(getX(), getY());
        enemyBearing = e.getBearingRadians();
        enemyAbsoluteBearing = getHeadingRadians() + enemyBearing;
        robotAbsoluteBearing = Math.PI + enemyAbsoluteBearing;
        enemyDistance = e.getDistance();
	enemyOldVelocity = enemyVelocity;
        enemyVelocity = e.getVelocity();
        enemyOldLocation.setLocation(enemyLocation);
        toLocation(enemyAbsoluteBearing, enemyDistance, robotLocation, enemyLocation);

        enemyOldDeltaBearing = enemyDeltaBearing;
        enemyDeltaBearing = normalRelativeAngle(absoluteBearing(robotOldLocation, enemyLocation) -
            absoluteBearing(robotOldLocation, enemyOldLocation));

	if (enemyDeltaBearing > 0) {
	    enemyBearingDirection = 1;
	}
	else if (enemyDeltaBearing < 0) {
	    enemyBearingDirection = -1;
	}

	if (enemyVelocity == 0) {
	    enemyMoveTime = 0;
	}
	else {
	    enemyMoveTime++;
	}

        robotOldDeltaBearing = robotDeltaBearing;
        robotDeltaBearing = normalRelativeAngle(absoluteBearing(enemyOldLocation, robotLocation) -
            absoluteBearing(enemyOldLocation, robotOldLocation));

        robotEnergy = getEnergy();
        double enemyEnergyLost = enemyEnergy - e.getEnergy();
        enemyEnergy = e.getEnergy();
        timeSinceEnemyFired++;
        if (enemyEnergyLost >= 0.1 && enemyEnergyLost <= 3.0) {
            enemyFirePower = enemyEnergyLost;
            timeSinceEnemyFired = 0;
            enemyHasFired = true;
            enemyShots++;
        }
        else {
            enemyHasFired = false;
        }

        currentMoveFactorVisits = moveFactorVisits[movePowerSegment()][moveAccelSegment()];
        currentMoveFactor = moveFactors[movePowerSegment()][moveAccelSegment()];
        robotMaxBearing = maxBearing(enemyFirePower);
	/*
        if (enemyHasFired) {
            EnemyWave ew = new EnemyWave(this, enemyFirePower, enemyOldLocation, robotOldLocation,
                robotDeltaBearing, robotMaxBearing, currentMoveFactorVisits, currentMoveFactor);
            addCustomEvent(ew);
            enemyWaves.add(ew);
        }
	*/

        enemyBulletTravelTime = enemyDistance / (bulletVelocity(enemyFirePower));
        move();

        enemyMaxBearing = maxBearing(bulletPower());
        currentAimFactorVisits = aimFactorVisits[aimMoveTimeSegment()][aimAccelSegment()][aimDistanceSegment()];

        gun();

        setTurnRadarRightRadians(normalRelativeAngle(enemyAbsoluteBearing - getRadarHeadingRadians()) * 2);
    }

    public void onDeath(DeathEvent e) {
        finishRound();
    }

    public void onWin(WinEvent e) {
        finishRound();
    }

    public void onHitByBullet(HitByBulletEvent e) {
        //updateMoveFactor(e.getPower());
    }

    public void onCustomEvent(CustomEvent e) {
        Condition condition = e.getCondition();
        if (condition instanceof RobotWave) {
            ((Wave)condition).updateStats();
        }
	/*
        if (condition instanceof EnemyWave) {
            enemyWaves.remove(condition);
        }
	*/
    }

    public int getAimAccelSegments() {
	return ACCEL_SEGMENTS;
    }

    public abstract boolean shouldRam();

    public abstract boolean shouldEvade();

    public abstract double bulletPower();

    public abstract double getFightingDistance(double defaultDistance);

    abstract void init();

    abstract void move();

    abstract void gun();

    abstract boolean shouldFire();

    abstract double getDefaultBulletPower();

    abstract int getMaxAimFactorVisits();

    abstract int getAimDistanceSegments();

    abstract int getAimVelocitySegments();

    abstract int getAimMoveTimeSegments();

    abstract int getAimPowerSegments();

    abstract int getMoveDistanceSegments();

    abstract int getMovePowerSegments();

    public boolean isCornered() {
        return  robotLocation.distance(getBattleFieldWidth() / 2, getBattleFieldHeight() / 2) > 0.85 * cornerDistance;
    }

    void updateMoveFactor(double power) {
        double shortestDistance = 50;
        EnemyWave selectedWave = null;
        Iterator iterator = enemyWaves.iterator();
        for (int i = 0, n = enemyWaves.size(); i < n; i++) {
            EnemyWave ew = (EnemyWave)enemyWaves.get(i);
            if (ew.powerIsEqual(power) && ew.shortestDistance() < shortestDistance) {
                selectedWave = ew;
                shortestDistance = ew.shortestDistance();
            }
        }
        if (selectedWave != null) {
            selectedWave.updateStats();
            selectedWave.updateMoveFactor();
        }
    }

    public static double visitIndexToFactor(int index, int numFactors) {
	return ((index + 0.5) / numFactors) * 2 - 1;
    }

    public static int factorToVisitIndex(double factor, int numFactors) {
	int index = (int)(((factor + 1) / 2) * numFactors);
if (index < 0 || index > numFactors -1) System.out.println(numFactors + ", " + index);
	return Math.min(Math.max(0, index), numFactors -1);
    }

    public static double mostVisitedFactor(int[] factors) {
        return visitIndexToFactor(mostVisitedIndex(factors), factors.length);
    }

    public static int mostVisitedIndex(int[] factors) {
        int numFactors = factors.length;
        int mostVisited = numFactors / 2;
        for (int i = 0; i < numFactors; i++) {
            if (factors[i] > factors[mostVisited]) {
                mostVisited = i;
            }
        }
        return mostVisited;
    }

    public void registerFactorVisit(int[] factors, int index) {
        if (factors[index] < getMaxAimFactorVisits() - 3) {
	    if (index > 0) factors[index - 1] += 1;
            factors[index] += 2;
	    if (index < factors.length - 1) factors[index + 1] += 1;
        }
        else {
            decrementAllFactors(factors, index);
	    registerFactorVisit(factors, index);
        }
    }

    void decrementAllFactors(int[] factors, int index) {
        for (int i = 0, numFactors = factors.length; i < numFactors; i++) {
            if (factors[i] > 3) {
		factors[i] -= 3;
	    }
        }
    }

    public double aimFactor(int[] factors) {
	int index = mostVisitedIndex(factors);
        return visitIndexToFactor(index, factors.length);
    }

    int aimAccelSegment() {
        return accelSegment(enemyVelocity, enemyOldVelocity);
    }

    int aimVelocitySegment() {
        return velocitySegment(enemyVelocity, getAimVelocitySegments());
    }

    int aimDistanceSegment() {
        return distanceSegment(enemyDistance, getAimDistanceSegments());
    }

    int aimMoveTimeSegment() {
        return moveTimeSegment(enemyMoveTime, getAimMoveTimeSegments());
    }

    int aimPowerSegment() {
        return powerSegment(bulletPower(), getAimPowerSegments());
    }

    int moveAccelSegment() {
	//FIX, use velocity
        return accelSegment(robotDeltaBearing, robotOldDeltaBearing);
    }

    int moveDistanceSegment() {
        return distanceSegment(enemyDistance, getMovePowerSegments());
    }

    int movePowerSegment() {
        return powerSegment(enemyFirePower, getMovePowerSegments());
    }

    int accelSegment(double velocity, double oldVelocity) {
        int delta = (int)(velocity - oldVelocity);
        if (delta < 0) {
            return 0;
        }
        else if (delta > 0) {
            return 2;
        }
        return 1;
    }

    int moveTimeSegment(int moveTime, int segments) {
	double bulletTravelTime = enemyDistance / Bot.bulletVelocity(bulletPower());
        return Math.min((int)Math.round(moveTime / (bulletTravelTime / segments)), segments - 1);
    }

    int velocitySegment(double velocity, int segments) {
        return Math.min((int)(Math.abs(velocity) / (MAX_VELOCITY / segments)), segments - 1);
    }

    int distanceSegment(double distance, int segments) {
        return Math.min((int)Math.round(distance / (MAX_DISTANCE / segments)), segments - 1);
    }

    int powerSegment(double power, int segments) {
        return Math.min((int)Math.round(power / (3D / segments)), segments - 1);
    }

    public void goTo(Point2D point) {
        double distance = robotLocation.distance(point);
        double angle = normalRelativeAngle(absoluteBearing(robotLocation, point) - getHeadingRadians());
        if (Math.abs(angle) > Math.PI / 2) {
            distance *= -1;
            if (angle > 0) {
                angle -= Math.PI;
            }
            else {
                angle += Math.PI;
            }
        }
        setTurnRightRadians(angle);
        setAhead(distance);
    }

    private boolean isMirrored() {
        return (new Point2D.Double(fieldRectangle.getCenterX() - (robotLocation.getX() - fieldRectangle.getCenterX()),
	    fieldRectangle.getCenterY() - (robotLocation.getY() - fieldRectangle.getCenterY()))).distance(enemyLocation) < 50;
    }

    void finishRound() {
        if (!factorsAreSaved) {
            factorsAreSaved = true;
            saveFactors();
        }
    }

    float initialMoveFactor() {
        return 0.75F;
    }

    int numFactors(double distance) {
        int n = (int)(1.5 * (distance / bulletVelocity(getDefaultBulletPower()) * MAX_VELOCITY / BOT_WIDTH));
        return Math.min(AIM_FACTORS, n + 1 - n % 2);
    }

    int[][][][] makeAimFactorVisits() {
        int[][][][] factors = new int[getAimMoveTimeSegments()][getAimAccelSegments()][getAimDistanceSegments()][];
        for (int s1 = 0; s1 < getAimMoveTimeSegments(); s1++) {
            for (int s2 = 0; s2 < getAimAccelSegments(); s2++) {
                for (int s3 = 0; s3 < getAimDistanceSegments(); s3++) {
		    factors[s1][s2][s3] = new int[numFactors((s3 + 2) * MAX_DISTANCE / getAimDistanceSegments())];
                }
            }
        }
        return factors;
    }

    void restoreFactors() {
	try {
	    ZipInputStream zipin = new ZipInputStream(new
		    FileInputStream(getDataFile(enemyName + ".zip")));
	    zipin.getNextEntry();
	    ObjectInputStream in = new ObjectInputStream(zipin);
	    aimFactorVisits = (int[][][][])in.readObject();
	    in.close();
	    System.out.println("My heart turns dark and gloomy seeing you again " + enemyName + ".");
	}
	catch (IOException e) {
	    System.out.println("Ah! A new aquaintance. I'll be watching you " + enemyName + ".");
	    aimFactorVisits = new int[getAimMoveTimeSegments()][getAimAccelSegments()][getAimDistanceSegments()][AIM_FACTORS];
	    //aimFactorVisits = makeAimFactorVisits();
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
	    /*
	    if (getRoundNum() == getNumRounds() -1) {
		for (int a = 0; a < ACCEL_SEGMENTS; a++) {
		    for (int p = 0; p < getAimPowerSegments(); p++) {
			for (int d = 0; d < getAimDistanceSegments(); d++) {
				for (int i = 0; i < aimFactorVisits[a][p][d].length; i++) {
				    if (i != mostVisitedIndex(aimFactorVisits[a][p][d])) {
					aimFactorVisits[a][p][d][i] = 0;
				    }
				    else {
					aimFactorVisits[a][p][d][i] = 10;
				    }
				}
			}
		    }
		}
	    }
	    */
            out.writeObject(aimFactorVisits);
            out.flush();
            zipout.closeEntry();
            out.close();
        }
        catch (IOException e) {
            System.out.println("Error writing factors:" + e);
        }
    }

    public static void translateInsideField(Rectangle2D field, Point2D point, double margin) {
        point.setLocation(Math.max(margin, Math.min(field.getWidth() - margin, point.getX())),
                          Math.max(margin, Math.min(field.getHeight() - margin, point.getY())));
    }

    public static double bulletVelocity(double power) {
        return 20 - 3 * power;
    }

    public static int sign(double v) {
        if (v < 0) {
            return -1;
        }
        else if (v > 0) {
            return 1;
        }
        else {
            return 0;
        }
    }

    public static void toLocation(double angle, double length, Point2D sourceLocation, Point2D targetLocation) {
        targetLocation.setLocation(sourceLocation.getX() + Math.sin(angle) * length,
            sourceLocation.getY() + Math.cos(angle) * length);
    }

    public static double absoluteBearing(Point2D source, Point2D target) {
        return Math.atan2(target.getX() - source.getX(), target.getY() - source.getY());
    }

    public static double maxBearing(double power) {
        return Math.abs(Math.asin(MAX_VELOCITY / (bulletVelocity(power))));
    }

    public static double normalRelativeAngle(double angle) {
	return Utils.normalRelativeAngle(angle);
    }
}
