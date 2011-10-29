package pez.movement;

import java.awt.geom.Point2D;
import pez.MarshmallowConstants;

public class MovementData {
    public void setDestination(Point2D destination) {
        m_destination = destination;
    }
    
    public Point2D getDestination() {
        return m_destination;
    }
    
    private Point2D m_destination = null;    
}
