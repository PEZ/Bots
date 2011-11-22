package pez.mini;
import robocode.*;
import robocode.util.Utils;
import java.awt.geom.*;


//This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
//http://robowiki.net/?RWPCL
//(Basically it means you must keep the code public if you base any bot on it.)

//Pugilist, by PEZ. Although a pugilist needs strong and accurate fists, he/she even more needs an evasive movement.

//Pugilist explores two major concepts:
//1. Guess factor targeting, invented by Paul Evans. http://robowiki.net/?GuessFacorTargeting
//2. Wave surfing movement, invented by ABC. http://robowiki.net/?WaveSurfing

//Many thanks to Jim, Kawigi, iiley, Jamougha, Axe, ABC, rozu, Kuuran, FnH, nano and many others who have helped me.
//Check out http://robowiki.net/?Members to get an idea about who those people are. =)

//$Id: Pugilist.java,v 1.3 2006/05/15 04:47:35 peter Exp $

public class Pugilist extends AdvancedRobot {
	static final double MAX_VELOCITY = 8;
	static final double BOT_WIDTH = 36;
	static final double BATTLE_FIELD_WIDTH = 800;
	static final double BATTLE_FIELD_HEIGHT = 600;

	static final double BLIND_STICK  = 130;
	static final double WALL_MARGIN = 20;

	static final double MAX_DISTANCE = 900;
	static final double MAX_BULLET_POWER = 3.0;
	static final double BULLET_POWER = 1.9;

	static Rectangle2D fieldRectangle = new Rectangle2D.Double(WALL_MARGIN, WALL_MARGIN,
				BATTLE_FIELD_WIDTH - WALL_MARGIN * 2, BATTLE_FIELD_HEIGHT - WALL_MARGIN * 2);
	static Point2D robotLocation = new Point2D.Double();
	static Point2D enemyLocation = new Point2D.Double();
	static double enemyDistance;
	static int distanceIndex;
	static int velocityIndex;
	static double enemyVelocity;
	static double enemyEnergy;
	static int enemyTimeSinceVChange;
	static double enemyBearingDirection;

	static double enemyFirePower = 3.0;
	static double robotVelocity;
	
