package pez.clean;
import pez.clean.pgun.*;
import pez.clean.pmove.*;
import robocode.*;
import robocode.util.Utils;
import java.awt.geom.*;
import java.awt.Color;
import java.util.zip.*;
import java.io.*;

// Swiffer - the cleaning bot
// This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
// http://robowiki.net/?RWPCL
//
// Think of Swiffer as a vacuum cleaner. It's as clean as one as well. The framework
// code was born some late nights and the rest is just vacuumed from my other
// bots. That means, no clean code here. I jus cram in stuff and see what
// works or not.
//
// $Id: Swiffer.java,v 1.5 2003/12/13 19:37:01 peter Exp $

public class Swiffer extends Bot {
}

abstract class Bot extends AdvancedRobot {
    static final double         MAX_VELOCITY              = 8;
    static final double         MAX_DISTANCE              = 700;
    static final double         WALL_SEGMENT_DISTANCE     = 90;
    static final int            ACCEL_INDEXES             = 3;
    static final int            WALL_INDEXES              = 2;
    static final int            DISTANCE_INDEXES          = 6;
    static final int            MOVETIME_INDEXES          = 9;
    static final int            AIM_FACTORS               = 33;
    static final double         DEFAULT_BULLET_POWER      = 2.1;
    static final int            ASCII_OFFSET              = 32;

    static String enemyName = "";
    static Rectangle2D.Double field;

    Point2D robotLocation = new Point2D.Double();
    Point2D lastRobotLocation = new Point2D.Double();
    Point2D lastEnemyLocation = new Point2D.Double();
    Point2D enemyLocation = new Point2D.Double();
    double robotEnergy;
    double enemyEnergy;
    double enemyDistance;
    double enemyHeading;
    double enemyAbsoluteBearing;
    double enemyFirePower;
    double enemyMaxEscapeAngle;
    double enemyLastDeltaBearing;
    double enemyDeltaBearing;
    int enemyMoveTime = 5;
    int timeSinceEnemyFired = 1;
    boolean enemyHasFired = false;
    double bulletPower = 3;
    int bulletTravelTime = 30;
    long time;
    PugilistDance pugilistDance;
    PugilistSting pugilistSting;

    private MovementManager movementManager;

    boolean isTC = false;

