package pez.nano;
import robocode.*;
import robocode.util.Utils;

// This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
// http://robowiki.net/?RWPCL
//
// Icarus, by PEZ. - Something small
// http://robowiki.net/?Icarus
// $Id: Icarus.java,v 1.4 2004/08/25 16:51:40 peter Exp $

public class Icarus extends AdvancedRobot {
    static int direction = 7;
    static double bulletVelocity = -3;
    static double distance;
    static int hits;

    public void run() {
	turnRadarRightRadians(Double.POSITIVE_INFINITY);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
	distance = e.getDistance();
	double v = 0.5 * bulletVelocity / distance;
	if (hits > 3 && Math.random() > Math.pow(v, v)) {
	    direction = -direction;
	}
	setAhead(direction * 15);
	setTurnRight(e.getBearing() + 90 - direction);

        double enemyAbsoluteBearing = getHeadingRadians() + e.getBearingRadians();
	setTurnGunRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getGunHeadingRadians() + e.getVelocity() * Math.sin(e.getHeadingRadians() - enemyAbsoluteBearing) / 13.0));
	setFire(distance < 140 ? 3.0 : 1.9);

	setTurnRadarLeftRadians(getRadarTurnRemaining());
    }

    public void onHitWall(HitWallEvent e) {
	turnRight(e.getBearing() - 100); 
	ahead(100);
    }

    public void onHitByBullet(HitByBulletEvent e) {
	bulletVelocity = e.getVelocity();
	hits++;
    }
}
