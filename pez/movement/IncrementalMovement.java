package pez.movement;

import java.awt.geom.Point2D;
import pez.Marshmallow;
import pez.Enemy;
import pez.Rutils;


// $Id: IncrementalMovement.java,v 1.1 2003/08/06 22:38:25 peter Exp $
public abstract class IncrementalMovement extends MovementStrategy implements pez.MarshmallowConstants {
    int direction = 1;
    double cornerMargin = 40;

    IncrementalMovement() {
        super();
        m_name = "IncrementalMovement";
    }
    
    public abstract MovementData getMovementData(Enemy enemy, Marshmallow robot);

    Point2D relativeDestination(double relativeAngle, Enemy enemy, Marshmallow robot) {
        Point2D destination = new Point2D.Double();
        double distanceExtra = distanceExtra(enemy, robot, cornerMargin);
        distanceExtra *= relativeAngle;
        Rutils.toLocation(enemy.getAbsoluteBearing() + 180 + direction * relativeAngle,
            enemy.getDistance() + distanceExtra, enemy.getLocation(), destination);
        double wantedTravelDistance = robot.getLocation().distance(destination);
        robot.translateInsideField(destination, MC_WALL_MARGIN);
        Rutils.toLocation(Rutils.pointsToAngle(robot.getLocation(), destination), wantedTravelDistance, robot.getLocation(), destination);
        robot.translateInsideField(destination, MC_WALL_MARGIN);
        if (robot.getLocation().distance(destination) < 0.9 * wantedTravelDistance) {
            direction *= -1;
        }
        return destination;
    }
}
