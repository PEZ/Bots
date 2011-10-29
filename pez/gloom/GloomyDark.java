package pez.gloom;
import robocode.*;
import robocode.util.Utils;
import java.awt.geom.*;
import java.awt.Color;
import java.util.zip.*;
import java.io.*;

// GloomyDark - Fighting gloomily and darkly
// This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
// http://robowiki.net/?RWPCL
//
// $Id: GloomyDark.java,v 1.6 2003/12/20 19:18:45 peter Exp $

public class GloomyDark extends Bot {
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
    double enemyBearingDirection = 1;
    double enemyFirePower;
    double enemyMaxEscapeAngle;
    double enemyLastDeltaBearing;
    double enemyDeltaBearing;
    int enemyMoveTime = 5;
    int timeSinceEnemyFired = 1;
    boolean enemyHasFired = false;
    static int[][][][] aimFactors;
    int[] currentAimFactors;
    double bulletPower = 3;
    int bulletTravelTime = 30;
    long time;

    private MovementManager movementManager;

    boolean isTC = false;

    public void run() {
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
	    //movementManager.considerSwitch();
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
        enemyLastDeltaBearing = enemyDeltaBearing;
        enemyDeltaBearing = Utils.normalRelativeAngle(absoluteBearing(lastRobotLocation, enemyLocation) -
            absoluteBearing(lastRobotLocation, lastEnemyLocation));
	if (enemyDeltaBearing < 0) {
	    enemyBearingDirection = -1;
	}
	else if (enemyDeltaBearing > 0) {
	    enemyBearingDirection = 1;
	}

	doGun();

	if (!isTC) {
	    movementManager.doTurn();
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
	movementManager.considerSwitch();
    }

    public void onBulletHit(BulletHitEvent e) {
	movementManager.score.onBulletHit(e);
    }

    public void onHitByBullet(HitByBulletEvent e) {
        movementManager.score.onHitByBullet(e);
    }

    void doGun() {
	int aimDistanceIndex = aimDistanceIndex();
	int aimAccelIndex = aimAccelIndex();
	int aimWallIndex = aimWallIndex();
        bulletPower = Math.min(robotEnergy / 5, Math.min(enemyEnergy / 4, 3.0 - 1.5 * ((double)aimDistanceIndex / (double)DISTANCE_INDEXES)));
	bulletTravelTime = (int)Math.round(enemyDistance / bulletVelocity(bulletPower));
	if (isTC) {
	    bulletPower = Math.min(robotEnergy, 3.0);
	}
	if (aimAccelIndex == 0) {
	    enemyMoveTime = 0;
	}
	else if (aimAccelIndex == 2) {
	    enemyMoveTime = bulletTravelTime * 2;
	}
	else {
	    enemyMoveTime++;
	}
	int aimMoveTimeIndex = aimMoveTimeIndex();
        enemyMaxEscapeAngle = maxEscapeAngle(bulletPower);
        currentAimFactors = aimFactors[aimMoveTimeIndex][aimWallIndex][aimDistanceIndex];

	setTurnGunRightRadians(Utils.normalRelativeAngle(
	    enemyAbsoluteBearing + enemyMaxEscapeAngle * enemyBearingDirection * mostVisitedFactor() - getGunHeadingRadians()));

	if (shouldFire()) {
	    Bullet bullet = setFireBullet(bulletPower);
	    addCustomEvent(new Wave(20 - (3 * bulletPower)));
	}
    }

    double mostVisitedFactor() {
	return visitIndexToFactor(mostVisitedIndex());
    }

    double visitIndexToFactor(int index) {
        return (index - (AIM_FACTORS - 1D) / 2D) / ((AIM_FACTORS - 1D) / 2D);
    }

    int mostVisitedIndex() {
	int mostVisited = (AIM_FACTORS - 1) / 2;;
	for (int i = 0; i < AIM_FACTORS; i++) {
	    if (currentAimFactors[i] > currentAimFactors[mostVisited]) {
		mostVisited = i;
	    }
	}
	return mostVisited;
    }

    int aimAccelIndex() {
        int delta = (int)(Math.round(400 * (Math.abs(enemyDeltaBearing) - Math.abs(enemyLastDeltaBearing))));
        if (delta < 0) {
            return 0;
        }
        else if (delta > 0) {
            return 2;
        }
        return 1;
    }

    int aimDistanceIndex() {
        return Math.min((int)(enemyDistance / (MAX_DISTANCE / DISTANCE_INDEXES)), DISTANCE_INDEXES - 1);
    }

    int aimMoveTimeIndex() {
        return Math.min((int)Math.round(enemyMoveTime / ((bulletTravelTime * 2.5) / MOVETIME_INDEXES)), MOVETIME_INDEXES - 1);
    }

    int aimWallIndex() {
        if (field.contains(Math.sin(enemyHeading) * WALL_SEGMENT_DISTANCE + enemyLocation.getX(),
		Math.cos(enemyHeading) * WALL_SEGMENT_DISTANCE + enemyLocation.getY())) {
	    return 1;
	}
	return 0;
    }

    void goTo(Point2D destination) {
        double angle = Utils.normalRelativeAngle(absoluteBearing(robotLocation, destination) - getHeadingRadians());
	double turnAngle = Math.atan(Math.tan(angle));
        int direction = angle == turnAngle ? 1 : - 1;
        setTurnRightRadians(turnAngle);
        setAhead(robotLocation.distance(destination) * direction);
    }

    public boolean shouldFire() {
	if (isTC) {
	    return robotEnergy > 0;
	}
        boolean result = false;
        if (shouldRam()) {
            return false;
        }
        else if (enemyDistance <= 200) {
            result = true;
        }
	else if (enemyEnergy > robotEnergy * 9) {
	    result = false;
	}
        else if (robotEnergy > 0.2) {
            result = true;
        }
        return result;
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
	    String s = r.readLine();
	    if (true) {
		int s1L = (int)s.charAt(0) - ASCII_OFFSET;
		int s2L = (int)s.charAt(1) - ASCII_OFFSET;
		int s3L = (int)s.charAt(2) - ASCII_OFFSET;
		aimFactors = new int[s1L][s2L][s3L][AIM_FACTORS];
		for (int s1 = 0, p = 3; s1 < s1L; s1++) {
		    for (int s2 = 0; s2 < s2L; s2++) {
			for (int s3 = 0; s3 < s3L; s3++) {
			    aimFactors[s1][s2][s3][(int)s.charAt(p++) - ASCII_OFFSET] = 15;
			}
		    }
		}
	    }
	    else {
		aimFactors = new int[MOVETIME_INDEXES][WALL_INDEXES][DISTANCE_INDEXES][AIM_FACTORS];
	    }
	    /*
	    int movements = Integer.parseInt(r.readLine());
	    for (int i = 0; i < movements; i++) {
		movementManager.setStats(i, Integer.parseInt(r.readLine()));
	    }
	    */
	    out.println(enemyName + ", I kill for money. But you are my friend. So I'll kill you for nothing.");
	}
	catch (Exception e) {
	    aimFactors = new int[MOVETIME_INDEXES][WALL_INDEXES][DISTANCE_INDEXES][AIM_FACTORS];
	    out.println("Even though I don't know you I have decided to kill you " + enemyName + ".");
	}
    }

