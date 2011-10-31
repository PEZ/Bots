package pez.rumble;
import java.awt.Graphics2D;

import pez.rumble.pgun.*;
import pez.rumble.pmove.*;
import pez.rumble.utils.*;
import robocode.*;
import robocode.util.Utils;

//CassiusClay - by PEZ - Float like a butterfly. Sting like a bee.
//http://robowiki.net/?CassiusClay

//This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
//http://robowiki.net/?RWPCL
//(Basically it means you must keep the code public if you base any bot on it.)

//$Id$

public abstract class RumbleBot extends AdvancedRobot {
	boolean isTC = false; // http://robowiki.net/?TargetingChallenge
	boolean isMC = false; // http://robowiki.net/?MovementChallenge
	boolean doGL = false; // http://robowiki.net/?CassiusClay/GL

	static double wins;
	static int skipped;

	Butterfly floater;
	Stinger stinger;
	RobotPredictor robotPredictor = new RobotPredictor();
	int timeSinceScan = 0;
	ScannedRobotEvent lastScanEvent;
	static long scans;
	static double enemyApproachVelocity;


	public void run() {
		Stinger.isTC = isTC;
		Butterfly.isMC = isMC;
		Butterfly.doGL = doGL;
		setAdjustRadarForGunTurn(true);
		setAdjustGunForRobotTurn(true);

		System.out.println("Skipped turns: " + skipped);
		do {
			if (timeSinceScan++ > 1) {
				setTurnRadarRightRadians(Double.POSITIVE_INFINITY); 
			}
			if (getOthers() == 0) {
				onScannedRobot(lastScanEvent);
			}
			execute();
		} while (true);
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		scans++;
		double enemyAbsBearing = getHeadingRadians() + e.getBearingRadians();
		if (getOthers() > 0 && scans > 0) {
			enemyApproachVelocity = PUtils.rollingAvg(enemyApproachVelocity, e.getVelocity() * -Math.cos(e.getHeadingRadians() - enemyAbsBearing), Math.min(scans, 5000));
		}

		lastScanEvent = e;
		timeSinceScan = 0;
		setTurnRadarRightRadians(Utils.normalRelativeAngle(enemyAbsBearing - getRadarHeadingRadians()) * 2);
		if (!isTC) {
			floater.onScannedRobot(e);
		}
		if (!isMC) {
			stinger.onScannedRobot(e);
		}
	}

	public void onHitByBullet(HitByBulletEvent e) {
		floater.onHitByBullet(e);
	}

	public void onBulletHit(BulletHitEvent e) {
		floater.onBulletHit(e);
		stinger.onBulletHit(e);
	}

	public void onBulletHitBullet(BulletHitBulletEvent e) {
		floater.onBulletHitBullet(e);
	}

	public void onWin(WinEvent e) {
		stinger.roundOver();
		wins++;
	}

	public void onDeath(DeathEvent e) {
		stinger.roundOver();
	}

	public void onSkippedTurn(SkippedTurnEvent e) {
		System.out.println("skipped turn! time = " + getTime());
		skipped++;
	}

	public void setTurnRightRadians(double turn) {
		super.setTurnRightRadians(turn);
		robotPredictor.setTurnRightRadians(turn);
	}

	public void setAhead(double d) {
		super.setAhead(d);
		robotPredictor.setAhead(d);
	}

	public void setMaxVelocity(double v) {
		super.setMaxVelocity(v);
		robotPredictor.setMaxVelocity(v);
	}
	
	public static boolean enemyIsRammer() {
		return enemyApproachVelocity > 4.5;
	}
	
	public void onPaint(Graphics2D g) {
		doGL = true;
		Butterfly.doGL = true;
		floater.onPaint(g);
	}
}
