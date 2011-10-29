package pez.tests;
import pez.tests.pgun.*;
import robocode.*;
import robocode.util.Utils;
import java.util.*;
import java.awt.geom.*;
import java.awt.Color;

// This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
// http://robowiki.net/?RWPCL
// (Basically it means you must keep the code public if you base any bot on it.)
//
// SouthPaw, by PEZ. Just a TC test bot of the PugilistPunch gun.
//
// $Id: SouthPaw.java,v 1.2 2004/05/18 22:13:48 peter Exp $

public class SouthPaw extends AdvancedRobot {
    PugilistPunch gun;

    public void run() {
	PugilistPunch.isTC = true;
	gun = new PugilistPunch(this);

	setAdjustRadarForGunTurn(true);
	setAdjustGunForRobotTurn(true);

	do {
	    turnRadarRightRadians(Double.POSITIVE_INFINITY); 
	} while (true);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
	gun.onScannedRobot(e);
	setTurnRadarRightRadians(Utils.normalRelativeAngle(getHeadingRadians() + e.getBearingRadians() - getRadarHeadingRadians()) * 2);
    }
}
