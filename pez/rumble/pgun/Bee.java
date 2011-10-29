package pez.rumble.pgun;
import pez.rumble.utils.*;
import robocode.*;
import robocode.util.Utils;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.awt.geom.*;

// Bee, a gun by PEZ. For CassiusClay - Sting like a bee!
// http://robowiki.net/?CassiusClay
//
// This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
// http://robowiki.net/?RWPCL
// (Basically it means you must keep the code public.)
//
// $Id: Bee.java,v 1.19 2004/09/22 21:16:25 peter Exp $

public class Bee {
    public static boolean isTC = false; // TargetingChallenge

    static final double WALL_MARGIN = 18;
    static final double MAX_BULLET_POWER = 3.0;
    static final double BULLET_POWER = 1.9;

    static double roundNum;
    static Rectangle2D fieldRectangle;

    static String enemyName = "";

    Point2D enemyLocation = new Point2D.Double();
    double lastVelocity;
    double timeSinceAccel;
    double timeSinceDeccel;
    double lastBearingDirection;
    double distance;
    long lastScanTime;
    AdvancedRobot robot;
    RobotPredictor robotPredictor;

    public Bee(AdvancedRobot robot, RobotPredictor robotPredictor) {
	this.robot = robot;
	this.robotPredictor = robotPredictor;
	fieldRectangle = PUtils.fieldRectangle(robot, WALL_MARGIN);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
	if (enemyName == "") {
	    enemyName = e.getName();
	}
	if (lastScanTime == 0) {
	    GunWave.initRound();
	    System.out.println("range hits given: " + (int)GunWave.rangeHits + " (average / round: " + java.text.NumberFormat.getNumberInstance().format(GunWave.hitRate()) + ")");
	    System.out.println(GunWave.guessors.toString());
	    System.out.println("Using: " + GunWave.currentGuessor.echoStats());
	    roundNum++;
	}

	if (robot.getTime() > lastScanTime) {
	    GunWave wave = new GunWave(robot);
	    Point2D robotLocation = new Point2D.Double(robot.getX(), robot.getY());
	    double bearing = robot.getHeadingRadians() + e.getBearingRadians();
	    distance = e.getDistance();
	    wave.distance = distance;
	    enemyLocation.setLocation(PUtils.project(robotLocation, bearing, distance));
	    double bulletPower = bulletPower(distance, e.getEnergy(), robot.getEnergy());
	    wave.bulletPower = bulletPower;
	    double lateralVelocity = e.getVelocity() * Math.sin(e.getHeadingRadians() - bearing);
	    wave.absV = Math.abs(e.getVelocity());
	    wave.absLatV = Math.abs(lateralVelocity);
	    double acceleration = wave.absV - Math.abs(lastVelocity);

	    timeSinceAccel++;
	    timeSinceDeccel++;
	    if (acceleration > 0.5) {
		timeSinceAccel = 0;
	    }
	    if (acceleration < -0.5) {
		timeSinceDeccel = 0;
	    }
	    wave.timeSinceVChange = Math.min(timeSinceAccel, timeSinceDeccel);
	    wave.timeSinceAccel = timeSinceAccel;
	    wave.timeSinceDeccel = timeSinceDeccel;

	    wave.setStartTime(robot.getTime());
	    wave.setBulletVelocity(PUtils.bulletVelocity(bulletPower));
	    wave.setGunLocation(robotLocation);
	    wave.setStartBearing(bearing);
	    wave.setTargetLocation(enemyLocation);
	    if (wave.absV > 0) {
		lastBearingDirection = wave.maxEscapeAngle() * PUtils.sign(lateralVelocity);
	    }
	    double orbitDirection = lastBearingDirection / (double)GunWave.MIDDLE_BIN;
	    wave.setOrbitDirection(orbitDirection);

	    wave.wallDistance = wave.wallDistance(1, fieldRectangle);
	    wave.reverseWallDistance = wave.wallDistance(-1, fieldRectangle);

	    wave.setSegmentation();

	    if (robot.getOthers() > 0 && lastBearingDirection != 0) {
		GunWave.waves.add(wave);
	    }
	    GunWave.updateWaves();

	    Point2D nextRobotLocation = robotPredictor.getNextLocation(robot);
	    Point2D nextEnemyLocation = PUtils.project(enemyLocation, e.getHeadingRadians(), e.getVelocity());
	    double nextBearing = PUtils.absoluteBearing(nextRobotLocation, nextEnemyLocation);
	    double guessedBearing = nextBearing + orbitDirection * (wave.mostVisited() - GunWave.MIDDLE_BIN);
	    robot.setTurnGunRightRadians(Utils.normalRelativeAngle(guessedBearing - robot.getGunHeadingRadians()));
	    if (isTC || (robot.getEnergy() >= 0.3 || e.getEnergy() < robot.getEnergy() / 5 || distance < 120)) {
		if (Math.abs(robot.getGunTurnRemainingRadians()) < PUtils.botWidthAngle(distance) / 2 && robot.setFireBullet(bulletPower) != null) {
		    wave.weight = 5;
		    GunWave.bullets.add(wave);
		    wave.currentGuessor().registerFire();
		}
	    }

	    lastVelocity = e.getVelocity();
	    lastScanTime = robot.getTime();
	}
    }