    void saveFactors() {
        try {
	    PrintStream w = new PrintStream(new RobocodeFileOutputStream(getDataFile(enemyName + ".dat")));
	    int s1L = aimFactors.length;
	    int s2L = aimFactors[0].length;
	    int s3L = aimFactors[0][0].length;
	    w.print((char)(ASCII_OFFSET + s1L));
	    w.print((char)(ASCII_OFFSET + s2L));
	    w.print((char)(ASCII_OFFSET + s3L));
	    for (int s1 = 0; s1 < s1L; s1++) {
		for (int s2 = 0; s2 < s2L; s2++) {
		    for (int s3 = 0; s3 < s3L; s3++) {
			currentAimFactors = aimFactors[s1][s2][s3];
			w.print((char)(ASCII_OFFSET + mostVisitedIndex()));
		    }
		}
	    }
	    w.println("");
	    int n = movementManager.getNumMovements();
	    w.println(n);
	    for (int i = 0; i < n; i++) {
		w.println(movementManager.getStats(i));
	    }
	    if (w.checkError()) {
		out.println("Error writing crib sheet.");
	    }
	    w.close();
        }
        catch (IOException e) {
	    out.println("Error writing crib sheet: " + e);
        }
    }

    class Wave extends Condition {
        private long wTime;
        private double wBulletVelocity;
        private Point2D wRLocation = new Point2D.Double();
	private double wBearing;
        private double wBearingDirection;
        private double wMaxEscapeAngle;
        int[] wAimFactors;

        public Wave(double bulletVelocity) {
            this.wTime = getTime();
            this.wBulletVelocity = bulletVelocity;
            this.wBearingDirection = enemyBearingDirection;
            this.wRLocation.setLocation(robotLocation);
            this.wBearing = enemyAbsoluteBearing;
            this.wMaxEscapeAngle = enemyMaxEscapeAngle;
            this.wAimFactors = currentAimFactors;
        }

