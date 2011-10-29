package pez.rumble.utils;

// MovementVector, utility class for use with RobotPredictor. By PEZ.
// http://robowiki.net/?PEZ
//
// This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
// http://robowiki.net/?RWPCL
// (Basically it means you must keep the code public if you use it in any way.)
//
// $Id:  Exp $

public final class MovementVector {
    public double h;
    public double v;

    public MovementVector(double heading, double velocity) {
	this.h = heading;
	this.v = velocity;
    }
}