    public void onBulletHit(BulletHitEvent e) {
	if (distance > 150 && e.getBullet().getPower() > 1.2) {
	    GunWave.rangeHits++;
	}
	Bullet b = e.getBullet();
	GunWave wave = (GunWave)Wave.findClosest(GunWave.bullets, new Point2D.Double(b.getX(), b.getY()), b.getVelocity());
	if (wave != null) {
	    wave.currentGuessor().registerHit(b.getPower(), distance);
	}
    }

    public void roundOver() {
	if (PUtils.isLastRound(robot)) {
	    if (!isTC) {
		GunWave.saveStats();
	    }
	    System.out.println("Bzzz bzzz. Over and out!");
	}
    }

    double bulletPower(double distance, double eEnergy, double rEnergy) {
	double wantedBulletPower = (isTC || distance < 130) ? MAX_BULLET_POWER : BULLET_POWER;
	double bulletPower = wantedBulletPower;
	if (!isTC) {
	    bulletPower = Math.min(Math.min(eEnergy / 4, rEnergy / (distance >= 130 ? 5 : 1)), wantedBulletPower);
	}

	return bulletPower;
    }
}

class GunWave extends Wave {
    static final int BINS = 47;
    static final int MIDDLE_BIN = (BINS - 1) / 2;

    static List waves;
    static List bullets;
    static double rangeHits;
    
    static AccumulatingGuessor accumulator;
    static ReplacingGuessor replacor;
    static List guessors;
    static Guessor currentGuessor;
    Map guesses = new HashMap();

    double bulletPower;
    double timeSinceAccel;
    double timeSinceDeccel;
    double distance;
    double absLatV;
    double absV;
    double timeSinceVChange;
    double wallDistance;
    double reverseWallDistance;
    int accelSegment;
    int distanceSegment;
    int distanceSegmentFaster;
    int velocitySegment;
    int velocitySegmentFaster;
    int vChangeSegment;
    int vChangeSegmentFaster;
    int reverseWallSegment;
    int wallSegment;
    int wallSegmentFaster;
    double weight = 1;

    static void initRound() {
	waves = new ArrayList();
	bullets = new ArrayList();
	if (guessors == null) {
	    readStats();
	}
	for (int i = 0, n = guessors.size(); i < n; i++) {
	    ((Guessor)guessors.get(i)).rounds++;
	}
	if (false && accumulator.rounds < 8) {
	    currentGuessor = (Guessor)guessors.get((int)(Bee.roundNum) % guessors.size());
	}
	else {
	    if (Bee.isTC && hitRate() > 6.5 || !Bee.isTC && hitRate() > 5.5) {
		currentGuessor = accumulator;
	    }
	    else {
		Collections.sort(guessors);
		currentGuessor = (Guessor)guessors.get(0);
	    }
	}
	currentGuessor.roundsUsed++;
    }

    public GunWave(AdvancedRobot robot) {
	init(robot, BINS);
    }

