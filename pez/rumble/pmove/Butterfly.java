package pez.rumble.pmove;
import pez.rumble.utils.*;
import pez.rumble.RumbleBot;
import robocode.*;
import java.util.*;
import java.awt.Graphics2D;
import java.awt.geom.*;

//Butterfly, a movement by PEZ. For CassiusClay - Float like a butterfly!
//http://robowiki.net/?CassiusClay

//This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
//http://robowiki.net/?RWPCL
//(Basically it means you must keep the code public if you base any bot on it.)

//$Id: Butterfly.java,v 1.16 2007-02-28 06:14:58 peters Exp $


public class Butterfly {
	public static boolean isMC;

	public static boolean doGL; // GL

	static final double MAX_VELOCITY = 8;
	static final double MAX_TURN_RATE = 10;

	static final double MAX_WALL_SMOOTH_TRIES = 175;
	static final double WALL_MARGIN = 20;
	static final double DEFAULT_BLIND_MANS_STICK = 80;

	static public double wallDistance;
	static Rectangle2D fieldRectangle;
	static Point2D robotLocation = new Point2D.Double();
	static Point2D enemyLocation = new Point2D.Double();
	static double enemyAbsoluteBearing;
	static double enemyDistance;
	static int distanceIndex;
	static double enemyEnergy;
	static double enemyVelocity;
	static double enemyFirePower = 2.5;
	static int lastVelocityIndex;
	static double approachVelocity = 4;
	static double velocity;
	static int timeSinceVChange;
	static double lastForwardSmoothing;
	static double roundNum;
	static long lastScanTime;
	static long time;
	static int bulletsThisRound;

	double roundsLeft;
	RumbleBot robot;

	public Butterfly(RumbleBot robot) {
		this.robot = robot;
		MovementWave.init();
		MovementWave.reset();
		enemyEnergy = 100;
		fieldRectangle = PUtils.fieldRectangle(robot, WALL_MARGIN);
		if (roundNum > 0) {
			System.out.println("range hits taken: " + (int)MovementWave.rangeHits + " (average / round: " + PUtils.formatNumber(MovementWave.rangeHits / roundNum) + ")");
		}

		roundsLeft = robot.getNumRounds() - roundNum - 1;
		roundNum++;
		bulletsThisRound = 0;
		WaveGrapher.initDangerGraph(); // GL
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		time = robot.getTime();
		if (RumbleBot.enemyIsRammer()) {
			fieldRectangle = PUtils.fieldRectangle(robot, 70);
		}
		MovementWave wave = new MovementWave(robot, this);
		wave.startTime = robot.getTime() - 1;

		double wallDamage = 0;
		if (Math.abs(e.getVelocity()) == 0 && Math.abs(enemyVelocity) > 2.0) {
			wallDamage = Math.max(0, Math.abs(enemyVelocity) / 2 - 1);
		}
		enemyVelocity = e.getVelocity();

		wave.setGunLocation(new Point2D.Double(enemyLocation.getX(), enemyLocation.getY()));
		wave.setStartBearing(wave.gunBearing(robotLocation));

		double enemyDeltaEnergy = enemyEnergy - e.getEnergy() - wallDamage;
		if (enemyDeltaEnergy > 0 && enemyDeltaEnergy <= 3.1) {
			enemyFirePower = enemyDeltaEnergy;
			robot.enemyFired(enemyFirePower);
			wave.isSurfable = true;
			MovementWave.bullets.add(wave);
			MovementWave.surfables.add(wave);
			bulletsThisRound++;
			if (doGL) { // GL
				wave.grapher = new WaveGrapher(wave); // GL
			} // GL
		}
		enemyEnergy = e.getEnergy();
		double bulletVelocity = PUtils.bulletVelocity(enemyFirePower);
		wave.setBulletVelocity(bulletVelocity);

		double orbitDirection = robotOrbitDirection(wave.gunBearing(robotLocation));
		wave.setOrbitDirection(wave.maxEscapeAngle() * orbitDirection / (double)MovementWave.getMiddleFactor());

		approachVelocity = velocity * -Math.cos(robot.getHeadingRadians() - (enemyAbsoluteBearing + Math.PI));
		wave.approachIndex = PUtils.index(MovementWave.APPROACH_SLICES, approachVelocity);

		distanceIndex = PUtils.index(MovementWave.DISTANCE_SLICES, enemyDistance);
		wave.bulletPower = enemyFirePower;
		wave.distanceIndex = distanceIndex;
		int velocityIndex = PUtils.index(MovementWave.VELOCITY_SLICES, Math.abs(velocity));
		velocity = robot.getVelocity();
		wave.accelIndex = 0;
		if (velocityIndex != lastVelocityIndex) {
			timeSinceVChange = 0;
			wave.accelIndex = velocityIndex < lastVelocityIndex ? 1 : 2;
		}
		wave.velocityIndex = velocityIndex;
		wave.lastVelocityIndex = lastVelocityIndex;
		lastVelocityIndex = velocityIndex;

		wave.setTargetLocation(robotLocation);

		wave.vChangeIndex = PUtils.index(MovementWave.TIMER_SLICES, timeSinceVChange++ / wave.travelTime());
		wallDistance = wave.wallDistance(1, fieldRectangle);
		wave.wallIndex = PUtils.index(MovementWave.WALL_SLICES, wallDistance);
		double wallDistanceReverse = wave.wallDistance(-1, fieldRectangle);
		wave.wallIndexReverse = PUtils.index(MovementWave.WALL_SLICES_REVERSE, wallDistanceReverse);


		robotLocation.setLocation(new Point2D.Double(robot.getX(), robot.getY()));
		enemyAbsoluteBearing = robot.getHeadingRadians() + e.getBearingRadians();
		enemyLocation.setLocation(PUtils.project(robotLocation, enemyAbsoluteBearing, enemyDistance));
		enemyDistance = e.getDistance();

		MovementWave.waves.add(wave);

		move(wave, orbitDirection);

		MovementWave.reset();
		lastScanTime = robot.getTime();
	}

