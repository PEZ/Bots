package pez.femto;
import robocode.*;

// By Peter Stršmberg, http://robowiki.dyndns.org/?PEZ

public class HaikuPoet extends AdvancedRobot {
    public void run() { 
        setTurnGunRightRadians(Double.POSITIVE_INFINITY); 
    } 

    public void onScannedRobot(ScannedRobotEvent e) { 
        setTurnRight(e.getBearing() + 80);
        setAhead((setFireBullet(2.1) == null ? 100 : 0) * Math.sin(getTime() / 12)); 
        setTurnGunLeftRadians(getGunTurnRemainingRadians());
    } 
} 
