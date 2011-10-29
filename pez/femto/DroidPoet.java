package pez.femto;
import robocode.*;

// By Peter Strmberg, http://robowiki.dyndns.org/?PEZ
// Blind and strong

public class DroidPoet extends Robot implements Droid {
    public void run() {
        while (true) {
            ahead(Math.random() * 600);
            turnGunRight(Math.toDegrees(Math.atan2(getBattleFieldWidth() / 2 - getX(), getBattleFieldHeight() / 2 - getY())) -
                getGunHeading());
            fire(1.2);
        }
    }

    public void onHitWall(HitWallEvent e) {
        turnRight(90 - getHeading() % 90); 
    }
} 