	public void onHitByBullet(HitByBulletEvent e) {
		Bullet b = e.getBullet();
		/*
		Hit hit = new Hit(b.getPower(), enemyDistance, robotLocation, enemyLocation);
		MovementWave wave = (MovementWave)Wave.findClosest(MovementWave.bullets, new Point2D.Double(b.getX(), b.getY()), b.getVelocity());
		if (wave != null) {
			hit.gf = wave.getGF(new Point2D.Double(b.getX(), b.getY()));
		}
		Hit.hits.add(hit);
		hit.print();
		*/
		MovementWave.hitsTaken++;
		if (b.getPower() > 1.2 && enemyDistance > 150) {
			MovementWave.rangeHits++;
		}
		MovementWave.registerHit(e.getBullet());
		enemyEnergy += 3 * e.getBullet().getPower();
	}

	public void onBulletHit(BulletHitEvent e){
		double power = e.getBullet().getPower();
		enemyEnergy -= 4 * power + Math.max(2 * power - 1, 0);
	}

	public void onBulletHitBullet(BulletHitBulletEvent e) {
		MovementWave.registerHit(e.getHitBullet());
	}

	void move(MovementWave wave, double direction) {
		MovementWave.updateWaves(robot);
		MovementWave closest = (MovementWave)Wave.findClosest(MovementWave.surfables, robotLocation);
		Point2D orbitCenter = orbitCenter(closest);
		if (closest != null) {
			updateDirectionStats(MovementWave.surfables, closest);
		}
		Move forward = wallSmoothedDestination(robotLocation, orbitCenter, direction);
		double forwardSmoothingDanger = forward.smoothingDanger();
		lastForwardSmoothing = forward.normalizedSmoothing();
		Move reverse = wallSmoothedDestination(robotLocation, orbitCenter, -direction);
		double reverseSmoothingDanger = reverse.smoothingDanger();
		if (RumbleBot.enemyIsRammer() && (!(forward.normalizedSmoothing() > 75 && reverse.normalizedSmoothing() > 75))) {
			MovementWave.dangerForward += forwardSmoothingDanger;
			MovementWave.dangerReverse += reverseSmoothingDanger;
		}
		else if (!(forward.normalizedSmoothing() > 20 && reverse.normalizedSmoothing() > 20)) {
			MovementWave.dangerForward += forwardSmoothingDanger;
			MovementWave.dangerReverse += reverseSmoothingDanger;
		}
		if (RumbleBot.enemyIsRammer() || forwardSmoothingDanger > 0 && reverseSmoothingDanger > 0) {
			MovementWave.dangerStop = MovementWave.dangerForward + MovementWave.dangerReverse;
		}
		Point2D destination = forward.location;
		double wantedVelocity = MAX_VELOCITY;
		/*
		if (MovementWave.hitsTaken == 0 && robot.getEnergy() > 25 && ((roundsLeft < 6 && enemyFirePower < 0.3) || (roundsLeft < 3 && enemyFirePower < (3.01 - roundsLeft)))) {
			if (!isMC) {
				wantedVelocity = 0;
			}
		}
		*/
		if (enemyEnergy > 0 && !RumbleBot.enemyIsRammer() && MovementWave.bullets.size() == 0) {
			if (enemyLocation.distance(reverse.location) / enemyLocation.distance(forward.location) > 1.03) {
				destination = reverse.location;
			}
		}
		else if (!RumbleBot.enemyIsRammer() && MovementWave.dangerStop < MovementWave.dangerReverse && MovementWave.dangerStop < MovementWave.dangerForward) {
			wantedVelocity = 0;
		}
		else if (MovementWave.dangerReverse < MovementWave.dangerForward) {
			destination = reverse.location;
		}
		double newHeading = PUtils.absoluteBearing(robotLocation, destination);
		double oldHeading = robot.getHeadingRadians();
		robot.setAhead(PUtils.backAsFrontDirection(newHeading, oldHeading) * 50);
		robot.setTurnRightRadians(PUtils.backAsFrontTurn(newHeading, oldHeading));
		robot.setMaxVelocity(wantedVelocity);
		if (doGL) { // GL
			WaveGrapher.drawDangerGraph(MovementWave.dangerForward, MovementWave.dangerStop, MovementWave.dangerReverse); // GL
		} // GL
	}

