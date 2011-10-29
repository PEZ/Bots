package pez;
import java.awt.geom.*;
import pez.Rutils.*;
import robocode.*;

// In the driver's seat of Marshmallow
// $Id: Driver.java,v 1.28 2004/02/20 09:55:35 peter Exp $

class Driver implements MarshmallowConstants {
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
    private Point2D destination = new Point2D.Double();

    public Driver(Marshmallow robot) {
        this.robot = robot;
        this.currentMove = new Move(0.0, 0.0, 0);
        setMargin(MC_WALL_MARGIN);
        time = robot.getTime();
        velocity = MC_MAX_ROBOT_VELOCITY;
    }

    void setMargin(double margin) {
        this.margin = margin;
        this.minX = margin;
        this.maxX = robot.getBattleFieldWidth() - margin;
        this.minY = margin;
        this.maxY = robot.getBattleFieldHeight() - margin;
        center = new Point2D.Double(maxX / 2.0, maxY / 2.0);
    }

    void drive() {
        time = robot.getTime();
        heading = robot.getHeading();
        turnRemaining = robot.getTurnRemaining();
        distanceRemaining = robot.getDistanceRemaining();
        if (Math.random() < 0.3 && distanceRemaining > 10) {
            double adjustTurning = Math.random() * (Math.random() < 0.5 ? 10 : -10);
            currentMove = new Move(heading + turnRemaining + adjustTurning, distanceRemaining, currentMove.weight);
            moveRobot();
        }
        if (isFinished()) {
            currentMove.setWeight(0);
        }
        destination = currentMove.getDestination();
    }

    private void moveRobot() {
        robot.setTurnRight(currentMove.getAngle());
        robot.setAhead(currentMove.getDistance());
    }

    Point2D getCenter() {
        return this.center;
    }

    Point2D getDestination() {
        return this.destination;
    }

    boolean isFinished() {
        return (Math.abs(distanceRemaining) < Math.random() * 75);
    }

    boolean isFinished(int weight) {
        return weight > currentMove.weight || isFinished();
    }

    void reset() {
        this.currentMove = new Move(0.0, 0.0, 0);
        moveRobot();
    }

    void suggest(Point2D destination, int weight) {
        if (weight >= currentMove.weight) {
            currentMove = new Move(destination, weight);
            moveRobot();
        }
    }

    void suggest(double angle, double distance, int weight) {
        if (weight > currentMove.weight) {
            currentMove = new Move(angle, distance, weight);
            moveRobot();
        }
    }

    void suggestClosestCorner(int weight) {
        if (weight > currentMove.weight) {
            Point2D corner = new Point2D.Double(robot.getX() > maxX / 2 ? maxX : minX,
                robot.getY() > maxY / 2 ? maxY : minY);
            currentMove = new Move(corner, weight);
            moveRobot();
        }
    }

    void adjustToBoundaries(Point2D location) {
        double m = Math.random() * margin * 2;
        double x = location.getX();
        double y = location.getY();
        if (x > maxX + m) {
            x -= x - maxX + m;
        }
        if (x < minX - m) {
            x += x + minX + m;
        }
        if (y > maxY + m) {
            y -= y - maxY + m;
        }
        if (y < minY - m) {
            y += y + minY + m;
        }
        x = Math.min(x, maxX);
        x = Math.max(x, minX);
        y = Math.min(y, maxY);
        y = Math.max(y, minY);
        location.setLocation(x, y);
    }

    class Move {
        private Point2D destination;
        private double angle;
        private double distance;
        private int weight;

        public Move(Point2D destination, int weight) {
            this.destination = destination;
            this.weight = weight;
            initDestination();
        }

        public Move(double angle, double distance, int weight) {
            setDestination(angle, distance);
            this.weight = weight;
            initDestination();
        }

        void blend(Move move) {
            double totWeight = this.weight + move.weight;
            double newAngle = (this.angle * this.weight + move.angle * move.weight) / totWeight;
            double newDistance = (this.distance * this.weight + move.distance * move.weight) / totWeight;
            setDestination(newAngle, newDistance);
            initDestination();
        }

        void update(double angle, double distance) {
            setDestination(angle, distance);
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

        int getWeight() {
            return this.weight;
        }

        void setWeight(int weight) {
            this.weight = weight;
        }

        private void setDestination(double angle, double distance) {
            this.destination = new Point2D.Double(robot.getX() + Rutils.sin(angle) * distance, robot.getY() + Rutils.cos(angle) * distance);
        }

        private void initDestination() {
            adjustToBoundaries(destination);
            angle = Rutils.normalRelativeAngle(Rutils.pointsToAngle(robot.getLocation(), destination) - robot.getHeading());
            distance = robot.getLocation().distance(destination);
            optimize();
        }

        private void optimize() {
            int direction = 1;
            if (Math.abs(angle) > 90) {
                direction = -1;
                if (angle > 90) {
                    angle -= 180;
                }
                else if (angle < -90) {
                    angle += 180;
                }
            }
            distance *= direction;
        }
    }
}
