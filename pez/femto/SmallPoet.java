package pez.femto;
import robocode.*;

// SmallPoet, by PEZ

public class SmallPoet extends AdvancedRobot {
    public void run() { 
        setTurnGunRight(Double.POSITIVE_INFINITY); 
    } 

    public void onScannedRobot(ScannedRobotEvent e) { 
        setTurnRight(90 + e.getBearing());
        setAhead(200 * Math.sin(getTime())); 
        fire(3); 
    } 
}