	static Move wallSmoothedDestination(Point2D location, Point2D orbitCenter, double direction) {
		Point2D destination = new Point2D.Double();
		destination.setLocation(location);
		double distance = enemyLocation.distance(location);
		double evasion = evasion(distance);
		double blindStick = RumbleBot.enemyIsRammer() ? PUtils.minMax(enemyDistance / 1.7, 40, DEFAULT_BLIND_MANS_STICK) : DEFAULT_BLIND_MANS_STICK;
		double smoothing = 0;
		while (!fieldRectangle.contains(destination = PUtils.project(location,
				PUtils.absoluteBearing(location, orbitCenter) - direction *
				 ((evasion - smoothing / 100) * Math.PI / 2), blindStick)) && smoothing < MAX_WALL_SMOOTH_TRIES) {
			smoothing += 5;
		}
		return new Move(destination, smoothing, evasion, distance, destination.distance(enemyLocation));
	}

	static double evasion(double distance) {
		double evasion;
		if (time < 16) {
			evasion = PUtils.minMax(distance / 700, 1.3, 5.0);
		}
		else {
			if (RumbleBot.enemyIsRammer()) {
				evasion = PUtils.minMax(150.0 / distance, 1.45, 1.65);
			}
			else if (time > 30 && bulletsThisRound == 0) {
				evasion = PUtils.minMax(300.0 / distance, 0.75, 1.5);
			}
			else if (MovementWave.isLowHitRate()) {
				evasion = PUtils.minMax(410.0 / distance, 0.95, 1.25);
			}
			else {
				evasion = PUtils.minMax((300 * Math.pow(MovementWave.hitRate(), 1.2)) / distance, 1.03, 1.3);
			}
		}
		return evasion;
	}

	void updateDirectionStats(List<MovementWave> _waves, MovementWave closest) {
		Move move = waveImpactLocation(closest, 1.0, MAX_VELOCITY);
		MovementWave.dangerForward += impactDanger(_waves, move.location);
		if (closest.grapher != null) { // GL
			closest.grapher.drawForwardDestination(move.location, closest.danger(move.location)); // GL
		} // GL
		move = waveImpactLocation(closest, -1.0, MAX_VELOCITY);
		MovementWave.dangerReverse += impactDanger(_waves, move.location);
		if (closest.grapher != null) { // GL
			closest.grapher.drawReverseDestination(move.location, closest.danger(move.location)); // GL
		} // GL
		move = waveImpactLocation(closest, 1.0, 0);
		MovementWave.dangerStop += impactDanger(_waves, move.location);
		if (closest.grapher != null) { // GL
			closest.grapher.drawStopDestination(move.location, closest.danger(move.location)); // GL
		} // GL
	}

