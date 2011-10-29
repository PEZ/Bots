package pez;
import robocode.*;
import java.awt.Color;

/**
 * Foo - a robot by (your name here)
 */
public class Foo extends Robot
{
	public void run() {
		setColors(Color.yellow,Color.blue,Color.green);
		while(true) {
			ahead(50);
			turnRadarRight(120);
			turnLeft(35);
			ahead(70);
			turnRadarLeft(180);
			turnRight(45);
			ahead(100);
			turnRadarLeft(180);
			turnRight(45);
		}
	}

	public String getName() {
	    return String.valueOf(getEnergy());
	}

	public void onHitWall(HitWallEvent e) {
		back(100);
		turnRight(90);
	}

	public void onHitRobot(HitRobotEvent e) {
		back(100);
		turnRight(90);
	}

	public void onScannedRobot(ScannedRobotEvent e) {
		turnGunRight(getHeading()-getGunHeading()+e.getBearing());
		fire(e.getDistance() < 290 ? 2.6 : 1.1);
		turnGunRight(getHeading()-getGunHeading()+e.getBearing());
		fire(e.getDistance() < 290 ? 2.6 : 1.1);
	}

	public void onHitByBullet(HitByBulletEvent e) {
		turnLeft(90 - e.getBearing());
	}

}
