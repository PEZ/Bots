package pez.mini;
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
// $Id: BlackWidow.java,v 1.2 2003/08/20 08:15:58 peter Exp $

public class BlackWidow extends AdvancedRobot {
    static final int AIM_FACTORS = 27;
    private static final int ACCEL_SEGMENTS = 3;
    private static final int DISTANCE_SEGMENTS = 4;
    static Rectangle2D fieldRectangle;
    static Point2D robotLocation = new Point2D.Double();
    static Point2D lastRobotLocation = new Point2D.Double();
    static Point2D enemyLocation = new Point2D.Double();
    static Point2D lastEnemyLocation = new Point2D.Double();
    static double enemyAbsoluteBearing;
    static double enemyDistance;
    static double enemyEnergy;
    static double enemyFirePower;
    static double enemyDeltaBearing;
    static double lastEnemyDeltaBearing;
    static double maxEnemyBearing;
    static double movementDirection = 1;
    private static int[][][] aimFactors = new int[DISTANCE_SEGMENTS][ACCEL_SEGMENTS][AIM_FACTORS];
    private static int aimAccelSegment;
    private static int aimDistanceSegment;
    private static int aimPowerSegment;
    long nextTime;
    long timeSinceLastScan;
    long timeSinceEnemyFired = 100;

    public void run() {
        fieldRectangle = new Rectangle2D.Double(0, 0 , getBattleFieldWidth(), getBattleFieldHeight());
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        setColors(Color.black, Color.black, Color.red);

        do {
            doScanner();
            execute();
        } while (true);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        timeSinceEnemyFired++;
        lastRobotLocation.setLocation(robotLocation);
        robotLocation.setLocation(getX(), getY());
        enemyAbsoluteBearing = getHeadingRadians() + e.getBearingRadians();
        enemyDistance = e.getDistance();
        lastEnemyLocation.setLocation(enemyLocation);
        toLocation(enemyAbsoluteBearing, enemyDistance, robotLocation, enemyLocation);
        double enemyEnergyLost = enemyEnergy - e.getEnergy();
        enemyEnergy = e.getEnergy();
        if (enemyEnergyLost >= 0.1 && enemyEnergyLost <= 3.0) {
            enemyFirePower = enemyEnergyLost;
            timeSinceEnemyFired = 0;
        }

        gun(Math.min(3, Math.min(enemyEnergy / 4, getEnergy() / 5)));

        move();

        timeSinceLastScan = 0;
    }

    void gun(double bulletPower) {
        lastEnemyDeltaBearing = enemyDeltaBearing;
        enemyDeltaBearing = robocode.util.Utils.normalRelativeAngle(absoluteBearing(lastRobotLocation, enemyLocation) -
            absoluteBearing(lastRobotLocation, lastEnemyLocation));
        aimDistanceSegment = Math.min((int)(enemyDistance / (getBattleFieldWidth() / DISTANCE_SEGMENTS)), DISTANCE_SEGMENTS - 1);
        aimAccelSegment = aimAccelSegment();
        maxEnemyBearing = Math.abs(Math.asin(8 / bulletVelocity(bulletPower)));
        setTurnGunRightRadians(robocode.util.Utils.normalRelativeAngle(
            enemyAbsoluteBearing + maxEnemyBearing * sign(enemyDeltaBearing) * mostVisitedFactor() - getGunHeadingRadians()));
        Bullet bullet = setFireBullet(bulletPower);
        if (bullet != null && bullet.getPower() > 2.5) {
            addCustomEvent(new VirtualBulletTrigger(bulletVelocity(bulletPower)));
        }
    }

    double mostVisitedFactor() {
        int mostVisited = (AIM_FACTORS - 1) / 2;
        for (int i = 0; i < AIM_FACTORS; i++) {
            if (aimFactors[aimDistanceSegment][aimAccelSegment][i] > aimFactors[aimDistanceSegment][aimAccelSegment][mostVisited]) {
                mostVisited = i;
            }
        }
        return (mostVisited - (AIM_FACTORS - 1D) / 2D) / ((AIM_FACTORS - 1D) / 2D);
    }