	double impactDanger(List<MovementWave> _waves, Point2D impact) {
		double danger = 0;
		for (int i = 0, n = _waves.size(); i < n; i++) {
			danger += ((MovementWave)_waves.get(i)).danger(impact);
		}
		return danger;
	}

	Move waveImpactLocation(MovementWave closest, double direction, double maxVelocity) {
		double currentDirection = robotOrbitDirection(closest.gunBearing(robotLocation));
		double v = Math.abs(robot.getVelocity()) * PUtils.sign(direction);
		double h = robot.getHeadingRadians();
		Point2D orbitCenter = orbitCenter(closest);
		Point2D impactLocation = new Point2D.Double(robot.getX(), robot.getY());
		Move smoothed = wallSmoothedDestination(impactLocation, orbitCenter, currentDirection * direction);
		double wantedHeading = PUtils.absoluteBearing(impactLocation, smoothed.location);
		h += PUtils.backAsFrontDirection(wantedHeading, h) < 0 ? Math.PI : 0.0;
		int time = 0;
		do {
			double maxTurn = Math.toRadians(MAX_TURN_RATE - 0.75 * Math.abs(v));
			h += PUtils.minMax(PUtils.backAsFrontTurn(wantedHeading, h), -maxTurn, maxTurn);
			if (v < maxVelocity) {
				v = Math.min(maxVelocity, v + (v < 0 ? 2 : 1));
			}
			else {
				v = Math.max(maxVelocity, v - 2);
			}
			impactLocation = PUtils.project(impactLocation, h, v);
			smoothed = wallSmoothedDestination(impactLocation, orbitCenter, currentDirection * direction);
			wantedHeading = PUtils.absoluteBearing(impactLocation, smoothed.location);
		} while (closest.distanceFromTarget(impactLocation, time++) > 18);
		return new Move(impactLocation, smoothed.smoothing, smoothed.wantedEvasion, smoothed.oldDistance, impactLocation.distance(enemyLocation));
	}

	Point2D orbitCenter(MovementWave wave) {
		return wave != null ? wave.getGunLocation() : enemyLocation;
	}

	double robotOrbitDirection(double bearing) {
		return PUtils.sign(robot.getVelocity() * Math.sin(robot.getHeadingRadians() - bearing));
	}

	public void onPaint(Graphics2D g) {
		WaveGrapher.onPaint(g);
	}

	public void roundOver() {
		WaveGrapher.removeAll();
	}
}

class Move {
	Point2D location;
	double smoothing;
	double wantedEvasion;
	double oldDistance;
	double newDistance;

	Move(Point2D location, double smoothing, double wantedEvasion, double oldDistance, double newDistance) {
		this.location = location;
		this.smoothing = smoothing;
		this.wantedEvasion = wantedEvasion;
		this.oldDistance = oldDistance;
		this.newDistance = newDistance;
	}

	double smoothingDanger() {
		if (normalizedSmoothing() > 65 || (oldDistance > 220 && newDistance < 250) && normalizedSmoothing() > 20) {
			return (1 + smoothing) * 50;
		}
		return 0;
	}

	double normalizedSmoothing() {
		return smoothing / wantedEvasion;
	}
}

class Hit {
	static List<Hit> hits = new ArrayList<Hit>();
	double bulletPower;
	int distance;
	int robotX;
	int robotY;
	int enemyX;
	int enemyY;
	double gf = -100;;

	Hit(double bulletPower, double distance, Point2D robotLocation, Point2D enemyLocation) {
		this.bulletPower = (double)((int)(bulletPower * 100) / 100.0);
		this.distance = (int)distance;
		this.robotX = (int)robotLocation.getX();
		this.robotY = (int)robotLocation.getY();
		this.enemyX = (int)enemyLocation.getX();
		this.enemyY = (int)enemyLocation.getY();
	}

	void print() {
		System.out.println("GF: " + PUtils.formatNumber(gf) + " - bp: " + bulletPower + " - distance: " + distance + " - robotLocation: " + robotX + ":" + robotY + " - enemyLocation: " + enemyX + ":" + enemyY);
	}

	static void printAll() {
		for (int i = 0, n = Hit.hits.size(); i < n; i++) {
			Hit hit = (Hit)Hit.hits.get(i);
			hit.print();
		}
	}
}
