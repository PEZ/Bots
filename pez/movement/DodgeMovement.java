package pez.movement;

import java.awt.geom.Point2D;
import pez.Marshmallow;
import pez.Enemy;
import pez.Rutils;


// $Id: DodgeMovement.java,v 1.2 2004/02/20 22:29:12 peter Exp $
public class DodgeMovement extends IncrementalMovement implements pez.MarshmallowConstants {
    private long nextTime;

    public DodgeMovement() {
        super();
        m_name = "DodgeMovement";
    }
    
    public MovementData getMovementData(Enemy enemy, Marshmallow robot) {
        double bulletTravelTime = Rutils.travelTime(enemy.getDistance(), Rutils.bulletVelocity(enemy.getEFirePower()));
	double factor = 1.3 / bulletTravelTime;

        if (robot.getTimeSinceEnemyFired() == 0 && Math.random() < factor) {
            direction *= -1;
            nextTime = robot.getTime() + nextTime(bulletTravelTime);
        }
        if (robot.getTime() >= nextTime) {
            direction *= -1;
            nextTime += nextTime(bulletTravelTime);
            //nextTime = (long)(robot.getTime() + Math.random() * nextTime(bulletTravelTime));
        }

        MovementData movementData = new MovementData();
        movementData.setDestination(relativeDestination(5, enemy, robot));
        return movementData;
    }

    private long nextTime(double bulletTravelTime) {
        return (long)(bulletTravelTime + bulletTravelTime * Math.random());
    }
}
