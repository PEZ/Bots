package pez.femto;
import robocode.*;

// By Peter Strmberg, http://robowiki.dyndns.org/?PEZ
// Haiku wall avoidance

public class WallsPoetHaiku extends AdvancedRobot {
    public void run() {
        setTurnGunRight(Double.POSITIVE_INFINITY);
        while (true) {
            ahead(getBattleFieldHeight() - 100);
            setTurnRight(90 - getHeading() % 90); 
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        fire(e.getEnergy() / 4);
    }
} 