    void setSegmentation() {
	accelSegment = timeSinceAccel == 0 ? 0 : timeSinceDeccel == 0 ? 1 : 2;
	distanceSegment = PUtils.index(Guessor.DISTANCE_SLICES, distance);
	distanceSegmentFaster = PUtils.index(Guessor.DISTANCE_SLICES_FASTER, distance);
	velocitySegment =  PUtils.index(Guessor.VELOCITY_SLICES, absLatV);
	velocitySegmentFaster =  PUtils.index(Guessor.VELOCITY_SLICES_FASTER, absV);
	vChangeSegment = PUtils.index(Guessor.TIMER_SLICES, timeSinceVChange / travelTime());
	vChangeSegmentFaster = PUtils.index(Guessor.TIMER_SLICES_FASTER, timeSinceVChange / travelTime());
	wallSegment = PUtils.index(Guessor.WALL_SLICES, wallDistance);
	wallSegmentFaster = PUtils.index(Guessor.WALL_SLICES_FASTER, wallDistance);
	reverseWallSegment = PUtils.index(Guessor.WALL_SLICES_REVERSE, reverseWallDistance);
    }

    static void updateWaves() {
	List reap = new ArrayList();
	for (int i = 0, n = waves.size(); i < n; i++) {
	    GunWave wave = (GunWave)waves.get(i);
	    wave.setDistanceFromGun((robot.getTime() - wave.getStartTime()) * wave.getBulletVelocity());
	    if (wave.passed(18)) {
		if (wave.getRobot().getOthers() > 0) {
		    wave.registerVisit();
		}
		reap.add(wave);
		bullets.remove(wave);
	    }
	}
	for (int i = 0, n = reap.size(); i < n; i++) {
	    waves.remove(reap.get(i));
	}
    }

    void registerVisit() {
	for (int i = 0, n = guessors.size(); i < n; i++) {
	    Guessor gun = (Guessor)guessors.get(i);
	    gun.registerVisit(this, guesses);
	}
    }

    int mostVisited() {
	for (int i = 0, n = guessors.size(); i < n; i++) {
	    Guessor gun = (Guessor)guessors.get(i);
	    gun.guess(this);
	    guesses.put(gun, new Integer(gun.guessed()));
	}
	return currentGuessor.guessed();
    }

    Guessor currentGuessor() {
	return currentGuessor;
    }

    public double maxEscapeAngle() {
	return PUtils.maxEscapeAngle(bulletVelocity) * 1.4;
    }
    
    static double hitRate() {
	if (Bee.roundNum > 0) {
	    return rangeHits / Bee.roundNum;
	}
	return 0;
    }

    static void readStats() {
	if (!Bee.isTC) {
	    try {
		guessors = (ArrayList)(new ObjectInputStream(new GZIPInputStream(new FileInputStream(robot.getDataFile(Bee.enemyName + ".obj.gz"))))).readObject();
		accumulator = (AccumulatingGuessor)guessors.get(0);
		replacor = (ReplacingGuessor)guessors.get(1);
		System.out.println("Read Guessor data for enemy: " + Bee.enemyName + "\n\t" + GunWave.guessors.toString());
	    } catch (Exception e) {
		System.out.println("Couldn't read Guessor data for enemy: " + Bee.enemyName + "\n ( " + e.getMessage() + ")");
	    }
	}
	if (guessors == null) {
	    guessors = new ArrayList();
	    guessors.add(accumulator = new AccumulatingGuessor());
	    guessors.add(replacor = new ReplacingGuessor());
	}
    }

    static void saveStats() {
	List orderedGuessors = new ArrayList();
	orderedGuessors.add(accumulator);
	orderedGuessors.add(replacor);
	try {
	    ObjectOutputStream oos = new ObjectOutputStream(new GZIPOutputStream(new RobocodeFileOutputStream(robot.getDataFile(Bee.enemyName + ".obj.gz"))));
	    oos.writeObject(orderedGuessors);
	    oos.close();
	    System.out.println("Wrote Guessor data for enemy: " + Bee.enemyName);
	} catch (IOException e) {
	    System.out.println("Couldn't write Guessor data for enemy: " + Bee.enemyName + "\n ( " + e.getMessage() + ")");
	}
    }
}

class VisitsIndex implements Comparable {
    double visits;
    int index;

    public VisitsIndex(double v, int i) {
	visits = v * 10000;
	index = i;
    }

    public int compareTo(Object o) {
	return (int)(((VisitsIndex)o).visits - visits);
    }
}

