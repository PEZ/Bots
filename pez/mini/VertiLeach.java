package pez.mini;
import robocode.*;
import robocode.util.Utils;
import java.awt.geom.*;

// This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
// http://robowiki.net/?RWPCL
// (Basically it means you must keep the code public if you base any bot on it.)
//
// VertiLeach, by PEZ. Stays close to you and follows your vertical movements
//
// $Id: Pugilist.java,v 1.38 2004/03/19 12:32:28 peter Exp $

public class VertiLeach extends AdvancedRobot {
    static final double MAX_VELOCITY = 8;

    static final double MAX_WALL_SMOOTH_TRIES = 100;
    static final double WALL_MARGIN = 50;

    static final double DEFAULT_DISTANCE = 375;
    static final double Y_OFFSET = 125;
    static final double MAX_BULLET_POWER = 3.0;
    static final double BULLET_POWER = 1.9;

    static Rectangle2D fieldRectangle;
    static Point2D robotLocation = new Point2D.Double();
    static Point2D enemyLocation = new Point2D.Double();
    static double enemyAbsoluteBearing;
    static double enemyDistance;
    static double enemyVelocity;
    double enemyEnergy;
    static int enemyTimeSinceVChange;
    static double enemyBearingDirection = 0.73;
    static double enemyYNormal;
    static double lastEnemyYNormal;
    static double midY;

    static double enemyFirePower = 2.5;
    static int lastRobotVelocityIndex;
    static double robotVelocity;
    static int enemyHits;

