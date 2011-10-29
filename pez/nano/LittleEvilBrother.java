package pez.nano;
import robocode.*;
import robocode.util.Utils;
import java.util.zip.*;
import java.io.*;

// This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
// http://robowiki.net/?RWPCL
//
// LittleEvilBrother, by PEZ. - Likes to be annoying
// http://robowiki.net/?LittleEvilBrother
// $Id: LittleEvilBrother.java,v 1.2 2004/02/27 15:21:26 peter Exp $

public class LittleEvilBrother extends AdvancedRobot {
    static final double BATTLE_FIELD_WIDTH = 800;
    static final double BATTLE_FIELD_HEIGHT = 600;

    static final double MAX_DISTANCE = 900;
    static final double MAX_BULLET_POWER = 3.0;
    static final double REVERSE_TUNER = 27;
    static final double APPROACH_ANGLE = 5; // degrees

    static final int DISTANCE_INDEXES = 5;
    static final int VELOCITY_INDEXES = 5;

    static double enemyVelocity;
    static double[][][] aimFactors;
    static double direction = 1;

    public void run() {
	turnRadarRightRadians(Double.POSITIVE_INFINITY);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
	setTurnRight(e.getBearing() + 90 - direction * APPROACH_ANGLE);
	setAhead(direction * 100);
	if (Math.random() < REVERSE_TUNER / e.getDistance()) {
	    direction = -direction;
	}

	if (aimFactors == null) {
	    try {
		aimFactors = (double[][][])(new ObjectInputStream(new GZIPInputStream(new FileInputStream(getDataFile(e.getName()))))).readObject();
	    } catch (Exception exception) {
		aimFactors = new double[DISTANCE_INDEXES][VELOCITY_INDEXES][VELOCITY_INDEXES];
	    }
	}
        double enemyAbsoluteBearing = getHeadingRadians() + e.getBearingRadians();

	setTurnGunRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getGunHeadingRadians() +
		aimFactors[(int)(e.getDistance() / (MAX_DISTANCE / DISTANCE_INDEXES))][(int)Math.abs(enemyVelocity) / 2][(int)Math.abs(enemyVelocity = e.getVelocity()) / 2] *
		(enemyVelocity * Math.sin(e.getHeadingRadians() - enemyAbsoluteBearing) > 0 ? 1 : -1)));

	setFire(MAX_BULLET_POWER);

        setTurnRadarLeftRadians(getRadarTurnRemaining());
    }
}