    public void run() {
	pugilistDance = new PugilistDance(this); // Dance like a butterfly!
	pugilistSting = new PugilistSting(this); // Sting like a bee!

        setColors(Color.blue.darker().darker().darker(), Color.black, Color.black);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        field = new Rectangle2D.Double(18, 18, getBattleFieldWidth() - 36, getBattleFieldHeight() - 36);

	movementManager = new MovementManager(this);

        do {
            turnRadarRightRadians(Double.POSITIVE_INFINITY); 
        } while (true);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        movementManager.score.onScannedRobot(e);
        if (enemyName == "") {
            enemyName = e.getName();
	    int p = enemyName.indexOf(" ");
	    if (p != -1) {
		enemyName = enemyName.substring(0, p);
	    }
            restoreFactors();
        }
        time = getTime();
	enemyHeading = e.getHeadingRadians();
        double enemyEnergyDelta = enemyEnergy - e.getEnergy();
        enemyEnergy = e.getEnergy();
        if (enemyEnergyDelta >= .1 && enemyEnergyDelta <= 3) {
	    enemyFirePower = enemyEnergyDelta;
	    timeSinceEnemyFired = 0;
	    movementManager.considerSwitch(0.03);
        }
	enemyHasFired = timeSinceEnemyFired == 0;
	timeSinceEnemyFired++;

	robotEnergy = getEnergy();

        lastRobotLocation.setLocation(robotLocation);
        double robotX = getX();
        double robotY = getY();
        robotLocation.setLocation(robotX, robotY);
        enemyAbsoluteBearing = getHeadingRadians() + e.getBearingRadians();
        enemyDistance = e.getDistance();
        lastEnemyLocation.setLocation(enemyLocation);
        enemyLocation.setLocation(robotX + Math.sin(enemyAbsoluteBearing) * enemyDistance, robotY + Math.cos(enemyAbsoluteBearing) * enemyDistance);
	pugilistDance.onScannedRobot(e);
	pugilistSting.onScannedRobot(e);

	if (!isTC) {
	    movementManager.doTurn(e);
	}

        setTurnRadarRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getRadarHeadingRadians()) * 2);
    }

    public void onWin(WinEvent e) {
        movementManager.score.onWin(e);
	movementManager.evaluateCurrentMovement();
	saveFactors();
    }

    public void onDeath(DeathEvent e) {
        movementManager.score.onDeath(e);
	movementManager.evaluateCurrentMovement();
	saveFactors();
    }

    public void onBulletHit(BulletHitEvent e) {
	movementManager.score.onBulletHit(e);
    }

    public void onHitByBullet(HitByBulletEvent e) {
        movementManager.score.onHitByBullet(e);
	pugilistDance.onHitByBullet(e);
    }

    void goTo(Point2D destination) {
        double angle = Utils.normalRelativeAngle(absoluteBearing(robotLocation, destination) - getHeadingRadians());
	double turnAngle = Math.atan(Math.tan(angle));
        int direction = angle == turnAngle ? 1 : - 1;
        setTurnRightRadians(turnAngle);
        setAhead(robotLocation.distance(destination) * direction);
    }

    boolean shouldRam() {
        boolean result = false;
        if (timeSinceEnemyFired > 50 && getOthers() == 1) {
            if (enemyEnergy <= 0.5 && robotEnergy > enemyEnergy * 8 || enemyEnergy == 0.0) {
                result = true;
            }
        }
        return result;
    }

    static void translateInsideField(Rectangle2D field, Point2D point, double margin) {
        point.setLocation(Math.max(margin, Math.min(field.getWidth() - margin, point.getX())),
                          Math.max(margin, Math.min(field.getHeight() - margin, point.getY())));
    }

    static void toLocation(double angle, double length, Point2D sourceLocation, Point2D targetLocation) {
        targetLocation.setLocation(sourceLocation.getX() + Math.sin(angle) * length,
            sourceLocation.getY() + Math.cos(angle) * length);
    }

    static double absoluteBearing(Point2D source, Point2D target) {
        return Math.atan2(target.getX() - source.getX(), target.getY() - source.getY());
    }

    static double bulletVelocity(double power) {
	return 20 - 3 * power;
    }

    static double maxEscapeAngle(double bulletPower) {
	return Math.abs(Math.asin(MAX_VELOCITY / bulletVelocity(bulletPower)));
    }

    static int sign(double v) {
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

    boolean isCornered() {
        return  robotLocation.distance(getBattleFieldWidth() / 2, getBattleFieldHeight() / 2) > 0.85 *
        Point2D.distance(0, 0, getBattleFieldWidth() / 2, getBattleFieldHeight() / 2);
    }

    void restoreFactors() {
	try {
	    BufferedReader r = new BufferedReader(new FileReader(getDataFile(enemyName + ".dat")));
	    int movements = Integer.parseInt(r.readLine());
	    for (int i = 0; i < movements; i++) {
		movementManager.setStats(i, Integer.parseInt(r.readLine()));
	    }
	    out.println(enemyName + ", I kill for money. But you are my friend. So I'll kill you for nothing.");
	}
	catch (Exception e) {
	    out.println("Even though I don't know you I have decided to kill you " + enemyName + ".");
	}
    }

    void saveFactors() {
        try {
	    PrintStream w = new PrintStream(new RobocodeFileOutputStream(getDataFile(enemyName + ".dat")));
	    int n = movementManager.getNumMovements();
	    w.println(n);
	    for (int i = 0; i < n; i++) {
		w.println(movementManager.getStats(i));
	    }
	    if (w.checkError()) {
		out.println("Error writing movement stats.");
	    }
	    w.close();
        }
        catch (IOException e) {
	    out.println("Error writing movement stats: " + e);
        }
    }
}

interface Movement {
    void doMove();
    void init(Bot robot);
}

class MovementManager {
    private static Movement[] movementPalet = {   new PugilistMovement(),
						  new MarshmallowMovement()
					      };
    private static int currentMovement = 0;
    private static int[] stats = new int[movementPalet.length];
    private static int statsSum = 0;
    private static boolean hasValidStats = false;

