package pez.femto;
import robocode.*;

// WallsPoet, is this Graffiti?
// By Peter Stromberg, http://robowiki.net/?PEZ

public class WallsPoet extends Robot {
    public void run() {
        turnGunLeft(Double.POSITIVE_INFINITY); 
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        fire(3);
        ahead(-75 + Math.random() * 350);
    }

    public void onHitWall(HitWallEvent e) {
        turnLeft(getHeading() % 90 - 90);
    }
} 
