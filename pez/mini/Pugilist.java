package pez.mini;

import robocode.*;
import robocode.util.Utils;
import java.awt.geom.*;

// This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
// http://robowiki.net/?RWPCL
// (Basically it means I like credit.)
//
// Pugilist, by PEZ. Although a pugilist needs strong and accurate fists, he/she even more needs an evasive movement.
//
// Pugilist explores two major concepts:
//    1. Guess factor targeting, invented by Paul Evans. http://robowiki.net/?GuessFacorTargeting
//    2. Wave surfing movement, invented by ABC. http://robowiki.net/?WaveSurfing
//
// Many thanks to Jim, Kawigi, iiley, Jamougha, Axe, ABC, rozu, Kuuran, FnH, nano and many others who have helped me.
// Check out http://robowiki.net/?Members to get an idea about who those people are. =)
//
// $Id: Pugilist.java,v 1.38 2004/03/19 12:32:28 peter Exp $

public class Pugilist extends AdvancedRobot {
    static final double MAX_VELOCITY = 8;
    static final double BOT_WIDTH = 36;
    static final double BATTLE_FIELD_WIDTH = 800;
    static final double BATTLE_FIELD_HEIGHT = 600;

    static final double MAX_WALL_SMOOTH_TRIES = 150;
    static final double WALL_MARGIN = 20;

    static final double MAX_DISTANCE = 900;
    static final double MAX_BULLET_POWER = 3.0;
    static final double BULLET_POWER = 1.9;

    static final int FACTORS = 75;
    static final double[] X_SLICES = { 100, 225, 400, 575, 700 };
    static final double[] Y_SLICES = { 75, 200, 300, 400, 525 };
    static final double[] VELOCITY_SLICES = { -5, -2, 2, 5 };
    static int[][][][][][] gunStatBuffer = new int[X_SLICES.length+1][Y_SLICES.length+1]
            [X_SLICES.length+1][Y_SLICES.length+1][VELOCITY_SLICES.length+1][FACTORS];
    static int[][][][][][] moveStatBuffer = new int[X_SLICES.length+1][Y_SLICES.length+1]
            [X_SLICES.length+1][Y_SLICES.length+1][VELOCITY_SLICES.length+1][FACTORS];

    static Rectangle2D fieldRectangle = new Rectangle2D.Double(WALL_MARGIN, WALL_MARGIN,
            BATTLE_FIELD_WIDTH - WALL_MARGIN * 2, BATTLE_FIELD_HEIGHT - WALL_MARGIN * 2);
    
    static Point2D robotLocation = new Point2D.Double();
    static Point2D enemyLocation = new Point2D.Double();
    static double enemyAbsoluteBearing;
    static double enemyDistance;
    static int rxIndex;
    static int ryIndex;
    static int exIndex;
    static int eyIndex;
    static int rLateralVelocityIndex;
    static int eLateralVelocityIndex;
    static double enemyVelocity;
    double enemyEnergy;
    static int enemyTimeSinceVChange;
    static double enemyBearingDirection;

    static double enemyFirePower = 2.5;
    static int lastRobotVelocityIndex;
    static double robotVelocity;

