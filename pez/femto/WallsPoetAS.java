package pez.femto;
import robocode.*;

// WallsPoetAS, for the http://robowiki.net/?SpinBotChallenge
// By Peter Stromberg, http://robowiki.net/?PEZ

public class WallsPoetAS extends Robot {
    public void run() {
        turnGunLeft(Double.POSITIVE_INFINITY); 
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        fire(3);
        ahead(150);
    }

    public void onHitWall(HitWallEvent e) {
        turnLeft(getHeading() % 90 - 90);
    }
} 