abstract class Guessor implements Comparable, Serializable {
    static final long serialVersionUID = 3;
    static final int ACCEL_INDEXES = 3;
    static final double[] DISTANCE_SLICES = { 150, 300, 450, 600 };
    static final double[] DISTANCE_SLICES_FASTER = { 300, 500 };
    static final double[] VELOCITY_SLICES = { 1, 3, 5, 7 };
    static final double[] VELOCITY_SLICES_FASTER = { 2, 4, 6 };
    static final double[] WALL_SLICES = { 0.15, 0.35, 0.55, 0.75 };
    static final double[] WALL_SLICES_FASTER = { 0.25, 0.5, 0.75 };
    static final double[] WALL_SLICES_REVERSE = { 0.35, 0.7 };
    static final double[] TIMER_SLICES = { 0.1, 0.3, 0.7, 1.2 };
    static final double[] TIMER_SLICES_FASTER = { 0.1, 0.3, 0.7 };
    private long rBulletsFired;
    private double rRating;
    private long vBulletsFired;
    private double vRating;
    private long vvBulletsFired;
    private double vvRating;
    int roundsUsed;
    int rounds;
    transient private int guess;

    abstract void registerVisit(int index, GunWave w);
    abstract int mostVisited(GunWave w);

    void registerVisit(GunWave w, Map guesses) {
	int index = Math.max(1, w.visitingIndex());
	updateVVRating(index, ((Integer)guesses.get(this)).intValue(), w);
	if (w.weight > 2.0) {
	    updateVRating(index, ((Integer)guesses.get(this)).intValue(), w);
	}
	registerVisit(index, w);
    }

    void guess(GunWave w) {
	guess = mostVisited(w);
    }

    int guessed() {
	return guess;
    }

    void registerFire() {
	rBulletsFired++;
    }

    void registerHit(double power, double distance) {
	rRating += reward(1, distance);
    }

    double reward(double reward, double distance) {
	return reward * distance / 100;
    }

    double hitOrMiss(double diff, GunWave w) {
	return (Math.abs(diff) <= (double)w.botWidth() / 2.0) ? 1 : 0;
    }

    void updateVVRating(int index, int guess, GunWave w) {
	vvBulletsFired++;
	vvRating += reward(hitOrMiss(guess - index, w), w.distance);
    }

    void updateVRating(int index, int guess, GunWave w) {
	vBulletsFired++;
	vRating += reward(hitOrMiss(guess - index, w), w.distance);
    }

    double rRating() {
	return rRating / rBulletsFired + 1;
    }

    double vRating() {
	return vRating / vBulletsFired;
    }

    double vvRating() {
	return vvRating / vvBulletsFired;
    }

    double weighedRRating() {
	return rRating() * (1 + 75 / Math.pow(roundsUsed + 1, 3));
    }

    public int compareTo(Object o) {
	Guessor g = (Guessor)o;
	double diff = 0;
	double vvDiff = g.vvRating() - vvRating();
	double vvDiffSize = Math.abs(vvDiff) / (g.vvRating() + vvRating());
	double vDiff = g.vRating() - vRating();
	double vDiffSize = Math.abs(vDiff) / (g.vRating() + vRating());
	if (vvDiffSize > 0.05 + Math.pow(rounds, 1.5) / ((80 * rounds) + 1)) {
	    diff = vvDiff;
	}
	else if (vDiffSize > 0.01 + Math.min(0.15, Math.pow(rounds, 1.5) / ((80 * rounds) + 1)) || rBulletsFired == 0) {
	    diff = vDiff;
	}
	else {
	    diff = g.weighedRRating() - weighedRRating();
	}
	return (int)(100000 * diff);
    }

    String echoStats() {
	String name = this.getClass().getName();
	name = name.substring(name.lastIndexOf(".") + 1);
	return name + " vvr" + java.text.NumberFormat.getNumberInstance().format(vvRating()) +
	    " vr" + java.text.NumberFormat.getNumberInstance().format(vRating()) +
	    " rr" + java.text.NumberFormat.getNumberInstance().format(rRating()) +
	    " rb" + (int)rBulletsFired;
    }

    public String toString() {
	return echoStats();
    }
}

