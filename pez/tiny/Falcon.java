package pez.tiny;
import robocode.*;
import robocode.util.Utils;
import java.awt.geom.*;

// This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
// http://robowiki.net/?RWPCL
//
// Falcon, by PEZ. How small can I get a GF gunning bot?
// $Id: Falcon.java,v 1.4 2004/02/16 23:35:12 peter Exp $

public class Falcon extends AdvancedRobot {
    static final double BULLET_POWER = 3.0;
    static final double BULLET_VELOCITY = 20 - 3 * BULLET_POWER;

    static final int AIM_FACTORS = 25;
    static final int MIDDLE_FACTOR = (AIM_FACTORS - 1) / 2;

    static double enemyX;
    static double enemyY;
    static int[] aimFactors = new int[AIM_FACTORS];

    public void run() {
        setAdjustRadarForGunTurn(true);
	turnRadarRightRadians(Double.POSITIVE_INFINITY); 
    }

    public void onScannedRobot(ScannedRobotEvent e) {
	Wave wave;
	addCustomEvent(wave = new Wave());
        double enemyAbsoluteBearing = getHeadingRadians() + e.getBearingRadians();
	enemyX = (wave.wGunX = getX()) + Math.sin(enemyAbsoluteBearing) * e.getDistance();
	enemyY = (wave.wGunY = getY()) + Math.cos(enemyAbsoluteBearing) * e.getDistance();

	wave.wBearingDirection = (e.getVelocity() * Math.sin(e.getHeadingRadians() - enemyAbsoluteBearing) < 0 ? -1 : 1) * 0.8 / (double)MIDDLE_FACTOR;
	wave.wBearing = enemyAbsoluteBearing;

	int mostVisited = MIDDLE_FACTOR;
	for (int i = 0; i < AIM_FACTORS; i++) {
	    if (aimFactors[i] > aimFactors[mostVisited]) {
		mostVisited = i;
	    }
	}
	setTurnGunRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getGunHeadingRadians() +
	    wave.wBearingDirection * (mostVisited - MIDDLE_FACTOR)));

	if (setFireBullet(BULLET_POWER) != null) out.println(mostVisited - MIDDLE_FACTOR);

        setTurnRadarRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getRadarHeadingRadians()) * 2);
    }

    class Wave extends Condition {
	double wGunX;
	double wGunY;
	double wBearing;
	double wBearingDirection;
	double wDistance;

	public boolean test() {
	    if ((wDistance += BULLET_VELOCITY) > Point2D.distance(wGunX, wGunY, enemyX, enemyY)) {
		try {
		    aimFactors[(int)Math.round(((Utils.normalRelativeAngle(Math.atan2(enemyX - wGunX, enemyY - wGunY) - wBearing)) /
				wBearingDirection) + MIDDLE_FACTOR)]++;
		}
		catch (Exception e) {
		}
		removeCustomEvent(this);
	    }
	    return false;
	}
    }
}
