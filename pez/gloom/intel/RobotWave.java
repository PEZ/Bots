package pez.gloom.intel;
import pez.gloom.Bot;
import java.awt.geom.*;

// $Id: RobotWave.java,v 1.3 2003/12/06 23:27:41 peter Exp $
public class RobotWave extends Wave {
    public RobotWave(Bot robot, double bulletPower, Point2D gunLocation, Point2D targetLocation,
                double targetDeltaBearing, double targetMaxBearing, double targetDirection, int[] currentFactorVisits) {

        super(robot, bulletPower, gunLocation, targetLocation, targetDeltaBearing, targetMaxBearing, targetDirection, currentFactorVisits);
        this.triggerDistanceDelta = -5;
    }

    long getTimeOffset() {
	return -1;
    }
}