    public void run() {
	setAdjustRadarForGunTurn(true);
	setAdjustGunForRobotTurn(true);

	fieldRectangle = new Rectangle2D.Double(WALL_MARGIN, WALL_MARGIN,
	    getBattleFieldWidth() - WALL_MARGIN * 2, getBattleFieldHeight() - WALL_MARGIN * 2);
	midY = getBattleFieldHeight() / 2;

	VEnemyWave.passingWave = null;

	do {
	    turnRadarRightRadians(Double.POSITIVE_INFINITY); 
	} while (true);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
	VWave wave = new VWave();
	wave.robot = this;
	VEnemyWave ew = new VEnemyWave();
	ew.robot = this;
	ew.gunLocation = new Point2D.Double(enemyLocation.getX(), enemyLocation.getY());
	ew.startBearing = ew.gunBearing(robotLocation);

	double enemyDeltaEnergy = enemyEnergy - e.getEnergy();
	if (enemyDeltaEnergy > 0 && enemyDeltaEnergy <= 3.0) {
	    enemyFirePower = enemyDeltaEnergy;
	    ew.surfable = true;
	}
	enemyEnergy = e.getEnergy();
	ew.bulletVelocity = 20 - 3 * enemyFirePower;

	double direction = robotBearingDirection(ew.startBearing);
	ew.bearingDirection = Math.asin(MAX_VELOCITY / ew.bulletVelocity) * direction / (double)VEnemyWave.MIDDLE_FACTOR;

	ew.visits = VEnemyWave.factors
	    [lastRobotVelocityIndex]
	    [lastRobotVelocityIndex = (int)Math.abs(robotVelocity / 2)]
	    ;
	robotVelocity = getVelocity();
	ew.targetLocation = robotLocation;

	robotLocation.setLocation(new Point2D.Double(getX(), getY()));
	enemyAbsoluteBearing = getHeadingRadians() + e.getBearingRadians();
	enemyLocation.setLocation(project(wave.gunLocation = new Point2D.Double(getX(), getY()), enemyAbsoluteBearing, enemyDistance));
	wave.targetLocation = enemyLocation;
	enemyDistance = e.getDistance();

	ew.advance(2);
	addCustomEvent(ew);

	// <gun>
	lastEnemyYNormal = enemyYNormal;
	double enemyY = enemyYNormal = enemyLocation.getY();
	if (enemyY > midY) {
	    enemyYNormal = midY - (enemyY - midY);
	}

	double bulletPower = Math.min(enemyEnergy / 4, enemyDistance > 150 ? BULLET_POWER : MAX_BULLET_POWER);
	wave.bulletVelocity = 20 - 3 * bulletPower;

	if (enemyVelocity != 0) {
	    enemyBearingDirection = 0.7 * sign(enemyVelocity * Math.sin(e.getHeadingRadians() - enemyAbsoluteBearing));
	}
	wave.bearingDirection = enemyBearingDirection / (double)VWave.MIDDLE_FACTOR;

	wave.startBearing = enemyAbsoluteBearing;

	wave.visits = VWave.factors[aimDirectionSegment()][aimVerticalSegment()];
	setTurnGunRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getGunHeadingRadians() +
		    wave.bearingDirection * (wave.mostVisited() - VWave.MIDDLE_FACTOR)));

	if (getEnergy() >= BULLET_POWER) {
	    if (setFireBullet(bulletPower) != null) {
		addCustomEvent(wave);
	    }
	}
	// </gun>

	if (VEnemyWave.visitsReverse < VEnemyWave.visitsForward) {
	    direction = -direction;
	}
	VEnemyWave.visitsForward = VEnemyWave.visitsReverse = 0;
	double angle;
	setAhead(Math.cos(angle = wave.gunBearing(destination(robotLocation, direction)) - getHeadingRadians()) * 100);
	setTurnRightRadians(Math.tan(angle));

	setTurnRadarRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getRadarHeadingRadians()) * 2);
    }

    public void onHitByBullet(HitByBulletEvent e) {
	VEnemyWave wave = VEnemyWave.passingWave;
	if (wave != null) {
	    wave.registerVisits(++enemyHits);
	}
    }

    private int aimVerticalSegment() {
	int segment = (int)minMax((int)(enemyYNormal / (midY / VWave.VERTICAL_INDEXES)), 0, VWave.VERTICAL_INDEXES - 1);
	return segment;
    }

    private int aimDirectionSegment() {
        double yDelta = enemyYNormal - lastEnemyYNormal;
        if (yDelta < 0) {
            return 0;
        }
        else if (yDelta > 0) {
            return 2;
        }
        return 1;
    }

    private Point2D destination(Point2D location, double direction) {
	Point2D destination = new Point2D.Double();
	double side = 1;
	for (int i = 0; i < 2; i++) {
	    double X = enemyLocation.getX() + side * sign(location.getX() - enemyLocation.getX()) * DEFAULT_DISTANCE;
	    destination = new Point2D.Double(Math.max(WALL_MARGIN, Math.min(getBattleFieldWidth() - WALL_MARGIN, X)),
		Math.max(WALL_MARGIN, Math.min(getBattleFieldHeight() - WALL_MARGIN, enemyLocation.getY() + direction * Y_OFFSET)));
	    if (Math.abs(X - destination.getX()) < 180) {
		break;
	    }
	    else {
		side = -side;
	    }
	}
	return destination;
    }

    void updateDirectionStats(VEnemyWave wave) {
	VEnemyWave.visitsReverse += wave.smoothedVisits(waveImpactLocation(wave, -1.0, 5));
	VEnemyWave.visitsForward += wave.smoothedVisits(waveImpactLocation(wave, 1.0, 0));
    }

    Point2D waveImpactLocation(VEnemyWave wave, double direction, double timeOffset) {
	Point2D impactLocation = new Point2D.Double(getX(), getY());
	double time = timeOffset;
	do {
	    impactLocation = project(impactLocation, absoluteBearing(impactLocation,
		destination(impactLocation, direction * robotBearingDirection(wave.gunBearing(robotLocation)))), MAX_VELOCITY);
	    time++;
	} while (wave.distance(impactLocation, (int)time) > 18);
	return impactLocation;
    }

    double robotBearingDirection(double enemyBearing) {
	return sign(getVelocity() * Math.sin(getHeadingRadians() - enemyBearing));
    }

    static Point2D project(Point2D sourceLocation, double angle, double length) {
	return new Point2D.Double(sourceLocation.getX() + Math.sin(angle) * length,
		sourceLocation.getY() + Math.cos(angle) * length);
    }

    static double absoluteBearing(Point2D source, Point2D target) {
	return Math.atan2(target.getX() - source.getX(), target.getY() - source.getY());
    }

    static int sign(double v) {
        return v < 0 ? -1 : 1;
    }

    static double minMax(double v, double min, double max) {
	return Math.max(min, Math.min(max, v));
    }
}