    private Bot robot;
    public Score score;

    public MovementManager(Bot robot) {
	this.robot = robot;
	this.score = new Score(robot);
	movementPalet[currentMovement].init(robot);
    }

    void doTurn(ScannedRobotEvent e) {
	movementPalet[currentMovement].doMove();
    }

    void considerSwitch(double defaultProbability) {
	double probability = hasValidStats ? (0.008 / ((double)stats[currentMovement] / (double)statsSum)) : defaultProbability;
	if (Math.random() < probability) {
	    switchMovement((int)(Math.random() * movementPalet.length));
	}
    }

    void switchMovement(int movement) {
	evaluateCurrentMovement();
	this.score = new Score(robot);
	currentMovement = movement;
	movementPalet[currentMovement].init(robot);
    }

    void evaluateCurrentMovement() {
	if (!hasValidStats) {
	    stats[currentMovement] += score.getScore(0) - score.getScore(1);
	}
    }

    int getNumMovements() {
	return movementPalet.length;
    }

    void normalizeStats() {
	int min = Integer.MAX_VALUE;
	statsSum = 0;
	for (int i = 0; i < stats.length; i++) {
	    if (stats[i] < min) {
		min = stats[i];
	    }
	}
	for (int i = 0; i < stats.length; i++) {
	    stats[i] -= min - 1;
	    statsSum += stats[i];
	}
    }

    int getStats(int movement) {
	return stats[movement];
    }

    void setStats(int movement, int value) {
	stats[movement] = value;
	normalizeStats();
	hasValidStats = true;
    }
}

class PugilistMovement implements Movement {
    Bot robot;
    public void init(Bot robot) {
	this.robot = robot;
    }

    public void doMove() {
	robot.pugilistDance.doMove();
    }
}

class MarshmallowMovement implements Movement {
    private static final double WALL_MARGIN = 38;
    private static final double DEFAULT_DISTANCE = 475;
    private Bot robot;
    private double accumulatedAngle;
    private double velocity = Bot.MAX_VELOCITY;
    private double velocityChangeFactor = 0.12;
    private double velocityMaxFactor = 64;
    private Point2D destination;
    private Point2D lastRobotLocation = new Point2D.Double();
    private Point2D lastEnemyLocation = new Point2D.Double();
    private RoundRectangle2D boundaryRectangle;
    private Rectangle2D fluffedFieldRectangle;
    
    public void init(Bot robot) {
	this.robot = robot;
        boundaryRectangle = new RoundRectangle2D.Double(WALL_MARGIN * 2, WALL_MARGIN * 2,
	    robot.getBattleFieldWidth() - WALL_MARGIN * 4, robot.getBattleFieldHeight() - WALL_MARGIN * 4, 75, 75);
	fluffedFieldRectangle = new Rectangle2D.Double(-145, -145, robot.getBattleFieldWidth() + 145, robot.getBattleFieldHeight() + 145);
	saveLocations();
	destination = null;
    }

    public void doMove() {
        if (destination == null || Math.abs(robot.getDistanceRemaining()) < 20) {
	    destination = new Point2D.Double();
            double maxRelativeAngle = Bot.maxEscapeAngle(robot.enemyFirePower);
            double deltaAngle = Bot.absoluteBearing(lastEnemyLocation, robot.robotLocation) -
                Bot.absoluteBearing(lastEnemyLocation, lastRobotLocation);
            if (Math.abs(deltaAngle) > maxRelativeAngle) {
                deltaAngle = Bot.sign(deltaAngle) * Math.abs(maxRelativeAngle) / 2;
            }
            accumulatedAngle += deltaAngle;
            double distanceFactor = distanceFactor(robot.enemyDistance, robot.enemyFirePower);
            double relativeAngle = distanceFactor * (maxRelativeAngle * 2 * Math.random() - maxRelativeAngle);
            double guessFactor = accumulatedAngle / maxRelativeAngle;
            if (Math.abs(guessFactor) > 1.00) {
                relativeAngle *= -1.7 * Bot.sign(accumulatedAngle) * Math.abs(relativeAngle);
		accumulatedAngle = 0;
            }
            double distanceExtra = distanceExtra() * Math.abs(Math.toDegrees(relativeAngle));
	    double robotBearing = Utils.normalRelativeAngle(robot.enemyAbsoluteBearing + Math.PI);
	    double wantedDistance = robot.enemyDistance + distanceExtra;
            Bot.toLocation(robotBearing + relativeAngle,
                wantedDistance, robot.enemyLocation, destination);
            if (!fluffedFieldRectangle.contains(destination)) {
                Bot.toLocation(robotBearing - relativeAngle,
                    wantedDistance, robot.enemyLocation, destination);
            }
            robot.translateInsideField(Bot.field, destination, WALL_MARGIN);
            while (!robot.shouldRam() && robot.enemyLocation.distance(destination) <
                    robot.enemyLocation.distance(robot.robotLocation) - robot.enemyLocation.distance(robot.robotLocation) / 5) {
                Bot.toLocation(Bot.absoluteBearing(robot.robotLocation, destination),
                    robot.robotLocation.distance(destination) / 2, robot.robotLocation, destination);
            }
	    saveLocations();
        }
        considerNewVelocity();
	robot.goTo(destination); 
    }

