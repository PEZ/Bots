package pez.femto;
import robocode.*;

// HaikuWalls, by tobe

public class HaikuWalls extends AdvancedRobot {
    public void run() { 
        setTurnGunRight(Double.POSITIVE_INFINITY); 
    } 
    public void onScannedRobot(ScannedRobotEvent e) { 
        setAhead(Double.POSITIVE_INFINITY); 
        fire( 3); 
    } 
    public void onHitWall(HitWallEvent e) { 
        turnRight( 90 +e.getBearing()); 
    } 
}