    public void run() {
        setAdjustRadarForGunTurn(true);
        setAdjustGunForRobotTurn(true);

        EnemyWave.passingWave = null;

        do {
            turnRadarRightRadians(Double.POSITIVE_INFINITY);
        } while (true);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        Wave wave = new Wave();
        wave.robot = this;
        EnemyWave ew = new EnemyWave();
        ew.robot = this;
        ew.gunLocation = (Point2D) enemyLocation.clone();
        ew.targetLocation = (Point2D) robotLocation.clone();
        ew.startBearing = ew.gunBearing(robotLocation);

        double enemyDeltaEnergy = enemyEnergy - e.getEnergy();
        if (enemyDeltaEnergy > 0 && enemyDeltaEnergy <= 3.0) {
            enemyFirePower = enemyDeltaEnergy;
            addCustomEvent(ew);
        }
        enemyEnergy = e.getEnergy();
        ew.bulletVelocity = 20 - 3 * enemyFirePower;

        double direction = robotBearingDirection(ew.startBearing);
        ew.bearingDirection = Math.asin(MAX_VELOCITY / ew.bulletVelocity)
                * direction / (double) EnemyWave.MIDDLE_FACTOR;

        double rLateralVelocity = robotVelocity * Math.sin(getHeadingRadians() - ew.startBearing);
        ew.visits = moveStatBuffer[rxIndex = index(X_SLICES, robotLocation.getX())]
                [ryIndex = index(Y_SLICES, robotLocation.getY())]
                [exIndex = index(X_SLICES, enemyLocation.getX())]
                [eyIndex = index(X_SLICES, enemyLocation.getY())]
                [index(VELOCITY_SLICES, rLateralVelocity)];

        double eLateralVelocity = enemyVelocity * Math.sin(e.getHeadingRadians() - enemyAbsoluteBearing);
        wave.visits = moveStatBuffer[rxIndex]
                [ryIndex]
                [exIndex]
                [eyIndex]
                [index(VELOCITY_SLICES, eLateralVelocity)];

        robotVelocity = getVelocity();

        robotLocation.setLocation(new Point2D.Double(getX(), getY()));
        enemyAbsoluteBearing = getHeadingRadians() + e.getBearingRadians();
        enemyLocation.setLocation(project(
                wave.gunLocation = (Point2D) robotLocation.clone(),
                enemyAbsoluteBearing, enemyDistance));
        wave.targetLocation = enemyLocation;
        enemyDistance = e.getDistance();

        ew.advance(2);

        // <gun>
        if (enemyVelocity != (enemyVelocity = e.getVelocity())) {
            enemyTimeSinceVChange = 0;
        }

        // double bulletPower = MAX_BULLET_POWER; // TargetingChallenge
        double bulletPower = Math.min(enemyEnergy / 4,
                e.getDistance() > 120 ? BULLET_POWER : MAX_BULLET_POWER);
        wave.bulletVelocity = 20 - 3 * bulletPower;

        if (enemyVelocity != 0) {
            enemyBearingDirection = 0.7 * sign(enemyVelocity
                    * Math.sin(e.getHeadingRadians() - enemyAbsoluteBearing));
        }
        wave.bearingDirection = enemyBearingDirection
                / (double) Wave.MIDDLE_FACTOR;

        wave.startBearing = enemyAbsoluteBearing;

        setTurnGunRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing
                - getGunHeadingRadians() + wave.bearingDirection
                * (wave.mostVisited() - Wave.MIDDLE_FACTOR)));

        addCustomEvent(wave);
        if (getEnergy() >= BULLET_POWER
                && Math.abs(getGunTurnRemainingRadians()) < Math.atan2(
                        BOT_WIDTH / 2, enemyDistance)) {
            setFire(bulletPower);
        }
        // </gun>

        setMaxVelocity((EnemyWave.dangerStop < Math.min(EnemyWave.dangerReverse, EnemyWave.dangerForward)) ? 0 : MAX_VELOCITY);
        if (EnemyWave.dangerReverse < EnemyWave.dangerForward) {
            direction = -direction;
        }
        double angle;
        setAhead(Math.cos(angle = wave.gunBearing(wallSmoothedDestination(
                robotLocation, direction)) - getHeadingRadians()) * 100);
        setTurnRightRadians(Math.tan(angle));

        setTurnRadarRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing
                - getRadarHeadingRadians()) * 2);
        EnemyWave.dangerForward = EnemyWave.dangerReverse = EnemyWave.dangerStop = 0;
    }

    public void onHitByBullet(HitByBulletEvent e) {
        EnemyWave.passingWave.registerVisits();
    }

    static int index(double[] slices, double v) {
        for (int i = 0; i < slices.length; i++) {
            if (v < slices[i]) {
                return i;
            }
        }
        return slices.length;
    }

    static int wallIndex(Wave wave) {
        int wallIndex = 0;
        do {
            wallIndex++;
        } while (wallIndex < (Wave.WALL_INDEXES)
                && fieldRectangle.contains(project(wave.gunLocation,
                        wave.startBearing + wave.bearingDirection
                                * (double) (wallIndex * 5.5), enemyDistance)));
        return wallIndex - 1;
    }

    static Point2D wallSmoothedDestination(Point2D location, double direction) {
        Point2D destination = new Point2D.Double();
        int tries = 0;
        while (tries < 2) {
            double currentSmoothing = 0;
            while (currentSmoothing < 100 && !fieldRectangle.contains(destination = project(location, absoluteBearing(location, enemyLocation) -
                direction*(Math.PI / 2 + 0.2 - (currentSmoothing++ / 100.0)), enemyDistance / 5.0)));
            direction -= direction;
            tries++;
            if (currentSmoothing < 45) {
                break;
            }
        }
        return destination;
    }

    void updateDirectionStats(EnemyWave wave) {
        EnemyWave.dangerReverse += wave
                .danger(waveImpactLocation(wave, -1.0, 5));
        EnemyWave.dangerStop += wave
                .danger(waveImpactLocation(wave, 0.0, 3));
        EnemyWave.dangerForward += wave
                .danger(waveImpactLocation(wave, 1.0, 0));
    }

    Point2D waveImpactLocation(EnemyWave wave, double direction, int timeOffset) {
        Point2D impactLocation = (Point2D) robotLocation.clone();
        do {
            impactLocation = project(
                    impactLocation,
                    absoluteBearing(
                            impactLocation,
                            wallSmoothedDestination(
                                    impactLocation,
                                    direction
                                            * robotBearingDirection(wave
                                                    .gunBearing(robotLocation)))),
                    MAX_VELOCITY);
            timeOffset++;
        } while (wave.distanceFromTarget(impactLocation, timeOffset) > -10);
        return impactLocation;
    }

    double robotBearingDirection(double enemyBearing) {
        return sign(getVelocity()
                * Math.sin(getHeadingRadians() - enemyBearing));
    }

    static Point2D project(Point2D sourceLocation, double angle, double length) {
        return new Point2D.Double(sourceLocation.getX() + Math.sin(angle)
                * length, sourceLocation.getY() + Math.cos(angle) * length);
    }

    static double absoluteBearing(Point2D source, Point2D target) {
        return Math.atan2(target.getX() - source.getX(),
                target.getY() - source.getY());
    }

    static int sign(double v) {
        return v < 0 ? -1 : 1;
    }

    static double minMax(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}

