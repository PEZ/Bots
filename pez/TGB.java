package pez;
import robocode.*;
import robocode.util.Utils;
import java.awt.geom.*;
import java.awt.Color;
import java.util.*;
import java.util.zip.*;
import java.io.*;

// This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
// http://robowiki.net/?RWPCL
//
// Tityus Grapher Bot
// $Id: TGB.java,v 1.2 2004/02/02 16:13:00 peter Exp $

public class TGB extends AdvancedRobot {
    static final double MAX_VELOCITY = 8;

    static final double WALL_MARGIN = 25;

    static final double MAX_DISTANCE = 800;
    static final int DISTANCE_INDEXES = 5;
    static final int ACCEL_INDEXES = 3;
    static final int WALL_INDEXES = 4;
    static final int ACCEL_TIMER_INDEXES = 4;
    static final int VELOCITY_INDEXES = 3;
    static final int BULLET_POWER_INDEXES = 5;
    static final int WAVE_TYPE_INDEXES = 2;
    static final int AIM_FACTORS = 27;

    static Point2D robotLocation;
    static Point2D enemyLocation;
    static double enemyDistance;
    static double enemyVelocity;
    static double enemyEnergy;
    static double enemyAbsoluteBearing;
    static double enemyHeading;
    static double lastEnemyAbsoluteBearing;
    static double enemyFirePower = 2.2;
    static double deltaBearing;
    long accelTimer;
    static double lastDeltaBearing;
    static double[][][][][][][][] aimFactors =
	new double[DISTANCE_INDEXES][VELOCITY_INDEXES][ACCEL_INDEXES]
	    [ACCEL_TIMER_INDEXES][WALL_INDEXES][BULLET_POWER_INDEXES][WAVE_TYPE_INDEXES][AIM_FACTORS];
    static String enemyName = "";
    static double direction = 0.25;
    static int fullLeadHits;
    static double bulletPower;
    static boolean hasSavedEnemyData = true;
    double[][] currentAimFactors;