class VWave extends Condition {
    static final int DIRECTION_INDEXES = 3;
    static final int VERTICAL_INDEXES = 6;
    static final int FACTORS = 23;
    static final int DISTANCE_INDEXES = 5;
    static final int VELOCITY_INDEXES = 5;
    static final int WALL_INDEXES = 4;
    static final int VCHANGE_TIME_INDEXES = 6;
    static final int MIDDLE_FACTOR = (FACTORS - 1) / 2;

    static int[][][] factors = new int[DIRECTION_INDEXES][VERTICAL_INDEXES][FACTORS];

    static int[] fastVisits = new int[FACTORS];

    VertiLeach robot;
    double bulletVelocity;
    Point2D gunLocation;
    Point2D targetLocation;
    double startBearing;
    double bearingDirection;
    int[] visits;
    double distanceFromGun;

    public boolean test() {
	advance(1);
	if (passed(-18)) {
	    if (robot.getOthers() > 0) {
		registerVisits(1);
	    }
	    robot.removeCustomEvent(this);
	}
	return false;
    }

    public boolean passed(double distanceOffset) {
	return distanceFromGun > gunLocation.distance(targetLocation) + distanceOffset;
    }

    void advance(int ticks) {
	distanceFromGun += ticks * bulletVelocity;
    }

    int visitingIndex(Point2D target) {
	return (int)VertiLeach.minMax(
	    Math.round(((Utils.normalRelativeAngle(gunBearing(target) - startBearing)) / bearingDirection) + (FACTORS - 1) / 2), 0, FACTORS - 1);
    }

    void registerVisits(int count) {
	int index = visitingIndex(targetLocation);
	visits[index] += count;
	fastVisits[index] += count;
    }

    double gunBearing(Point2D target) {
	return VertiLeach.absoluteBearing(gunLocation, target);
    }

    double distance(Point2D location, int timeOffset) {
	return gunLocation.distance(location) - distanceFromGun - (double)timeOffset * bulletVelocity;
    }

    int mostVisited() {
	int mostVisited = MIDDLE_FACTOR, i = FACTORS - 1;
	do  {
	    if (visits[--i] > visits[mostVisited]) {
		mostVisited = i;
	    }
	} while (i > 0);
	return mostVisited;
    }
}

class VEnemyWave extends VWave {
    static int[][][] factors = new int[VELOCITY_INDEXES][VELOCITY_INDEXES][FACTORS];
    static double visitsForward;
    static double visitsReverse;
    static VEnemyWave passingWave;
    boolean surfable;

    public boolean test() {
	advance(1);
	if (passed(-18)) {
	    surfable = false;
	    passingWave = this;
	}
	if (passed(18)) {
	    robot.removeCustomEvent(this);
	}
	if (surfable) {
	    robot.updateDirectionStats(this);
	}
	return false;
    }

    double smoothedVisits(Point2D destination) {
	return smoothedVisits(visitingIndex(destination));
    }

    double smoothedVisits(int index) {
	double smoothed = 0;
	int i = 1;
	do {
	    smoothed += ((double)(fastVisits[i] / (double)(VELOCITY_INDEXES * VELOCITY_INDEXES)) +
		(double)visits[i]) / Math.sqrt((double)(Math.abs(index - i) + 1.0));
	    i++;
	} while (i < FACTORS);
	return smoothed / Math.sqrt(distance(targetLocation, 0) / bulletVelocity);
    }
}
