package pez.movement;
import java.awt.geom.*;
import pez.Rutils;
import pez.Marshmallow;
import robocode.*;

// In the driver's seat of Marshmallow
// $Id: Driver.java,v 1.7 2003/06/11 09:36:52 peter Exp $

public class Driver implements pez.MarshmallowConstants {
    private Marshmallow robot;
    private Move currentMove;
    private double minX;
    private double maxX;
    private double minY;
    private double maxY;
    private double margin;
    private double heading;
    private double turnRemaining;
    private double distanceRemaining;
    private Point2D center;
    private double velocity;
    private long time;
    private double velocityChangeFactor = 0.12;
    private double velocityMaxFactor = 64;

    public Driver(Marshmallow robot) {
        this.robot = robot;
        this.currentMove = new Move(new Point2D.Double());
        setMargin(MC_WALL_MARGIN);
        time = robot.getTime();
        velocity = MC_MAX_ROBOT_VELOCITY;
    }

    public void setMargin(double margin) {
        this.margin = margin;
        this.minX = margin;
        this.maxX = robot.getBattleFieldWidth() - margin;
        this.minY = margin;
        this.maxY = robot.getBattleFieldHeight() - margin;
        center = new Point2D.Double(maxX / 2.0, maxY / 2.0);
    }

    public void drive() {
        if (Math.random() < velocityChangeFactor) {
            velocity = Math.min(MC_MAX_ROBOT_VELOCITY, Math.random() * velocityMaxFactor);
        }
        robot.setMaxVelocity(wallDanger() ? 0.1 : velocity);
        if (Math.abs(robot.getDistanceRemaining()) < 150) {
            goTo(currentMove.getDestination());
        }
    }

    private void moveRobot(Move move) {
        robot.setTurnRight(move.getAngle());
        robot.setAhead(move.getDistance());
    }

    public void goTo(Point2D destination) {
        currentMove = new Move(destination);
        moveRobot(currentMove);
    }

    public void suggestClosestCorner() {
        Point2D corner = new Point2D.Double(robot.getX() > maxX / 2 ? maxX : minX,
            robot.getY() > maxY / 2 ? maxY : minY);
        currentMove = new Move(corner);
    }

    public void reset() {
        currentMove = new Move(new Point2D.Double(robot.getX(), robot.getY()));
        setMargin(MC_WALL_MARGIN);
    }   
    
    private void adjustToBoundaries(Point2D location, double margin) {
        location.setLocation(Math.max(margin, Math.min(robot.getFieldRectangle().getWidth() - margin, location.getX())),
                          Math.max(margin, Math.min(robot.getFieldRectangle().getHeight() - margin, location.getY())));
    }

    boolean isFinished() {
        return Math.abs(robot.getDistanceRemaining()) < 20;
    }

    public Point2D getDestination() {
        return currentMove.getDestination();
    }

    public boolean wallDanger() {
        boolean danger;
        //Point2D sensingPoint = new Point2D.Double();
        //Rutils.toLocation(robot.getHeading(), 125, robot.getLocation(), sensingPoint);
        //danger = Math.abs(robot.getTurnRemaining()) > 35 && !robot.getFieldRectangle().contains(sensingPoint);
        danger = Math.abs(robot.getTurnRemaining()) > 35;
        return danger;
    }

    class Move {
        private Point2D destination;
        private double angle;
        private double distance;

        public Move(Point2D destination) {
            this.destination = destination;
            initDestination();
        }

        Point2D getDestination() {
            return this.destination;
        }

        double getAngle() {
            return this.angle;
        }

        double getDistance() {
            return this.distance;
        }

        private void initDestination() {
            adjustToBoundaries(destination, margin);
            angle = Rutils.normalRelativeAngle(Rutils.pointsToAngle(robot.getLocation(), destination) - robot.getHeading());
            distance = robot.getLocation().distance(destination);
            optimize();
        }

        private void optimize() {
            if (Math.abs(angle) > 90) {
                distance *= -1;
                if (angle > 0) {
                    angle -= 180;
                }
                else {
                    angle += 180;
                }
            }
        }
    }
}