    double fightingDistance;
    public void run() {
        setColors(Color.MAGENTA, Color.MAGENTA, Color.MAGENTA);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

	fightingDistance = 150 + Math.random() * 700;
        bulletPower = 0.5 + Math.random() * 3.0;
        bulletPower = Math.min(3.0, bulletPower);

        do {
            turnRadarRightRadians(Double.POSITIVE_INFINITY); 
        } while (true);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        if (enemyName == "") {
            enemyName = e.getName();
        }
        Point2D lastRobotLocation = robotLocation;
        robotLocation = new Point2D.Double(getX(), getY());
	enemyHeading = e.getHeadingRadians();
	lastEnemyAbsoluteBearing = enemyAbsoluteBearing;
        enemyAbsoluteBearing = getHeadingRadians() + e.getBearingRadians();
        enemyDistance = e.getDistance();
        enemyLocation = vectorToLocation(enemyAbsoluteBearing, enemyDistance, robotLocation);
	if (enemyVelocity != (enemyVelocity = e.getVelocity()))
	    accelTimer = getTime();


        double enemyEnergyLost = enemyEnergy - e.getEnergy();
        enemyEnergy = e.getEnergy();
        if (enemyEnergyLost >= 0.1 && enemyEnergyLost <= 3.0) {
            enemyFirePower = enemyEnergyLost;
        }

	move(e);

	shoot(lastRobotLocation);

        setTurnRadarRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getRadarHeadingRadians()) * 2);
    }

    public void onCustomEvent(CustomEvent e) {
	Wave wave = (Wave)(e.getCondition());
	wave.wAimFactors[wave.wType][(int)Math.min(AIM_FACTORS - 1, Math.max(0, Math.round(((((wave.wBearingDirection *
	    Utils.normalRelativeAngle(absoluteBearing(wave.wGunLocation, enemyLocation) - wave.wBearing)) /
	    maxEscapeAngle(wave.wBulletPower)) + 1) / 2) * AIM_FACTORS)))]++;
    }

    static final double modifiers[] = {.6,.6,.72,.7,.6};
    static final double modifiers2[] = {3,1.3,1.3,1.1,1};
    double nextChoiceTime = 10;
    double lastReverseTime = 0;
    double circleTime = 30;
    static double circleDir = 1;
    void move(ScannedRobotEvent e) {
	Point2D newDestination;

	double distDelta = 0.01 + Math.PI/2 + (e.getDistance() > fightingDistance ? -.1 : .5);

	while (getDistanceToWalls((newDestination = projectMotion(robotLocation, enemyAbsoluteBearing + circleDir*(distDelta-=.01), 170))) < 18);
	if (getTime() > nextChoiceTime){
	    double limFactor = modifiers2[Math.min((int)(e.getDistance() / (800 / 5)), 4)];
	    circleDir = Math.random() > .5 ? 1 : -1;
	    nextChoiceTime = getTime() + enemyDistance/bulletVelocity(enemyFirePower);
	    circleTime = getTime() + limFactor*Math.random()*enemyDistance/bulletVelocity(enemyFirePower);
	}

	if (getTime() > circleTime || distDelta < Math.PI/4 || (distDelta < Math.PI/3 && enemyDistance < 400)){

	    double redFactor = modifiers[Math.min((int)(e.getDistance() / (800 / 5)), 4)];
	    circleTime = getTime() + Math.random()*redFactor*(5 + nextChoiceTime - getTime());
	    circleDir *= -1;
	    lastReverseTime = getTime();
	}


	double theta = Utils.normalRelativeAngle(absoluteBearing(robotLocation, newDestination) - getHeadingRadians());
	setAhead(Math.cos(theta)*100);
	setTurnRightRadians(Math.tan(theta));
    }

    void shoot(Point2D lastRobotLocation) {

	if (getTime() > 10) {
	    lastDeltaBearing = deltaBearing;
	    deltaBearing = Utils.normalRelativeAngle(absoluteBearing(lastRobotLocation, enemyLocation) - lastEnemyAbsoluteBearing);
	    if (Math.abs(deltaBearing) < 0.00001) {
		deltaBearing = lastDeltaBearing;
	    }
	    currentAimFactors = aimFactors[distanceIndex()][velocityIndex()][accelIndex()][accelTimerIndex(bulletPower)][wallIndex(bulletPower)][bulletPowerIndex(bulletPower)];

	    setTurnGunRightRadians(Utils.normalRelativeAngle(
			enemyAbsoluteBearing + maxEscapeAngle(bulletPower) *
			sign(deltaBearing) * (enemyEnergy > 0 ? mostVisitedFactor() : 0) - getGunHeadingRadians()));

	    if (getEnergy() > 0.3 || enemyDistance < 150 || enemyEnergy == 0) {
		Bullet bullet = setFireBullet(bulletPower);
		Wave wave = new Wave();
		wave.wType = (bullet != null ? 0 : 1);
		wave.wTime = getTime() - 1;
		wave.wBulletPower = bulletPower;
		wave.wGunLocation = robotLocation;
		wave.wAimFactors = currentAimFactors;
		wave.wBearing = enemyAbsoluteBearing;
		wave.wBearingDirection = sign(deltaBearing);
		if (enemyEnergy > 0) {
		    addCustomEvent(wave);
		}
	    }
	}
    }

    double bulletPower(int distanceIndex) {
	return 3.0 - 1.25 * ((double)distanceIndex / (double)DISTANCE_INDEXES);
    }

    RoundRectangle2D fieldRectangle(double margin) {
        return new RoundRectangle2D.Double(margin, margin,
	    getBattleFieldWidth() - margin * 2, getBattleFieldHeight() - margin * 2, 75, 75);
    }

    void goTo(Point2D destination) {
        double angle = Utils.normalRelativeAngle(absoluteBearing(robotLocation, destination) - getHeadingRadians());
	double turnAngle = Math.atan(Math.tan(angle));
        setTurnRightRadians(turnAngle);
        setAhead(robotLocation.distance(destination) * (angle == turnAngle ? 1 : -1));
	setMaxVelocity(Math.abs(getTurnRemaining()) > 30 ? 0 : MAX_VELOCITY);
    }

    double mostVisitedFactor() {
        int mostVisited = (AIM_FACTORS - 1) / 2;
        for (int i = 0; i < AIM_FACTORS; i++) {
            if (currentAimFactors[0][i] > currentAimFactors[0][mostVisited]) {
                mostVisited = i;
            }
        }
	return ((double)mostVisited / AIM_FACTORS) * 2 - 1;
    }

    int distanceIndex() {
        return Math.min((int)(enemyDistance / (MAX_DISTANCE / DISTANCE_INDEXES)), DISTANCE_INDEXES - 1);
    }

    int accelIndex() {
        int delta = (int)(20 * (Math.abs(deltaBearing) - Math.abs(lastDeltaBearing)) / (MAX_VELOCITY / enemyDistance));
        if (delta < 0) {
            return 0;
        }
        else if (delta > 0) {
            return 2;
        }
        return 1;
    }

    int bulletPowerIndex(double power) {
        return Math.min((int)(power / (3D / BULLET_POWER_INDEXES)), BULLET_POWER_INDEXES - 1);
    }

    int wallIndex(double bulletPower) {
	for (int i = 1; i < WALL_INDEXES; i++) {
	    if (!fieldRectangle(i * 18).contains(vectorToLocation(enemyAbsoluteBearing + deltaBearing * 9,
			    enemyDistance, robotLocation))) {
		return i;
	    }
	}
	return 0;
    }

    int accelTimerIndex(double bulletPower){
	double bulletTravelTime = enemyDistance / bulletVelocity(bulletPower);
	double t = (getTime() - accelTimer) / bulletTravelTime;
	if (t < .1) {
	    return 0;
	}
	if (t < .3) {
	    return 1;
	}
	if (t < 1) {
	    return 2;	
	}
	return 3;
    }

    int velocityIndex() {
	double v = Math.abs(enemyVelocity);
	if (v < 2) {
	    return 0;
	}
	if (v < 6) {
	    return 1;
	}
	return 2;
    }

    static double maxEscapeAngle(double bulletPower) {
	return Math.asin(MAX_VELOCITY / bulletVelocity(bulletPower));
    }

    static double bulletVelocity(double power) {
        return 20 - 3 * power;
    }

    static Point2D vectorToLocation(double angle, double length, Point2D sourceLocation) {
        return new Point2D.Double(sourceLocation.getX() + Math.sin(angle) * length,
            sourceLocation.getY() + Math.cos(angle) * length);
    }

    static double absoluteBearing(Point2D source, Point2D target) {
        return Math.atan2(target.getX() - source.getX(), target.getY() - source.getY());
    }

    private static int sign(double v) {
        return v > 0 ? 1 : -1;
    }

	Point2D projectMotion(Point2D loc, double heading, double distance){
		
		Point2D newLoc = new Point2D.Double();	
		newLoc.setLocation((loc.getX() + distance*Math.sin(heading)),(loc.getY() + distance*Math.cos(heading)));
		return newLoc;
		
	}

	private double getDistanceToWalls(Point2D location){
		
		return Math.min(Math.min(location.getY(), getBattleFieldHeight() - location.getY()), Math.min(location.getX() , getBattleFieldWidth() - location.getX()));
	}


    public void onWin(WinEvent e) {
        saveFactors();
    }

    public void onDeath(DeathEvent e) {
        saveFactors();
    }

    void saveFactors() {
        try {
            ZipOutputStream zipout = new ZipOutputStream(new RobocodeFileOutputStream(getDataFile(enemyName)));
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
	int wType;
	long wTime;
	double wBulletPower;
	Point2D wGunLocation;
	double wBearing;
	double wBearingDirection;
	double[][] wAimFactors;

	public boolean test() {
	    if (bulletVelocity(wBulletPower) * (getTime() - wTime) > wGunLocation.distance(enemyLocation) - 10) {
		removeCustomEvent(this);
		return true;
	    }
	    return false;
	}
    }
}