class Wave extends Condition {
    static final int DISTANCE_INDEXES = 5;
    static final int VELOCITY_INDEXES = 5;
    static final int WALL_INDEXES = 4;
    static final int VCHANGE_TIME_INDEXES = 6;
    static final int MIDDLE_FACTOR = (Pugilist.FACTORS - 1) / 2;


    static int[] fastHits = new int[Pugilist.FACTORS];

    Pugilist robot;
    double bulletVelocity;
    Point2D gunLocation;
    Point2D targetLocation;
    double startBearing;
    double bearingDirection;
    int[] visits;
    double distanceFromGun;

    public boolean test() {
        advance(1);
        if (passed(-18)) {
            if (robot.getOthers() > 0) {
                registerVisits();
            }
            robot.removeCustomEvent(this);
        }
        return false;
    }

    public boolean passed(double distanceOffset) {
        return distanceFromGun > gunLocation.distance(targetLocation)
                + distanceOffset;
    }

    void advance(int ticks) {
        distanceFromGun += ticks * bulletVelocity;
    }

    int visitingIndex(Point2D target) {
        return (int) Pugilist.minMax(
                Math.round(((Utils.normalRelativeAngle(gunBearing(target)
                        - startBearing)) / bearingDirection)
                        + MIDDLE_FACTOR), 0, Pugilist.FACTORS - 1);
    }

    void registerVisits() {
        int index = visitingIndex(targetLocation);
        visits[index]++;
        fastHits[index]++;
    }

    double gunBearing(Point2D target) {
        return Pugilist.absoluteBearing(gunLocation, target);
    }

    double distanceFromTarget(Point2D location, int timeOffset) {
        return gunLocation.distance(location) - distanceFromGun
                - (double) timeOffset * bulletVelocity;
    }

    int mostVisited() {
        int mostVisited = MIDDLE_FACTOR, i = Pugilist.FACTORS - 1;
        do {
            if (visits[--i] > visits[mostVisited]) {
                mostVisited = i;
            }
        } while (i > 0);
        return mostVisited;
    }
}

class EnemyWave extends Wave {
    static double dangerForward;
    static double dangerReverse;
    static double dangerStop;
    static EnemyWave passingWave;

    public boolean test() {
        advance(1);
        if (passed(-18)) {
            passingWave = this;
        }
        if (passed(15)) {
            robot.removeCustomEvent(this);
        }
        robot.updateDirectionStats(this);
        return false;
    }

    double danger(Point2D destination) {
        double smoothed = 0;
        int i = 0;
        do {
            smoothed += ((double) (fastHits[i]) + (double) visits[i] * 100000.0)
                    / Math.sqrt((Math.abs(visitingIndex(destination) - i) + 1.0));
            i++;
        } while (i < Pugilist.FACTORS);
        return smoothed / Math.abs(distanceFromTarget(targetLocation, 0))
                / bulletVelocity;
    }
}