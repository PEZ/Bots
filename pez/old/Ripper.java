package pez;
import robocode.*;
import java.awt.Color;
import java.util.Vector;
import java.util.Iterator;

public class Ripper extends AdvancedRobot {
    static final double longTravel = 99999;
    static final double closeDistance = 150;
    static final double farAwayDistance = 850;

    double previousEnergy = 100;
    double previousDistance = 500;
    double firePower = 3;
    
    int movementDirection = -1;
    int gunDirection = 1;
	
    Enemies enemies = new Enemies();
    Enemy currentEnemy = null;

    class Enemy {
	private String name;
	
	public Enemy(String name) {
	    setName(name);
	}

	public String getName() {
	    return name;
	}
	
	public void setName(String name) {
	    this.name = name;
	}
	
	public boolean equals(Object object) {
	    if (object instanceof Enemy) {
		return (((Enemy) object).getName().equals(this.getName()));
	    }
	    return false;
	}
    }

    class Enemies extends Vector {
	public Enemies() {
	}

	public Enemy getClosest() {
	    Enemy closest = null;
	    Iterator iterator = this.iterator();
	    while (iterator.hasNext()) {
		Enemy anEnemy = (Enemy)iterator.next();
		out.println("An enemy: " + anEnemy.getName());
		closest = anEnemy;
	    }
	    return closest;
	}
    }

    public void run() {
	setColors(Color.red,Color.blue,Color.green);
	setTurnGunRight(longTravel);
    }

    public void onHitWall(HitWallEvent e) {
	movementDirection = -movementDirection;
	setAhead(closeDistance);
    }

    public void onHitByBullet(HitByBulletEvent e) {
	setTurnRight(e.getBearing()+100 * movementDirection);
	movementDirection = -movementDirection;
	setAhead((closeDistance) * movementDirection);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
	double energyDelta = previousEnergy-e.getEnergy();
	//Enemy enemy = new Enemy(e.getName());
	//if (!enemies.contains(enemy)) {
	//    enemies.add(enemy);
	//}
	firePower = e.getDistance() < closeDistance ? 3 : 1.2;
	if (energyDelta > 0 && energyDelta <= 3) {
	    setTurnRight(e.getBearing()+60 * movementDirection);
	    setAhead((e.getDistance()/4+25) * movementDirection);
	    movementDirection = -movementDirection;
	}
	gunDirection = -gunDirection;
	setTurnGunRight(longTravel*gunDirection);
	fire(firePower);		
	previousEnergy = e.getEnergy();
	previousDistance = e.getDistance();

    }
}
