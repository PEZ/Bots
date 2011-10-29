package pez.micro;
import robocode.*;
import java.awt.geom.*;
import java.awt.Color;

// BlackWidow - Small, black, shiny and deadly - By PEZ
//
// BlackWidow is open source under GPL-like conditions. Meaning you can use
// the code. Meaning you should feel obliged to share any improvements you do
// to the code. Meaning you must release your bots code if you directly use this
// code.
//
// Home page of this bot is: http://robowiki.dyndns.org/?BlackWidow
// The code should be available there and it is also the place for you to share any
// code improvements.
//
// $Id: BlackWidow.java,v 1.5 2003/08/20 08:15:58 peter Exp $

public class BlackWidow extends AdvancedRobot {
    static final int AIM_FACTORS = 17;
    static Rectangle2D fieldRectangle;
    static Point2D robotLocation = new Point2D.Double();
    static Point2D oldRobotLocation = new Point2D.Double();
    static Point2D enemyLocation = new Point2D.Double();
    static Point2D oldEnemyLocation = new Point2D.Double();
    static double enemyAbsoluteBearing;
    static double enemyDistance;
    static double enemyEnergy;
    static double enemyFirePower;
    static double enemyDeltaBearing;
    static double maxEnemyBearing;
    static double movementDirection = 1;
    static double[] aimFactors = new double[AIM_FACTORS];
    long nextTime;

    public void run() {
        fieldRectangle = new Rectangle2D.Double(0, 0 , getBattleFieldWidth(), getBattleFieldHeight());
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setColors(Color.black, Color.black, Color.red);
        turnRadarRightRadians(Double.POSITIVE_INFINITY); 
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        oldRobotLocation.setLocation(robotLocation);
        robotLocation.setLocation(getX(), getY());
        enemyAbsoluteBearing = getHeadingRadians() + e.getBearingRadians();
        enemyDistance = e.getDistance();
        oldEnemyLocation.setLocation(enemyLocation);
        toLocation(enemyAbsoluteBearing, enemyDistance, robotLocation, enemyLocation);
        double enemyEnergyLost = enemyEnergy - e.getEnergy();
        enemyEnergy = e.getEnergy();
        if (enemyEnergyLost >= 0.1 && enemyEnergyLost <= 3.0) {
            enemyFirePower = enemyEnergyLost;
        }

        gun(Math.min(3, Math.min(enemyEnergy / 4, getEnergy() / 5)));

        move();

        setTurnRadarLeftRadians(getRadarTurnRemaining()); 
    }

    void gun(double bulletPower) {
        enemyDeltaBearing = robocode.util.Utils.normalRelativeAngle(absoluteBearing(oldRobotLocation, enemyLocation) -
            absoluteBearing(oldRobotLocation, oldEnemyLocation));
        maxEnemyBearing = Math.abs(Math.asin(8 / bulletVelocity(bulletPower)));
        setTurnGunRightRadians(robocode.util.Utils.normalRelativeAngle(
            enemyAbsoluteBearing + maxEnemyBearing * sign(enemyDeltaBearing) * mostVisitedFactor(bulletPower) - getGunHeadingRadians()));
        setFireBullet(bulletPower);
        if (bulletPower > 2.5) {
            addCustomEvent(new VirtualBulletTrigger(bulletVelocity(bulletPower)));
        }
    }

    double mostVisitedFactor(double bulletPower) {
        int mostVisited = (AIM_FACTORS - 1) / 2;
        for (int i = 0; i < AIM_FACTORS; i++) {
            if (aimFactors[i] > aimFactors[mostVisited]) {
                mostVisited = i;
            }
        }
        return (mostVisited - AIM_FACTORS / 2) / (AIM_FACTORS / 2);
    }

    void move() {
        setMaxVelocity(Math.abs(getTurnRemaining()) < 40 ? 8 : 0);
        if (getTime() > nextTime && Math.random() < 0.5) {
            if (Math.random() < 0.5) {
                movementDirection *= -1;
            }
            nextTime = getTime() + (long)((enemyDistance / (bulletVelocity(enemyFirePower))) * Math.random());
        }
        Point2D destination = new Point2D.Double();
        toLocation(enemyAbsoluteBearing + Math.PI + 0.3 * movementDirection, enemyDistance * 1.1, enemyLocation, destination);
        translateInsideField(destination, 40);

        double angle = robocode.util.Utils.normalRelativeAngle(absoluteBearing(robotLocation, destination) - getHeadingRadians());
        int direction = 1;
        if (Math.abs(angle) > Math.PI / 2) {
            angle += Math.acos(direction = -1);
        }
        setTurnRightRadians(robocode.util.Utils.normalRelativeAngle(angle));
        setAhead(robotLocation.distance(destination) * direction);
    }

    double bulletVelocity(double bulletPower) {
        return 20 - 3 * bulletPower;
    }

    void translateInsideField(Point2D point, double margin) {
        point.setLocation(Math.max(margin, Math.min(fieldRectangle.getWidth() - margin, point.getX())),
                          Math.max(margin, Math.min(fieldRectangle.getHeight() - margin, point.getY())));
    }

    void toLocation(double angle, double length, Point2D sourceLocation, Point2D targetLocation) {
        targetLocation.setLocation(sourceLocation.getX() + Math.sin(angle) * length, sourceLocation.getY() + Math.cos(angle) * length);
    }

    double absoluteBearing(Point2D source, Point2D target) {
        return Math.atan2(target.getX() - source.getX(), target.getY() - source.getY());
    }

    int sign(double v) {
        return v > 0 ? 1 : -1;
    }

    class VirtualBulletTrigger extends Condition {
        private long vbTime;
        private double bulletVelocity;
        private Point2D vbRobotLocation = new Point2D.Double();
        private Point2D vbEnemyLocation = new Point2D.Double();
        private double vbDeltaBearing;
        private double vbMaxBearing;

        public VirtualBulletTrigger(double bulletVelocity) {
            this.vbTime = getTime();
            this.bulletVelocity = bulletVelocity;
            this.vbRobotLocation.setLocation(robotLocation);
            this.vbEnemyLocation.setLocation(enemyLocation);
            this.vbDeltaBearing = enemyDeltaBearing;
            this.vbMaxBearing = maxEnemyBearing;
        }

        public boolean test() {
            if (bulletVelocity * (getTime() - vbTime) > vbRobotLocation.distance(enemyLocation)) {
                double bearingDiff = robocode.util.Utils.normalRelativeAngle(absoluteBearing(vbRobotLocation, enemyLocation) -
                    absoluteBearing(vbRobotLocation, vbEnemyLocation));
                aimFactors[(int)(Math.max(0, Math.min(AIM_FACTORS - 1,
                    Math.round((((sign(vbDeltaBearing) * bearingDiff) / vbMaxBearing) / 2 + vbMaxBearing / 2) * (AIM_FACTORS - 1)))))]++;
                removeCustomEvent(this);
            }
            return false;
        }
    }
}