    private void saveLocations() {
	lastRobotLocation.setLocation(robot.robotLocation);
	lastEnemyLocation.setLocation(robot.enemyLocation);
    }

    private double distanceFactor(double distance, double bulletPower) {
        double bulletTravelTime = distance / Bot.bulletVelocity(bulletPower);

        if (bulletPower > 2.5) {
            return Math.max(1, 0.33 + bulletTravelTime / 87);
        }
        else {
            return 0.738 + bulletTravelTime / 685;
        }
    }

    private double distanceExtra() {
        double extra = 3;
        if (robot.shouldRam()) {
            extra = -7;
        }
        else if (!boundaryRectangle.contains(robot.robotLocation)) {
            extra = -1;
        }
	else if (robot.enemyDistance < 200) {
            extra = 12;
        }
        return extra;
    }

    private void considerNewVelocity() {
	if (robot.enemyDistance <= 200) {
	    velocity = Bot.MAX_VELOCITY;
	}
        else if (Math.random() < velocityChangeFactor) {
	    velocity = (Math.min(Bot.MAX_VELOCITY, Math.random() * velocityMaxFactor));
        }
        robot.setMaxVelocity(Math.abs(robot.getTurnRemaining()) < 40 ? velocity : 0.1);
    }
}

class TityusMovement implements Movement {
    private static final double DEFAULT_DISTANCE = 500;
    private static final double WALL_MARGIN = 25;
    private Bot robot;
    private RoundRectangle2D boundaryRectangle;
    private Flattener flattener;
    private double maxRobotVelocity;

    public void init(Bot robot) {
	this.robot = robot;
        boundaryRectangle = new RoundRectangle2D.Double(WALL_MARGIN * 2, WALL_MARGIN * 2,
	    robot.getBattleFieldWidth() - WALL_MARGIN * 4, robot.getBattleFieldHeight() - WALL_MARGIN * 4, 75, 75);
        flattener = new TityusFlattener(robot, this);
    }

    public void doMove() {
        flattener.doFlattening();
        robot.goTo(relativeDestination(0.3 * flattener.getDirection()));
        robot.setMaxVelocity(Math.abs(robot.getTurnRemaining()) < 40 ? flattener.getRobotVelocity() : 0.0);
    }

    public boolean canRam() {
        return true;
    }

    public boolean canEvade() {
        return true;
    }

    private Point2D relativeDestination(double relativeAngle) {
        Point2D destination = new Point2D.Double();
        double wantedEnemyDistance = robot.enemyDistance * distanceFactor();
        Bot.toLocation(robot.enemyAbsoluteBearing + Math.PI + relativeAngle, wantedEnemyDistance, robot.enemyLocation, destination);
        double wantedTravelDistance = robot.robotLocation.distance(destination);
        Bot.translateInsideField(Bot.field, destination, WALL_MARGIN);
        Bot.toLocation(Bot.absoluteBearing(robot.robotLocation, destination), wantedTravelDistance, robot.robotLocation, destination);
        Bot.translateInsideField(Bot.field, destination, WALL_MARGIN);

        return destination;
    }