	public void run() {
		setAdjustRadarForGunTurn(true);
		setAdjustGunForRobotTurn(true);

		EnemyWave.passingWave = null;

		do {
			turnRadarRightRadians(Double.POSITIVE_INFINITY); 
		} while (true);
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		Wave wave = new Wave(this);
		EnemyWave ew = new EnemyWave(this);
		ew.gunLocation = (Point2D)enemyLocation.clone();
		ew.startBearing = ew.gunBearing(robotLocation);

		double enemyDeltaEnergy = enemyEnergy - e.getEnergy();
		if (enemyDeltaEnergy > 0 && enemyDeltaEnergy <= 3.0) {
			enemyFirePower = enemyDeltaEnergy;
			addCustomEvent(ew);
		}
		enemyEnergy = e.getEnergy();
		ew.bulletVelocity = 20 - 3 * enemyFirePower;

		double direction = robotBearingDirection(ew.startBearing);
		ew.bearingDirection = Math.asin(MAX_VELOCITY / ew.bulletVelocity) * direction / (double)EnemyWave.MIDDLE_FACTOR;

		int accelIndex = 1;
		if (robotVelocity != getVelocity()) {
			accelIndex = (int)Math.signum(robotVelocity - getVelocity()) + 1;
		}
		ew.visits = EnemyWave.factors
		[distanceIndex = (int)Math.min(Wave.DISTANCE_INDEXES - 1, (enemyDistance / (MAX_DISTANCE / Wave.DISTANCE_INDEXES)))]
		 [(int)Math.abs(robotVelocity / 2)]
		  [accelIndex]
		   ;
		robotVelocity = getVelocity();
		ew.targetLocation = robotLocation;

		robotLocation.setLocation(new Point2D.Double(getX(), getY()));
		double enemyAbsoluteBearing;
		wave.startBearing = enemyAbsoluteBearing = getHeadingRadians() + e.getBearingRadians();
		enemyLocation.setLocation(project(wave.gunLocation = (Point2D)robotLocation.clone(), enemyAbsoluteBearing, enemyDistance));
		wave.targetLocation = enemyLocation;
		enemyDistance = e.getDistance();

		ew.advance(2);

		// <gun>
		if (enemyVelocity != (enemyVelocity = e.getVelocity())) {
			enemyTimeSinceVChange = 0;
		}

		//double bulletPower = MAX_BULLET_POWER; // TargetingChallenge
		double bulletPower;
		wave.bulletVelocity = 20 - 3 * (bulletPower = Math.min(enemyEnergy / 4, distanceIndex > 0 ? Math.min(BULLET_POWER, enemyFirePower) : MAX_BULLET_POWER));

		if (enemyVelocity != 0) {
			enemyBearingDirection = 0.7 * Math.signum(enemyVelocity * Math.sin(e.getHeadingRadians() - enemyAbsoluteBearing));
		}
		wave.bearingDirection = enemyBearingDirection / (double)Wave.MIDDLE_FACTOR;

		wave.visits = Wave.factors[distanceIndex]
		                           [velocityIndex]
		                            [velocityIndex = (int)Math.abs(enemyVelocity / 2)]
		                             [(int)minMax(Math.pow(enemyTimeSinceVChange++, 0.45) - 1, 0, Wave.VCHANGE_TIME_INDEXES - 1)]
		                              [wallIndex(wave)];

		setTurnGunRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getGunHeadingRadians() +
				wave.bearingDirection * (wave.mostVisited() - Wave.MIDDLE_FACTOR)));

		addCustomEvent(wave);
		if (getEnergy() >= BULLET_POWER && Math.abs(getGunTurnRemainingRadians()) < Math.atan2(BOT_WIDTH / 2, enemyDistance)) {
			setFire(bulletPower);
		}
		// </gun>

		if (EnemyWave.dangerReverse < EnemyWave.dangerForward) {
			direction = -direction;
		}
        double angle;
        setTurnRightRadians(Math.tan(angle = 
                absoluteBearing(robotLocation, wallSmoothedDestination(robotLocation, direction)) - getHeadingRadians()));
        setAhead(Math.cos(angle) * Double.POSITIVE_INFINITY);

		setTurnRadarRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getRadarHeadingRadians()) * 2);
		EnemyWave.dangerForward = EnemyWave.dangerReverse = 0;
	}

	public void onHitByBullet(HitByBulletEvent e) {
		EnemyWave.passingWave.registerVisits();
	}

	static int wallIndex(Wave wave) {
		int wallIndex = 0;
		do {
			wallIndex++;
		} while (wallIndex < (Wave.WALL_INDEXES) &&
				fieldRectangle.contains(project(wave.gunLocation, wave.startBearing + wave.bearingDirection * (double)(wallIndex * 5.5), enemyDistance)));
		return wallIndex - 1;
	}

	static Point2D wallSmoothedDestination(Point2D location, double direction) {
		Point2D destination = new Point2D.Double();
		int tries = 2;
		int smoothing;
		do  {
		    smoothing = 0;
	        while (smoothing < 105 && !fieldRectangle.contains(destination = project(location, absoluteBearing(location, enemyLocation) -
	                direction * ((1.17 - smoothing++ / 100.0) * Math.PI / 2), BLIND_STICK)));
	        direction -= direction;
	        tries--;
		} while (smoothing > 45 && tries > 0);
		return destination;
	}

	void updateDirectionStats(EnemyWave wave) {
		EnemyWave.dangerForward += wave.danger(wave.visitingIndex(waveImpactLocation(wave, 1.0, 0)));
		EnemyWave.dangerReverse += wave.danger(wave.visitingIndex(waveImpactLocation(wave, -1.0, 5)));
	}

	Point2D waveImpactLocation(EnemyWave wave, double direction, int timeOffset) {
		Point2D impactLocation = (Point2D)robotLocation.clone();
		do {
			impactLocation = project(impactLocation, absoluteBearing(impactLocation,
					wallSmoothedDestination(impactLocation, direction * robotBearingDirection(wave.gunBearing(robotLocation)))), MAX_VELOCITY);
			timeOffset++;
		} while (wave.distanceFromTarget(impactLocation, timeOffset) > -10);
		return impactLocation;
	}

	double robotBearingDirection(double enemyBearing) {
		return Math.signum(getVelocity() * Math.sin(getHeadingRadians() - enemyBearing));
	}

	static Point2D project(Point2D sourceLocation, double angle, double length) {
		return new Point2D.Double(sourceLocation.getX() + Math.sin(angle) * length,
				sourceLocation.getY() + Math.cos(angle) * length);
	}

	static double absoluteBearing(Point2D source, Point2D target) {
		return Math.atan2(target.getX() - source.getX(), target.getY() - source.getY());
	}

	static double minMax(double v, double min, double max) {
		return Math.max(min, Math.min(max, v));
	}
	
