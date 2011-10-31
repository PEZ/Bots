package pez.rumble.utils;
import robocode.*;
import java.awt.geom.*;

// RobotPredictor, for predicting the next tick's location of the robot. By PEZ.
// http://robowiki.net/?PEZ
//
// This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
// http://robowiki.net/?RWPCL
// (Basically it means you must keep the code public if you use it in any way.)
//
// $Id: RobotPredictor.java,v 1.2 2006/02/23 23:42:30 peter Exp $

public final class RobotPredictor {
    double ahead = 0;
    double turnRightRadians = 0;
    double maxVelocity = PUtils.MAX_ROBOT_VELOCITY;

    public void setAhead(double d) {
	ahead = d;
    }

    public void setTurnRightRadians(double turn) {
	turnRightRadians = turn;
    }

    public void setMaxVelocity(double v) {
	maxVelocity = v;
    }

    public Point2D getNextLocation(AdvancedRobot robot) {
	double currentDirection = PUtils.sign(robot.getDistanceRemaining());
	double wantedDirection = PUtils.sign(ahead);
	double v = robot.getVelocity() * currentDirection;
	return nextLocation(new Point2D.Double(robot.getX(), robot.getY()), new MovementVector(robot.getHeadingRadians(), v), wantedDirection, turnRightRadians, maxVelocity);
    }

    public static Point2D nextLocation(Point2D location, MovementVector mv, double wantedDirection, double wantedTurn, double maxV) {
	double maxTurn = Math.toRadians(PUtils.MAX_TURN_RATE - 0.75 * Math.abs(mv.v));
	mv.h = mv.h + PUtils.minMax(wantedTurn, -maxTurn, maxTurn);
	if (mv.v < maxV) {
	    mv.v = Math.min(maxV, mv.v + (mv.v < 0 ? 2 : 1));
	}
	else {
	    mv.v = Math.max(maxV, mv.v - 2);
	}
	return PUtils.project(location, mv.h, mv.v * wantedDirection);
    }
}
