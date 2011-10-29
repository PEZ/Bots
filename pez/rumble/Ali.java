package pez.rumble;
import pez.rumble.pgun.*;
import pez.rumble.pmove.*;
import robocode.*;
import robocode.util.Utils;
import java.util.*;
import java.awt.geom.*;
import java.awt.Color;

// Ali - by PEZ - Float like a butterfly. Sting like a bumble-bee.
//
// This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
// http://robowiki.net/?RWPCL
// (Basically it means you must keep your code public if you use mine.)
//
// $Id: CassiusClay.java,v 1.12 2004/09/28 23:47:16 peter Exp $

public class Ali extends AdvancedRobot {
    static boolean isTC = true; // http://robowiki.net/?TargetingChallenge
    static boolean isMC = false; // http://robowiki.net/?MovementChallenge
    static boolean doGL = false; // http://robowiki.net/?RobocodeGL
    static double wins;

    Butterfly floater;
    BumbleBee stinger;
    int timeSinceScan = 0;
    ScannedRobotEvent lastScanEvent;

    public void run() {
	BumbleBee.isTC = isTC;
	BumbleBee.doGL = doGL;
	Butterfly.isMC = isMC;
	floater = new Butterfly(this);
	stinger = new BumbleBee(this);

	setColors(new Color(90, 55, 55), Color.black, Color.red);
	setAdjustRadarForGunTurn(true);
	setAdjustGunForRobotTurn(true);

	do {
	    if (timeSinceScan++ > 1) {
		setTurnRadarRightRadians(Double.POSITIVE_INFINITY); 
	    }
	    if (getOthers() == 0 && timeSinceScan > 5) {
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

    public void onDeath(DeathEvent e) {
	stinger.roundOver();
    }

    public void onWin(WinEvent e) {
	wins++;
	stinger.roundOver();
    }
}
