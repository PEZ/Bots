package pez.femto;
import robocode.*;
import java.awt.geom.Point2D;

// Poet, just for fun
// By Peter Strmberg, http://robowiki.dyndns.org/?PEZ

public class Poet extends AdvancedRobot {
    public void run() {
       turnGunRightRadians(Double.POSITIVE_INFINITY); 
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        setTurnRight(Point2D.Double.distance(400, 300, getX(), getY()) - 150);
        setAhead(100);
        setFire(3);
        setTurnGunLeftRadians(getGunTurnRemainingRadians());
    }
} 
