package pez.tests;
import robocode.*;

public class EventPrioTest extends AdvancedRobot {
    static long counter;

    public void run() {
	setEventPriority("ScannedRobotEvent", 90);
	do {
	    turnGunRightRadians(Double.POSITIVE_INFINITY); 
	} while (true);
    }

    public void onBulletHit(BulletHitEvent e) {
	out.println("bullet hit! " + getTime() + " " + counter++);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
	out.println("scanning! " + getTime()+ " " + counter++);
	setFire(0.1);
	setTurnGunLeftRadians(getGunTurnRemaining());
    }
}
