package pez.rumble.utils;
import robocode.*;
import robocode.util.Utils;
import java.util.*;
import java.awt.geom.*;

// PUtils, some nice-to-have methods and constants. By PEZ.
// http://robowiki.net/?PEZ
//
// This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
// http://robowiki.net/?RWPCL
// (Basically it means you must keep the code public if you base any bot on it.)
//
// $Id: PUtils.java,v 1.6 2004/09/17 14:54:14 peter Exp $

public final class PUtils {
    public static final double MAX_ROBOT_VELOCITY = 8;
    public static final double MAX_TURN_RATE = 10;
    public static final double BOT_WIDTH = 36;

    public static Point2D project(Point2D sourceLocation, double angle, double length) {
	return new Point2D.Double(sourceLocation.getX() + Math.sin(angle) * length,
		sourceLocation.getY() + Math.cos(angle) * length);
    }

    public static double absoluteBearing(Point2D source, Point2D target) {
	return Math.atan2(target.getX() - source.getX(), target.getY() - source.getY());
    }

    public static int sign(double v) {
	return v < 0 ? -1 : 1;
    }

    public static double minMax(double v, double min, double max) {
	return Math.max(min, Math.min(max, v));
    }

    public static double maxEscapeAngle(double bulletVelocity) {
	return Math.asin(MAX_ROBOT_VELOCITY / bulletVelocity);
    }

    public static Rectangle2D fieldRectangle(AdvancedRobot robot, double margin) {
	return new Rectangle2D.Double(margin, margin,
		robot.getBattleFieldWidth() - margin * 2, robot.getBattleFieldHeight() - margin * 2);
    }

    public static double bulletVelocity(double bulletPower) {
	return 20 - 3 * bulletPower;
    }

    public static int getVelocityIndex(double velocity) {
	return (int)Math.abs(velocity / 2);
    }

    public static int index(double[] slices, double v) {
	for (int i = 0; i < slices.length; i++) {
	    if (v < slices[i]) {
		return i;
	    }
	}
	return slices.length;
    }

    public static int index(double v, double indexes, double maxV) {
	return (int)Math.min(indexes - 1, (v / (maxV / indexes)));
    }

    public static double botWidthAngle(double distance) {
	return Math.atan2(BOT_WIDTH, distance);
    }

    //Paul Evans' rolling average function
    public static double rollingAvg(double value, double newEntry, double n) {
	return (value * n + newEntry) / (n + 1.0);
    } 

    public static double backAsFrontTurn(double newHeading, double oldHeading) {
	return Math.tan(newHeading - oldHeading);
    }

    public static double backAsFrontDirection(double newHeading, double oldHeading) {
	return sign(Math.cos(newHeading - oldHeading));
    }

    public static String formatNumber(double num) {
	return java.text.NumberFormat.getNumberInstance().format(num);
    }

    public static boolean isLastRound(AdvancedRobot robot) {
	return robot.getRoundNum() == robot.getNumRounds() - 1;
    }
}
