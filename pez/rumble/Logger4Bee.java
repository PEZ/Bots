package pez.rumble;
import pez.rumble.pgun.*;
import wiki.rmove.*;
import robocode.*;
import robocode.util.Utils;
import java.awt.Color;
	
// Logger4Bee (a gun logging bot with - LoggingBee gun by PEZ - Raiko Movement by Jamougha)
//
// Released under the RWPCL - RoboWiki Public Code Licence - http://robowiki.net/?RWPCL

public class Logger4Bee extends AdvancedRobot {
    static boolean isTC = false; // http://robowiki.net/?TargetingChallenge
    LoggingBee gun;
    RaikoNMT movement;

    public void run() {
	LoggingBee.isTC = isTC;
	gun = new LoggingBee(this);
	movement = new RaikoNMT(this);
        setColors(Color.white, Color.white, Color.magenta);
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        do {
            turnRadarRightRadians(Double.POSITIVE_INFINITY); 
        } while (true);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
	gun.onScannedRobot(e);
	if (!isTC) {
	    movement.onScannedRobot(e);
	}
	setTurnRadarRightRadians(Utils.normalRelativeAngle(getHeadingRadians() + e.getBearingRadians() - getRadarHeadingRadians()) * 2);
    }

    public void onBulletHit(BulletHitEvent e) {
	gun.onBulletHit(e);
    }

    public void onDeath(DeathEvent e) {
	gun.roundOver();
    }

    public void onWin(WinEvent e) {
	gun.roundOver();
    }
}
