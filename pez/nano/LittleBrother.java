package pez.nano;
import robocode.*;
import robocode.util.Utils;

// This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
// http://robowiki.net/?RWPCL
//
// LittleBrother, by PEZ. - Something small
// http://robowiki.net/?LittleBrother
// $Id:  Exp $

public class LittleBrother extends AdvancedRobot {
    static final double REVERSE_TUNER = 27;

    static int direction = 5;
    //static double factors[] = { 1.5, 1.5 };
    static double factors[] = { 1.5, 1.5, 1.5, 1.5, 1.5 };
    static int enemyHits;
    static int timer;

    public void run() {
	turnRadarRightRadians(Double.POSITIVE_INFINITY);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
	int stopOrGo = 1;
	double d = e.getDistance() / 11;
	if (timer++ > d) {
	    stopOrGo = 0;
	}
	if (timer > d * factors[enemyHits % 5]) {
	    timer = 0;
	}
	setAhead(direction * 20 * stopOrGo);
	setTurnRight(e.getBearing() + 90 - direction);

        double enemyAbsoluteBearing = getHeadingRadians() + e.getBearingRadians();
	setTurnGunRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing + e.getVelocity() * Math.sin(e.getHeadingRadians() - enemyAbsoluteBearing) * (Math.random() / (8 + getRoundNum())) - getGunHeadingRadians()));
	setFire(e.getDistance() < 140 ? 3.0 : 1.9);

        setTurnRadarLeftRadians(getRadarTurnRemaining() * 2);
    }

    public void onHitByBullet(HitByBulletEvent e) {
	enemyHits++;
    }

    public void onHitWall(HitWallEvent e) {
	direction = -direction;
    }
}