    private double distanceFactor() {
	double fightingDistance = DEFAULT_DISTANCE;
        double distanceFactor = 1.15;
        if (robot.shouldRam()) {
            distanceFactor = 0.50;
        }
        else if (robot.enemyDistance < 150) {
            distanceFactor = 1.35;
        }
        else if (!boundaryRectangle.contains(robot.robotLocation)) {
            distanceFactor = 0.98;
        }
        else if (robot.enemyDistance > fightingDistance) {
            distanceFactor = 1.0;
        }
        return distanceFactor;
    }
}

interface Flattener {
    void doFlattening();
    int getDirection();
    double getRobotVelocity();
}

class TityusFlattener implements Flattener {
    private Bot robot;
    private Movement movement;
    private int direction = 1;
    private double nextTime = 0;
    private double robotVelocity = Bot.MAX_VELOCITY;

    TityusFlattener (Bot robot, Movement movement) {
        this.robot = robot;
        this.movement = movement;
    }

    public void doFlattening() {
	double bulletTravelTime = robot.enemyDistance / Bot.bulletVelocity(robot.enemyFirePower);
	if (robot.getTime() > nextTime) {
	    if (Math.random() < reverseFactor(bulletTravelTime)) {
		direction *= -1;
	    }
	    nextTime = robot.getTime() + bulletTravelTime * Math.random();
	}
	setRobotVelocity();
    }

    public int getDirection() {
        return direction;
    }

    public double getRobotVelocity() {
        return robotVelocity;
    }

    private double reverseFactor(double bulletTravelTime) {
	/*
	double bulletsInTheAir = Math.max(1.0, bulletTravelTime / (robot.enemyFirePower / robot.getGunCoolingRate()));
        return 1.15 / bulletsInTheAir;
	*/
	return 0.92 - 7 / bulletTravelTime;
    }

    private void setRobotVelocity() {
	if (robotVelocity < 0.1 && Math.random() < 0.45) {
	    robotVelocity = Bot.MAX_VELOCITY;
	}
        else if (Math.random() < 0.06) {
            robotVelocity = 0.0;
        }
    }
}

class VertiMovement implements Movement {
    private static final double DEFAULT_DISTANCE = 275;
    private static final double WALL_MARGIN = 50;
    private double yOffset = 100;
    private Bot robot;

    public void init(Bot robot) {
	this.robot = robot;
    }

    public void doMove() {
	Point2D robotDestination = null;
	double wantedDistance = robot.enemyDistance > DEFAULT_DISTANCE * 1.3 ? robot.enemyDistance - 2 : DEFAULT_DISTANCE;
	if (robot.enemyHasFired) {
	    double maxOffset = 175 + (wantedDistance - Math.min(wantedDistance, robot.enemyDistance));
	    yOffset = 2 * maxOffset * Math.random() - maxOffset;
	}
	double direction = 1;
	for (int i = 0; i < 2; i++) {
	    double X = robot.enemyLocation.getX() + direction * side() * (robot.enemyEnergy > 0 ? wantedDistance : 0);;
	    robotDestination = new Point2D.Double(X, robot.enemyLocation.getY() + yOffset);
	    robot.translateInsideField(robot.field, robotDestination, WALL_MARGIN);
	    if (Math.abs(X - robotDestination.getX()) < 100) {
		break;
	    }
	    else {
		direction *= -1;
	    }
	}
        robot.goTo(robotDestination);
    }

    int side() {
	return Bot.sign(robot.getX() - robot.enemyLocation.getX());
    }
}

class MirrorMovement implements Movement {
    private Bot robot;

    public void init(Bot robot) {
	this.robot = robot;
    }

    public void doMove() {
        robot.goTo(new Point2D.Double(robot.field.getCenterX() - (robot.enemyLocation.getX() - robot.field.getCenterX()),
	    robot.field.getCenterY() - (robot.enemyLocation.getY() - robot.field.getCenterY())));
    }
}

// Vuen's CalculateScore class, published at http://robowiki.net/?CalculatingScore
class Score {
    
    Robot robot;
    
    public Score(Robot robot) {
        this.robot = robot;
    }
    
    
    
