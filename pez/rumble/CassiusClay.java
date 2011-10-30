package pez.rumble;
import pez.rumble.pgun.*;
import pez.rumble.pmove.*;
import pez.rumble.utils.*;
import robocode.*;
import robocode.util.Utils;
import java.util.*;
import java.awt.geom.*;
import java.awt.Color;
import java.awt.Graphics2D;

// CassiusClay - by PEZ - Float like a butterfly. Sting like a bee.
//
// This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
// http://robowiki.net/?RWPCL
// (Basically it means you must keep the code public if you base any bot on it.)
//
// $Id: CassiusClay.java,v 1.12 2004/09/28 23:47:16 peter Exp $

public class CassiusClay extends AdvancedRobot {
	static boolean isTC = false; // http://robowiki.net/?TargetingChallenge
	static boolean isMC = false; // http://robowiki.net/?MovementChallenge
	static boolean doGL = true; // http://robowiki.net/?CassiusClay/GL
	static double wins;
	static int skipped;

	Butterfly floater;
	Bee stinger;
	RobotPredictor robotPredictor = new RobotPredictor();
	int timeSinceScan = 0;
	ScannedRobotEvent lastScanEvent;

	public void run() {
		Bee.isTC = isTC;
		Butterfly.isMC = isMC;
		Butterfly.doGL = doGL;
		floater = new Butterfly(this);
		stinger = new Bee(this, robotPredictor);

		setColors(new Color(60, 30, 10), Color.green, Color.black);
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
		lastScanEvent = e;
		timeSinceScan = 0;
		setTurnRadarRightRadians(Utils.normalRelativeAngle(getHeadingRadians() + e.getBearingRadians() - getRadarHeadingRadians()) * 2);
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
	
	public void onPaint(Graphics2D g) {
		floater.onPaint(g);
	}
}