    void move() {
        boolean isDodging = false;
        double bulletTravelTime = enemyDistance / bulletVelocity(enemyFirePower);
        Point2D destination = new Point2D.Double();
        toLocation(enemyAbsoluteBearing + Math.PI + 0.3 * movementDirection, enemyDistance * (bulletTravelTime < 30 ? 1.1 : 0.9),
            enemyLocation, destination);
        double wantedTravelDistance = robotLocation.distance(destination);
        translateInsideField(destination, 40);
        if (robotLocation.distance(destination) < wantedTravelDistance * 0.6) {
            movementDirection *= -1;
            isDodging = true;
        }
        else if (((timeSinceEnemyFired == 0) || getTime() > nextTime) &&
                Math.random() < Math.max(0.2, Math.pow(enemyFirePower, 2.3) / 12.51)) {
            if (Math.random() < 0.3) {
                movementDirection *= -1;
                isDodging = true;
            }
            nextTime = getTime() + (long)(bulletTravelTime * Math.random() * (isDodging ? 1.0 : 0.33));
        }

        double angle = robocode.util.Utils.normalRelativeAngle(absoluteBearing(robotLocation, destination) - getHeadingRadians());
        int direction = 1;
        if (Math.abs(angle) > Math.PI / 2) {
            angle += Math.acos(direction = -1);
        }
        setTurnRightRadians(robocode.util.Utils.normalRelativeAngle(angle));
        setAhead(robotLocation.distance(destination) * direction);
        setMaxVelocity(Math.abs(getTurnRemaining()) < 45 ? 8 : 0);
    }

    private void doScanner() {
        double radarOffset = Double.POSITIVE_INFINITY;
        if(getOthers() == 1 && timeSinceLastScan < 2) {
            radarOffset = robocode.util.Utils.normalRelativeAngle(getRadarHeadingRadians() - enemyAbsoluteBearing);
            radarOffset += sign(radarOffset) * 0.02;
        }
        setTurnRadarLeftRadians(radarOffset);
        timeSinceLastScan++;
    }

    private int aimAccelSegment() {
        if (enemyDeltaBearing < lastEnemyDeltaBearing) {
            return 0;
        }
        else if (enemyDeltaBearing > lastEnemyDeltaBearing) {
            return 2;
        }
        return 1;
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
        private int vbAccelSegment;
        private int vbDistanceSegment;

        public VirtualBulletTrigger(double bulletVelocity) {
            this.vbTime = getTime();
            this.bulletVelocity = bulletVelocity;
            this.vbRobotLocation.setLocation(robotLocation);
            this.vbEnemyLocation.setLocation(enemyLocation);
            this.vbDeltaBearing = enemyDeltaBearing;
            this.vbMaxBearing = maxEnemyBearing;
            this.vbAccelSegment = aimAccelSegment;
            this.vbDistanceSegment = aimDistanceSegment;
        }

        public boolean test() {
            if (bulletVelocity * (getTime() - vbTime) > vbRobotLocation.distance(enemyLocation)) {
                double bearingDiff = robocode.util.Utils.normalRelativeAngle(absoluteBearing(vbRobotLocation, enemyLocation) -
                    absoluteBearing(vbRobotLocation, vbEnemyLocation));
                aimFactors[vbDistanceSegment][vbAccelSegment][(int)Math.round(Math.max(0D, Math.min(AIM_FACTORS - 1D,
                    ((sign(vbDeltaBearing) * bearingDiff) / vbMaxBearing) *
                    (AIM_FACTORS - 1D) / 2D + (AIM_FACTORS - 1D) / 2D)))]++;
                removeCustomEvent(this);
            }
            return false;
        }
    }
}