    public double enemyEnergy;
    public double myEnergy;
    public String enemyName;
    
    public double[] bullet = new double[2];
    public double[] curbullet = new double[2];
    public double[] survival = new double[2];
    
    
    
    public void onScannedRobot(ScannedRobotEvent e) {
        myEnergy = robot.getEnergy();
        enemyEnergy = e.getEnergy();
        if (enemyName == null) enemyName = e.getName();
    }
    
    public void onBulletHit(BulletHitEvent e) {
        if (e.getEnergy() < 0.001) return; //ignore if enemy dead

        curbullet[0] += 4 * e.getBullet().getPower() + 2 * Math.max(e.getBullet().getPower() - 1, 0);
    }
    
    public void onHitByBullet(HitByBulletEvent e) {
        if (e.getPower() * 4 + Math.max(0, e.getPower() - 1) * 2 > myEnergy) return; //ignore if self dead
            //this works regardless of order of hitbybullet and scan

        curbullet[1] += 4 * e.getBullet().getPower() + 2 * Math.max(e.getBullet().getPower() - 1, 0);
    }

    public void onWin(WinEvent e) {
        survival[0] += 60;
        
        curbullet[0] += enemyEnergy;
        
        bullet[0] += curbullet[0] * 1.2;
        bullet[1] += curbullet[1];
        
        curbullet[0] = 0; curbullet[1] = 0;
    }
    
    public void onDeath(DeathEvent e) {
        survival[1] += 60;
        
        curbullet[1] += myEnergy;
        
        bullet[0] += curbullet[0];
        bullet[1] += curbullet[1] * 1.2;
        
        curbullet[0] = 0; curbullet[1] = 0;
    }
    
    
    
    /** returns the score of the requested robot: 0=self, 1=enemy */
    public int getScore(int id) {
        return (int)Math.round(bullet[id] + curbullet[id] + survival[id]);
    }
    
    /** prints the scorecard to the console */
    public void printScore() {
        if (enemyName == null) return;

        System.out.println("  ***********SCORECARD***********");
        System.out.print("  ");
        for (int i = 0; i < Math.max(robot.getName().length(), enemyName.length()); i++) System.out.print(" ");
        System.out.println(" Total Survival Bullet");
        
        String p0 = "  " + robot.getName();
        String p1 = "  " + enemyName;
        
        String pTemp = " " + Math.round(bullet[0] + survival[0] + curbullet[0]);
        for (int i = robot.getName().length(); i < Math.max(robot.getName().length(), enemyName.length()) + 7 - pTemp.length(); i++) p0 += " ";
        
        pTemp = (" " + Math.round(bullet[1] + survival[1] + curbullet[1]));
        for (int i = enemyName.length(); i < Math.max(robot.getName().length(), enemyName.length()) + 7 - pTemp.length(); i++) p1 += " ";
        
        p0 += Math.round(bullet[0] + survival[0] + curbullet[0]) + "  ";
        p1 += Math.round(bullet[1] + survival[1] + curbullet[1]) + "  ";
        pTemp = (" " + Math.round(survival[0]));
        for (int i = 0; i < 8 - pTemp.length(); i++) p0 += " ";
        pTemp = (" " + Math.round(survival[1]));
        for (int i = 0; i < 8 - pTemp.length(); i++) p1 += " ";
        
        p0 += Math.round(survival[0]) + "  ";
        p1 += Math.round(survival[1]) + "  ";
        pTemp = (" " + Math.round(bullet[0] + curbullet[0]));
        for (int i = 0; i < 6 - pTemp.length(); i++) p0 += " ";

        pTemp = (" " + Math.round(bullet[1] + curbullet[1]));
        for (int i = 0; i < 6 - pTemp.length(); i++) p1 += " ";
        
        p0 += Math.round(bullet[0] + curbullet[0]);
        p1 += Math.round(bullet[1] + curbullet[1]);
        
        if (bullet[0] + survival[0] + curbullet[0] >= bullet[1] + survival[1] + curbullet[1]) {
            System.out.println(p0); System.out.println(p1);
        } else {
            System.out.println(p1); System.out.println(p0);
        }
    }
}
