package pez.rumble.utils;
import robocode.*;
import robocode.util.Utils;
import java.util.*;
import java.awt.geom.*;

//Wave, base class for stat gathering wave. By PEZ.
//http://robowiki.net/?PEZ

//This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
//http://robowiki.net/?RWPCL
//(Basically it means you must keep the code public if you use it in any way.)

//$Id: Wave.java,v 1.2 2006/02/23 23:42:30 peter Exp $

public abstract class Wave {
	protected AdvancedRobot robot;

	protected double[] roots;
	protected int numBins;
	protected int middleBin;
	protected double bulletVelocity;
	protected double distanceFromGun;
	protected double startBearing;
	protected double bearingDirection;
	protected Point2D gunLocation;
	protected Point2D targetLocation;
	protected Point2D startTargetLocation = new Point2D.Double();
	protected long startTime;

	public void init(AdvancedRobot robot, int numBins) {
		this.robot = robot;
		this.numBins = numBins;
		this.middleBin = (numBins - 1) / 2;
		if (roots == null) {
			roots = new double[numBins];
			for (int i = 0; i < numBins; i++) {
				roots[i] = Math.pow(i + 1.0, 0.5);
			}
		}
	}

	public boolean passed(double distanceOffset) {
		return distanceFromTarget() < distanceOffset;
	}

	public void advance(int ticks) {
		distanceFromGun += ticks * bulletVelocity;
	}

	public int visitingIndex() {
		return visitingIndex(targetLocation);
	}

	public int visitingIndex(Point2D location) {
		return visitingIndex(PUtils.absoluteBearing(gunLocation, location));
	}

	public int visitingIndex(double bearing) {
		return (int)PUtils.minMax(
				Math.round(((Utils.normalRelativeAngle(bearing - startBearing)) / bearingDirection) + middleBin), 1, numBins - 1);
	}

	public double maxEscapeAngle() {
		return PUtils.maxEscapeAngle(bulletVelocity);
	}

	public double gunBearing(Point2D target) {
		return PUtils.absoluteBearing(gunLocation, target);
	}

	public double distanceFromTarget(Point2D location, int timeOffset) {
		return gunLocation.distance(location) - distanceFromGun - (double)timeOffset * bulletVelocity;
	}

	public double distanceFromTarget(int timeOffset) {
		return distanceFromTarget(targetLocation, timeOffset);
	}

	public double distanceFromTarget(Point2D location) {
		return distanceFromTarget(location, 0);
	}

	public double distanceFromTarget() {
		return distanceFromTarget(0);
	}

	public void setStartTime(long time) {
		this.startTime = time - 1;
	}

	public double getStartTime() {
		return this.startTime;
	}

	public void setDistanceFromGun(double distance) {
		this.distanceFromGun = distance;
	}

	public double distanceFromGun() {
		return this.distanceFromGun;
	}

	public double travelTime(double distance) {
		return distance / bulletVelocity;
	}

	public double travelTime() {
		return distanceFromTarget(0) / bulletVelocity;
	}

	public double getFireDistance() {
		return gunLocation.distance(targetLocation);
	}

	public AdvancedRobot getRobot() {
		return robot;
	}

	public Point2D getGunLocation() {
		return gunLocation;
	}

	public void setBulletVelocity(double bulletVelocity) {
		this.bulletVelocity = bulletVelocity;
	}

	public double getBulletVelocity() {
		return this.bulletVelocity;
	}

	public void setStartBearing(double startBearing) {
		this.startBearing = startBearing;
	}

	public double getStartBearing() {
		return this.startBearing;
	}

	public void setOrbitDirection(double bearingDirection) {
		this.bearingDirection = bearingDirection;
		if (bearingDirection == 0) {
			this.bearingDirection = 0.73;
		}
	}

	public double getOrbitDirection() {
		return this.bearingDirection;
	}

	public double getGF(Point2D location) {
		return Utils.normalRelativeAngle(PUtils.absoluteBearing(gunLocation, location) - startBearing) / (bearingDirection * middleBin);
	}

	public double getGF() {
		return getGF(targetLocation);
	}

	public void setGunLocation(Point2D gunLocation) {
		this.gunLocation = gunLocation;
	}

	public void setTargetLocation(Point2D targetLocation) {
		this.targetLocation = targetLocation;
		this.startTargetLocation.setLocation(targetLocation);
	}

	public int botWidth() {
		return (int)Math.ceil(middleBin * PUtils.botWidthAngle(getFireDistance() / maxEscapeAngle()));
	}

	public double wallDistance(double direction, Rectangle2D fieldRectangle) {
		double targetDistance = distanceFromTarget();
		for (double d = 0; d < 2; d += 0.01) {
			if (!fieldRectangle.contains(PUtils.project(gunLocation,
					startBearing + middleBin * bearingDirection * direction * d, targetDistance))) {
				return d;
			}
		}
		return 2;
	}

	public static Wave findClosest(List _waves, Point2D location) {
		return findClosest(_waves, location, -1);
	}

	public static Wave findClosest(List _waves, Point2D location, double velocity) {
		double closestDistance = Double.POSITIVE_INFINITY;
		Wave closest = null;
		for (int i = 0, n = _waves.size(); i < n; i++) {
			Wave wave = (Wave)_waves.get(i);
			double d = wave.distanceFromTarget(location);
			if (velocity > 0 || ( velocity < 0 && d > 0)) {
				d = Math.abs(d);
				if (d < closestDistance && (velocity < 0 || Math.abs(wave.bulletVelocity - velocity) < 0.2)) {
					closest = wave;
					closestDistance = d;
				}
			}
		}
		return closest;
	}
}