        public boolean test() {
            if (wDistance() > wRLocation.distance(enemyLocation) - 10) {
                double bearingDiff = Utils.normalRelativeAngle(absoluteBearing(wRLocation, enemyLocation) - wBearing);
                int index = (int)Math.round(Math.max(0D, Math.min(AIM_FACTORS - 1D,
                            ((wBearingDirection * bearingDiff) / wMaxEscapeAngle) * (AIM_FACTORS - 1D) / 2D + (AIM_FACTORS - 1D) / 2D)));
		updateStats(wAimFactors, index);
                removeCustomEvent(this);
            }
            return false;
        }

	private double wDistance() {
	    return (wBulletVelocity * (time - wTime));
	}

	private void updateStats(int[] factors, int index) {
	    factors[index] += 2;
	    if (index < factors.length - 1) factors[index + 1] += 1;
	    if (index > 0) factors[index - 1] += 1;
	}
    }
}

interface Movement {
    void doTurn();
    void init(Bot robot);
}

class MovementManager {
    private static Movement[] movementPalet = {   new TityusMovement(0.89),
						  new TityusMovement(0.90),
						  new TityusMovement(0.91),
						  new TityusMovement(0.915),
						  new TityusMovement(0.92),
						  new TityusMovement(0.925)
					      };
    private static int currentMovement = (int)(Math.random() * movementPalet.length);
    private static int[] stats = new int[movementPalet.length];
    private static int statsSum = 0;
    private static boolean hasValidStats = false;
    private static double defaultProbability = 0.35;

    private Bot robot;
    public Score score;

    public MovementManager(Bot robot) {
	this.robot = robot;
	this.score = new Score(robot);
	movementPalet[currentMovement].init(robot);
    }

    void doTurn() {
	movementPalet[currentMovement].doTurn();
    }

    void considerSwitch() {
	double probability = hasValidStats ? (0.01 / ((double)stats[currentMovement] / (double)statsSum)) : defaultProbability;
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
	//hasValidStats = true;
    }
}

class TityusMovement implements Movement {
    private static final double DEFAULT_DISTANCE = 650;
    private static final double WALL_MARGIN = 25;
    private Bot robot;
    private RoundRectangle2D boundaryRectangle;
    private Flattener flattener;
    private double flattenerTuning;
    private double maxRobotVelocity;

    public TityusMovement(double flattenerTuning) {
	this.flattenerTuning = flattenerTuning;
    }

    public void init(Bot robot) {
	this.robot = robot;
        boundaryRectangle = new RoundRectangle2D.Double(WALL_MARGIN * 1.5, WALL_MARGIN * 1.5,
	    robot.getBattleFieldWidth() - WALL_MARGIN * 3, robot.getBattleFieldHeight() - WALL_MARGIN * 3, 120, 120);
        flattener = new TityusFlattener(robot, this, flattenerTuning);
    }

    public void doTurn() {
        flattener.doTurn();
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
	if (robot.robotLocation.distance(destination) < wantedTravelDistance / 3) {
	    flattener.endCurrentTravel();
	}
	else {
	    Bot.toLocation(Bot.absoluteBearing(robot.robotLocation, destination), wantedTravelDistance, robot.robotLocation, destination);
	    Bot.translateInsideField(Bot.field, destination, WALL_MARGIN);
	}

        return destination;
    }

    private double distanceFactor() {
	double fightingDistance = DEFAULT_DISTANCE;
        double distanceFactor = 1.15;
        if (robot.shouldRam()) {
            distanceFactor = 0.50;
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
    void doTurn();
    int getDirection();
    double getRobotVelocity();
    void endCurrentTravel();
}

class TityusFlattener implements Flattener {
    private Bot robot;
    private Movement movement;
    private int direction = 1;
    private double nextTime = 0;
    private double robotVelocity = Bot.MAX_VELOCITY;
    private double tuning;

    TityusFlattener (Bot robot, Movement movement, double tuning) {
        this.robot = robot;
        this.movement = movement;
	this.tuning = tuning;
    }

    public void doTurn() {
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

    public void endCurrentTravel() {
	nextTime = robot.getTime() - 1;
    }

    private double reverseFactor(double bulletTravelTime) {
	/*
	double bulletsInTheAir = Math.max(1.0, bulletTravelTime / (robot.enemyFirePower / robot.getGunCoolingRate()));
        return 1.15 / bulletsInTheAir;
	*/
	return tuning - 7 / bulletTravelTime;
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

class MirrorMovement implements Movement {
    private Bot robot;

    public void init(Bot robot) {
	this.robot = robot;
    }

    public void doTurn() {
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