//	public void onHitWall(HitWallEvent e) {
//	    System.out.println("Ouch!");
//	}
}

class Wave extends Condition {
	public Pugilist robot;
    static final int DISTANCE_INDEXES = 5;
	static final int VELOCITY_INDEXES = 5;
	static final int WALL_INDEXES = 4;
	static final int VCHANGE_TIME_INDEXES = 6;
	static final int FACTORS = 31;
	static final int MIDDLE_FACTOR = (FACTORS - 1) / 2;

	static double[][][][][][] factors = new double[DISTANCE_INDEXES][VELOCITY_INDEXES][VELOCITY_INDEXES][VCHANGE_TIME_INDEXES][WALL_INDEXES][FACTORS];
    static double[] fastFactors = new double[FACTORS];

	double bulletVelocity;
	Point2D gunLocation;
	Point2D targetLocation;
	double startBearing;
	double bearingDirection;
	double[] visits;
	double distanceFromGun;

	public Wave(Pugilist robot) {
	    this.robot = robot;
	}

	public boolean test() {
		advance(1);
		if (passed(-18)) {
			if (robot.getOthers() > 0) {
				registerVisits(visits, 5000);
                registerVisits(fastFactors, 5000);
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
		return (int)Pugilist.minMax(
				Math.round(((Utils.normalRelativeAngle(gunBearing(target) - startBearing)) / bearingDirection) + (FACTORS - 1) / 2), 0, FACTORS - 1);
	}

	void registerVisits(double[] buffer, double depth) {
		for (int i = 1; i < FACTORS; i++) {
			buffer[i] = rollingAvg(buffer[i], i == visitingIndex(targetLocation) ? 100 : 0, depth);
		}
	}

	double gunBearing(Point2D target) {
		return Pugilist.absoluteBearing(gunLocation, target);
	}

	double distanceFromTarget(Point2D location, int timeOffset) {
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

    double danger(int index) {
        double smoothed = 0;
        int i = 0;
        do {
            smoothed += ((double)(fastFactors[i]) + (double)visits[i] * 10000) / Math.sqrt((Math.abs(index - i) + 1.0));
            i++;
        } while (i < FACTORS);
        return smoothed / Math.abs(distanceFromTarget(targetLocation, 0)) / bulletVelocity;
    }

	static double rollingAvg(double value, double newEntry, double n) {
    	return (value * n + newEntry) / (n + 1.0);
    }
}

class EnemyWave extends Wave {
	static final int ACCEL_INDEXES = 3;
	static double[][][][] factors = new double[DISTANCE_INDEXES][VELOCITY_INDEXES][ACCEL_INDEXES][FACTORS];
    static double[] fastFactors = new double[FACTORS];
	static double dangerForward;
	static double dangerReverse;
	static EnemyWave passingWave;

	public EnemyWave(Pugilist pugilist) {
        super(pugilist);
    }

    void registerVisits() {
		registerVisits(visits, 1);
		registerVisits(fastFactors, 1);
	}

	public boolean test() {
		advance(1);
		if (passed(-30)) {
			passingWave = this;
		}
		if (passed(20)) {
			robot.removeCustomEvent(this);
		}
	    robot.updateDirectionStats(this);
		return false;
	}
}