class AccumulatingGuessor extends Guessor {
    static final long serialVersionUID = 3;
    private static double[][][][][][] faster = new double[DISTANCE_SLICES_FASTER.length + 1][VELOCITY_SLICES_FASTER.length + 1][ACCEL_INDEXES][TIMER_SLICES_FASTER.length + 1][WALL_SLICES_FASTER.length + 1][GunWave.BINS];
    private static double[][][][][][][] slower = new double[DISTANCE_SLICES.length + 1][VELOCITY_SLICES.length + 1][ACCEL_INDEXES][TIMER_SLICES.length + 1][WALL_SLICES.length + 1][WALL_SLICES_REVERSE.length + 1][GunWave.BINS];



    double[][] buffers(GunWave w) {
	return new double[][] {
	    faster[w.distanceSegmentFaster][w.velocitySegmentFaster][w.accelSegment][w.vChangeSegmentFaster][w.wallSegmentFaster],
	    slower[w.distanceSegment][w.velocitySegment][w.accelSegment][w.vChangeSegment][w.wallSegment][w.reverseWallSegment]
	};
    }

    void registerVisit(int index, GunWave w) {
	double[][] buffers = buffers(w);
	for (int b = 0; b < buffers.length; b++) {
	    buffers[b][0]++;
	    for (int i = 1; i < GunWave.BINS; i++) {
		buffers[b][i] *= 1 - w.weight / 600;
	    }
	    for (int i = 1; i < GunWave.BINS; i++) {
		buffers[b][i] += (w.weight / 600) / (Math.pow(i - index, 2) + 1);
	    }
	}
    }

    int mostVisited(GunWave w) {
	int defaultIndex = GunWave.MIDDLE_BIN + GunWave.MIDDLE_BIN / 4;
	double uses = 0;
	double[][] buffers = buffers(w);
	for (int b = 0; b < buffers.length; b++) {
	    uses += buffers[b][0];
	}
	if (uses < 1) {
	    return defaultIndex;
	}
	List visitRanks = new ArrayList();
	for (int i = 1; i < GunWave.BINS; i++) {
	    double visits = 0;
	    for (int b = 0; b < buffers.length; b++) {
		visits += 1000 * buffers[b][i] / Math.max(1, buffers[b][0]);
	    }
	    visitRanks.add(new VisitsIndex(visits, i));
	}
	Collections.sort(visitRanks);
	return ((VisitsIndex)visitRanks.get(0)).index;
    }
}

class ReplacingGuessor extends Guessor {
    static final long serialVersionUID = 3;
    private static int[][][][][][] faster = new int[DISTANCE_SLICES_FASTER.length + 1][VELOCITY_SLICES_FASTER.length + 1][ACCEL_INDEXES][TIMER_SLICES_FASTER.length + 1][WALL_SLICES_FASTER.length + 1][2];
    private static int[][][][][][][] slower = new int[DISTANCE_SLICES.length + 1][VELOCITY_SLICES.length + 1][ACCEL_INDEXES][TIMER_SLICES.length + 1][WALL_SLICES.length + 1][WALL_SLICES_REVERSE.length + 1][2];

    int[][] buffers(GunWave w) {
	return new int[][] { faster[w.distanceSegmentFaster][w.velocitySegmentFaster][w.accelSegment][w.vChangeSegmentFaster][w.wallSegmentFaster],
			     slower[w.distanceSegment][w.velocitySegment][w.accelSegment][w.vChangeSegment][w.wallSegment][w.reverseWallSegment] };
    }

    void registerVisit(int index, GunWave w) {
	int[][] buffers = buffers(w);
	for (int b = 0; b < buffers.length; b++) {
	    buffers[b][0]++;
	    buffers[b][1] = index;
	}
    }
	    
    int mostVisited(GunWave w) {
	int defaultIndex = GunWave.MIDDLE_BIN - GunWave.MIDDLE_BIN / 2 + (int)(Math.random() * (double)GunWave.MIDDLE_BIN);
	double uses = 0;
	int[][] buffers = buffers(w);
	for (int b = 0; b < buffers.length; b++) {
	    uses += buffers[b][0];
	}
	if (uses < 1) {
	    return defaultIndex;
	}
	List visitRanks = new ArrayList();
	for (int b = 0; b < buffers.length; b++) {
	    if (buffers[b][0] > 0) {
		visitRanks.add(new VisitsIndex(uses / Math.max(1, buffers[b][0]), buffers[b][1]));
	    }
	}
	Collections.sort(visitRanks);
	return ((VisitsIndex)visitRanks.get(0)).index;
    }
}
