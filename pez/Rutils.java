package pez;
import java.awt.geom.*;
import java.awt.Polygon;

// Robot Utils - for doing the math and such
// $Id: Rutils.java,v 1.26 2004/02/20 09:55:35 peter Exp $
public class Rutils implements MarshmallowConstants {
    public static double bulletVelocity(double power) {
        return 20 - 3 * power;
    }

    public static double travelVelocity(double distance, long time) {
        return distance / time;
    }

    public static long travelTime(double distance, double velocity) {
        return (long)Math.round(distance / velocity);
    }

    public static double cos(double n) {
        return Math.cos(Math.toRadians(n));
    }

    public static double sin(double n) {
        return Math.sin(Math.toRadians(n));
    }

    public static double acos(double n) {
        return Math.toDegrees(Math.acos(n));
    }

    public static double asin(double n) {
        return Math.toDegrees(Math.asin(n));
    }

    public static double atan(double n) {
        return Math.toDegrees(Math.atan(n));
    }

    public static double atan2(double xDelta, double yDelta) {
        return Math.toDegrees(Math.atan2(xDelta, yDelta));
    }

    public static int sign(double n) {
        return n < 0 ? -1 : 1;
    }

    public static double pointsToAngle(Point2D source, Point2D target) {
        double sourceX = source.getX();
        double sourceY = source.getY();
        double targetX = target.getX();
        double targetY = target.getY();
        return pointsToAngle(sourceX, sourceY, targetX, targetY);
    }

    public static double pointsToAngle(double sourceX, double sourceY, double targetX, double targetY) {
        double angle = atan((targetX - sourceX) / (targetY - sourceY));
        angle += targetY < sourceY ? 180 : 0;
        return angle;
    }

    public static void pointTranslate(Point2D target, Point2D translation) {
        target.setLocation(target.getX() + translation.getX(), target.getY() + translation.getY());
    }

    public static void pointTranslate(Point2D target, double x, double y) {
        target.setLocation(target.getX() + x, target.getY() + x);
    }

    public static void toLocation(double angle, double length, Point2D sourceLocation, Point2D targetLocation) {
        double X = sourceLocation.getX() + Rutils.sin(angle) * length;
        double Y = sourceLocation.getY() + Rutils.cos(angle) * length;
        targetLocation.setLocation(X, Y);
    }

    // Paul Evans' magic function
    public static double rollingAvg(double value, double newEntry, double n, double weighting ) {
        return (value * n + newEntry * weighting)/(n + weighting);
    } 

    public static Area escapeArea(Point2D gunLocation, Point2D targetLocation, double forwardAngle,
        double backwardAngle, Rectangle2D battleField, double bulletPower, double targetVelocity) {
        double distance = gunLocation.distance(targetLocation);
        double maxDistance = targetVelocity * travelTime(distance, bulletVelocity(bulletPower));
        double bearingToGun = pointsToAngle(targetLocation, gunLocation);
        Area escapeArea;
        Rectangle2D escapeRect;
        Point2D point = new Point2D.Double();
        Polygon escapePolygon = new Polygon();
        toLocation(bearingToGun, 5.0, targetLocation, point);
        escapePolygon.addPoint((int)point.getX(), (int)point.getY());
        toLocation(bearingToGun - forwardAngle, maxDistance, targetLocation, point);
        escapePolygon.addPoint((int)point.getX(), (int)point.getY());
        toLocation(bearingToGun - 90.0, maxDistance, targetLocation, point);
        escapePolygon.addPoint((int)point.getX(), (int)point.getY());
        toLocation(bearingToGun - (180.0 - backwardAngle), maxDistance, targetLocation, point);
        escapePolygon.addPoint((int)point.getX(), (int)point.getY());
        toLocation(bearingToGun - 180.0, 5.0, targetLocation, point);
        escapePolygon.addPoint((int)point.getX(), (int)point.getY());
        toLocation(bearingToGun + (180.0 - backwardAngle), maxDistance, targetLocation, point);
        escapePolygon.addPoint((int)point.getX(), (int)point.getY());
        toLocation(bearingToGun + 90.0, maxDistance, targetLocation, point);
        escapePolygon.addPoint((int)point.getX(), (int)point.getY());
        toLocation(bearingToGun + forwardAngle, maxDistance, targetLocation, point);
        escapePolygon.addPoint((int)point.getX(), (int)point.getY());
        escapeArea = new Area(escapePolygon);
        escapeRect = escapeArea.getBounds2D(); 
        escapeArea.intersect(new Area(battleField));
        escapeRect = escapeArea.getBounds2D(); 
        return escapeArea;
    }

    public static double[] escapeMinMaxAngles(Point2D gunLocation, Point2D targetLocation, Area escapeArea) {
        double[] angles = new double[2];
        double min = java.lang.Double.POSITIVE_INFINITY;
        double max = java.lang.Double.NEGATIVE_INFINITY;
        double bearingToTarget = pointsToAngle(gunLocation, targetLocation);
        PathIterator pathIterator = escapeArea.getPathIterator(null);
        double[] points = new double[6];
        Point2D point = new Point2D.Double();
        while (!pathIterator.isDone()) {
            int type = pathIterator.currentSegment(points);
            if (type != java.awt.geom.PathIterator.SEG_CLOSE) {
                point.setLocation(points[0], points[1]);
                double angle = pointsToAngle(gunLocation, point);
                angle = normalRelativeAngle(angle - bearingToTarget);
                if (angle < min) {
                    min = angle;
                }
                if (angle > max) {
                    max = angle;
                }
            }
            pathIterator.next();
        }
        angles[0] = min;
        angles[1] = max;
        return angles;
    }

    public static boolean isCornered(Point2D location, Rectangle2D field) {
        double m = 180;
        double mnX = m;
        double mnY = m;
        double mxX = field.getWidth() - m;
        double mxY = field.getHeight() - m;
        double x = location.getX();
        double y = location.getY();
        if ((x < mnX && (y < mnY || y > mxY)) || (x > mxX && (y < mnY || y > mxY))) {
            return true;
        }
        return false;
    }

    public static double normalRelativeAngle(double angle) {
        double relativeAngle = angle % 360;
        if (relativeAngle <= -180 )
            return 180 + (relativeAngle % 180);
        else if ( relativeAngle > 180 )
            return -180 + (relativeAngle % 180);
        else
            return relativeAngle;
    }

    public static double normalizeAngle(double angle) {
        while (angle < 0.0) {
            angle += 360;
        }
        return angle % 360;
    }
}
