package pez.mako;
import robocode.*;
import java.awt.geom.*;
import java.awt.Color;
import java.io.*;
import java.util.*;

// Mako, by PEZ. Prey sighted - prey eaten.
// $Id: Mako.java,v 1.27 2003/08/20 08:15:58 peter Exp $

public class Mako extends AdvancedRobot {
    private static Bot enemy;
    private static Bot me;

    // movement
    private static final double DEFAULT_DISTANCE = 485;
    private static final double MAX_VELOCITY = 8;
    private static Rectangle2D fieldRectangle;
    private static Rectangle2D fluffedFieldRectangle;
    private static double velocity = 8;
    private double accumulatedAngle;
    private Point2D oldEnemyLocation;
    private Point2D oldRobotLocation;

    private boolean haveEnemy;
    private double enemyDistance;
    private double enemyEnergy;
    private String enemyName;
    private double enemyBulletPower = 3;
    private double absoluteBearing;
    private double deltaBearing;
    private double advancingVelocity;
    private boolean roundOver = false;
    private static boolean statsRestored = false;
    private static boolean isOneOnOne;
    private static int totalEncounters;
    private static RatingComparator ratingComparator = new RatingComparator();

    private static int wins;
    private static long enemyShots;
    private static long enemyHits;
    private static long hits;

    public void run() {
        if (me == null) {
            me = new Bot(getName());
            fieldRectangle = new Rectangle2D.Double(0, 0 , getBattleFieldWidth(), getBattleFieldHeight());
            fluffedFieldRectangle = new Rectangle2D.Double(-125, -125 , getBattleFieldWidth() + 125, getBattleFieldHeight() + 125);
            setColors(Color.gray, Color.gray, Color.black);
        }
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        isOneOnOne = getOthers() == 1;

        double velocityChangeFactor = Math.random() < 0.5 ? 0.07 : 0.16;
        double velocityMaxFactor = Math.random() < 0.5 ? 48 : 80;

        while (true) {
            if (Math.random() < velocityChangeFactor) {
                velocity = Math.min(8, Math.random() * velocityMaxFactor);
            }
            setMaxVelocity(Math.abs(getTurnRemaining()) > 45 ? 0.1 : velocity);
            if (!haveEnemy) {
                setTurnRadarLeft(22.5);
            }
            haveEnemy = false;
            if (getOthers() == 0) {
                move();
            }
            if (getOthers() > 0 && enemyEnergy > 0.0 &&
                (getEnergy() > 0.3 || (enemyDistance < 200 && (enemyEnergy < 1.0 || enemyEnergy > 10)))) {
                Bullet bullet = setFireBullet(bulletPower(enemyEnergy));
                if (bullet != null) {
                    addCustomEvent(new CheckVirtualGunsCondition(bullet, deltaBearing, enemyDistance, me, enemy, this));
                }
            }
            execute();
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        double radarTurn;
        enemyName = e.getName();
        if (enemy == null) {
            enemy = createEnemy(e.getName());
            restoreStats(e.getName());
        }
        me.updateLocation(getX(), getY());
        absoluteBearing = getHeading() + e.getBearing();
        advancingVelocity = -Math.cos(Math.toRadians(e.getHeading() - absoluteBearing)) * e.getVelocity();
        double enemyEnergyDelta = enemyEnergy - e.getEnergy();
        if (enemyEnergyDelta >= 0.1 && enemyEnergyDelta <= 3.0) {
            enemyShots++;
            enemyBulletPower = enemyEnergyDelta;
        }
        enemyEnergy = e.getEnergy();
        enemyDistance = e.getDistance();
        // Refactor enemyLocation stuff later
        Point2D enemyLocation = new Point2D.Double();
        toLocation(absoluteBearing, enemyDistance, me.getLocation(), enemyLocation);
        enemy.updateLocation(enemyLocation.getX(), enemyLocation.getY());
        deltaBearing = rollingAvg(deltaBearing, absoluteBearing(me.getOldLocation(), enemy.getLocation()) -
                                  absoluteBearing(me.getOldLocation(), enemy.getOldLocation()), 5, 1);
        haveEnemy = true;
        radarTurn = normalRelativeAngle(getHeading() + e.getBearing() - getRadarHeading()) * 1.6;
        setTurnRadarRight(radarTurn);
        aimGun();
        move();
    }

    public void onHitByBullet(HitByBulletEvent e) {
        enemyHits++;
    }

    public void onBulletHit(BulletHitEvent e) {
        hits++;
    }

    public void onWin(WinEvent e) {
        wins++;
        if (!roundOver) {
            printStats();
            saveStats(enemyName);
        }
        roundOver = true;
    }

    public void onDeath(DeathEvent e) {
        if (!roundOver) {
            printStats();
            saveStats(enemyName);
        }
        roundOver = true;
    }

    /*
    public void onHitWall(HitWallEvent e) {
        System.out.println("Ouch!");
    }
    */

    private void move() {
        if (!(oldEnemyLocation == null) && Math.abs(getDistanceRemaining()) < 20) {
            Point2D destination = new Point2D.Double();
            double enemyBulletVelocity = 20 - 3 * enemyBulletPower;
            double maxRelativeAngle = Math.toDegrees(Math.asin(8 / enemyBulletVelocity));
            double deltaAngle = absoluteBearing(oldEnemyLocation, me.getLocation()) -
                absoluteBearing(oldEnemyLocation, oldRobotLocation);
            accumulatedAngle += deltaAngle;
            double relativeAngle = distanceFactor() * (maxRelativeAngle * 2 * Math.random() - maxRelativeAngle);
            double absoluteDestinationAngle = Math.abs(accumulatedAngle + relativeAngle);
            if (absoluteDestinationAngle > maxRelativeAngle) {
                relativeAngle = sign(relativeAngle) * (maxRelativeAngle - Math.abs(accumulatedAngle));
            }
            else if (absoluteDestinationAngle < maxRelativeAngle / 3) {
                relativeAngle *= 1.7 * sign(deltaAngle) * (Math.random() < 0.5 ? -0.2 : 1);
            }
            double distanceExtra = distanceExtra() * Math.abs(relativeAngle);
            toLocation(absoluteBearing + 180 + relativeAngle, enemyDistance + distanceExtra, enemy.getLocation(), destination);
            if (!fluffedFieldRectangle.contains(destination)) {
                toLocation(absoluteBearing + 180 - relativeAngle, enemyDistance + distanceExtra, enemy.getLocation(), destination);
            }
            translateInsideField(destination, 35);
            oldEnemyLocation.setLocation(enemy.getLocation());
            oldRobotLocation.setLocation(me.getLocation());
            goTo(destination);
        }
        else {
            oldEnemyLocation = new Point2D.Double();
            oldEnemyLocation.setLocation(enemy.getLocation());
            oldRobotLocation = new Point2D.Double();
            oldRobotLocation.setLocation(me.getLocation());
        }
    }

    // make this work with Bot instances later
    private double distanceFactor() {
        if (enemyDistance < 190) {
            return 0.60;
        }
        if (enemyDistance < 250) {
            return 0.61;
        }
        if (enemyDistance > 500) {
            return 0.68;
        }
        return 0.635;
    }

    // make this work with Bot instances later
    private double distanceExtra() {
        double distanceExtra = 3;
        if (enemyEnergy == 0 && getOthers() == 1) {
            distanceExtra = -12;
        }
        /*
        else if (robot.isLoosing(enemy)) {
            distanceExtra = -3;
        }
        */
        else if (isCornered(me) && !isCornered(enemy)) {
            distanceExtra = -1;
        }
        else if (enemyDistance < 100) {
            distanceExtra = 12;
        }
        else if (enemyDistance > DEFAULT_DISTANCE) {
            distanceExtra = -1;
        }
        return distanceExtra;
    }

    Bot createEnemy (String name) {
        Bot bot = new Bot(name);
        VirtualGun[][][] guns = { 
            // SEGMENT_LEFT
            {
                // SEGMENT_CLOSE
                {
                    new DirectGun(),
                    new LaserGun(100),
                    new AngularFactoredGun(10), new AngularFactoredGun(30), new AngularFactoredGun(70),
                    new BearingOffsetGun(10), new BearingOffsetGun(30), new BearingOffsetGun(70)
                },
                // SEGMENT_NORMAL
                {
                    new DirectGun(),
                    new LaserGun(100),
                    new AngularFactoredGun(10), new AngularFactoredGun(30), new AngularFactoredGun(70),
                    new BearingOffsetGun(10), new BearingOffsetGun(30), new BearingOffsetGun(70)
                },
                // SEGMENT_FAR
                {
                    new DirectGun(),
                    new LaserGun(100),
                    new AngularFactoredGun(10), new AngularFactoredGun(30), new AngularFactoredGun(70),
                    new BearingOffsetGun(10), new BearingOffsetGun(30), new BearingOffsetGun(70)
                }
            },
            // SEGMENT_STRAIGHT
            {
                // SEGMENT_CLOSE
                {
                    new DirectGun(),
                    new LaserGun(100),
                    new AngularFactoredGun(10), new AngularFactoredGun(30), new AngularFactoredGun(70),
                    new BearingOffsetGun(10), new BearingOffsetGun(30), new BearingOffsetGun(70)
                },
                // SEGMENT_NORMAL
                {
                    new DirectGun(),
                    new LaserGun(100),
                    new AngularFactoredGun(10), new AngularFactoredGun(30), new AngularFactoredGun(70),
                    new BearingOffsetGun(10), new BearingOffsetGun(30), new BearingOffsetGun(70)
                },
                // SEGMENT_FAR
                {
                    new DirectGun(),
                    new LaserGun(100),
                    new AngularFactoredGun(10), new AngularFactoredGun(30), new AngularFactoredGun(70),
                    new BearingOffsetGun(10), new BearingOffsetGun(30), new BearingOffsetGun(70)
                }
            },
            // SEGMENT_RIGHT
            {
                // SEGMENT_CLOSE
                {
                    new DirectGun(),
                    new LaserGun(100),
                    new AngularFactoredGun(10), new AngularFactoredGun(30), new AngularFactoredGun(70),
                    new BearingOffsetGun(10), new BearingOffsetGun(30), new BearingOffsetGun(70)
                },
                // SEGMENT_NORMAL
                {
                    new DirectGun(),
                    new LaserGun(100),
                    new AngularFactoredGun(10), new AngularFactoredGun(30), new AngularFactoredGun(70),
                    new BearingOffsetGun(10), new BearingOffsetGun(30), new BearingOffsetGun(70)
                },
                // SEGMENT_FAR
                {
                    new DirectGun(),
                    new LaserGun(100),
                    new AngularFactoredGun(10), new AngularFactoredGun(30), new AngularFactoredGun(70),
                    new BearingOffsetGun(10), new BearingOffsetGun(30), new BearingOffsetGun(70)
                }
            }
        };
        bot.setVirtualGuns(guns);
        return bot;
    }

    private int sign(double v) {
        if (v > 0) {
            return +1;
        }
        if (v < 0) {
            return -1;
        }
        return 0;
    }

    private double bulletPower(double enemyEnergy) {
        double power = 2 + Math.random() * 30;
        power = Math.min(power, 1800 / enemyDistance);
        power = Math.min(power, enemyEnergy >= 4 ? 
            (enemyEnergy + 2) / 6 : enemyEnergy / 4);
        power = Math.min(power, getEnergy() / 4);
        return power;
    }

    private void aimGun() {
        double absoluteBearing = absoluteBearing(me.getLocation(), enemy.getLocation());
        double guessedDistance = enemyDistance;
        double guessedBearing = absoluteBearing;
        if (guessedDistance > 60.0) {
            VirtualGun[] guns = enemy.getVirtualGuns(deltaBearing, enemyDistance);
            Arrays.sort(guns, ratingComparator);
            VirtualGun gun = guns[0];
            if (gun instanceof FactorGun) {
                guessedBearing = ((FactorGun)gun).guessedBearing(absoluteBearing, deltaBearing);
            }
        }
        Point2D impactLocation = new Point2D.Double();
        toLocation(guessedBearing, guessedDistance, me.getLocation(), impactLocation);
        translateInsideField(impactLocation, 1);
        guessedBearing = absoluteBearing(me.getLocation(), impactLocation);
        setTurnGunRight(normalRelativeAngle(guessedBearing - getGunHeading()));
    }

    private void goTo(Point2D point) {
        double distance = me.getLocation().distance(point);
        double angle = normalRelativeAngle(absoluteBearing(me.getLocation(), point) - getHeading());
        if (Math.abs(angle) > 90) {
            distance *= -1;
            if (angle > 0) {
                angle -= 180;
            }
            else {
                angle += 180;
            }
        }
        setTurnRight(angle);
        setAhead(distance);
    }

    // Move this to Bot class
    private boolean isCornered(Bot bot) {
        double m = 140;
        double mnX = m;
        double mnY = m;
        double mxX = fieldRectangle.getWidth() - m;
        double mxY = fieldRectangle.getHeight() - m;
        double x = bot.getLocation().getX();
        double y = bot.getLocation().getY();
        if ((x < mnX && (y < mnY || y > mxY)) || (x > mxX && (y < mnY || y > mxY))) {
            return true;
        }
        return false;
    }

    private static double bulletVelocity(double power) {
        return 20 - 3 * power;
    }

    private static double travelTime(double distance, double velocity) {
        return distance / velocity;
    }

    private void translateInsideField(Point2D point, double margin) {
        point.setLocation(Math.max(margin, Math.min(fieldRectangle.getWidth() - margin, point.getX())),
                          Math.max(margin, Math.min(fieldRectangle.getHeight() - margin, point.getY())));
    }

    static void toLocation(double angle, double length, Point2D sourceLocation, Point2D targetLocation) {
        targetLocation.setLocation(sourceLocation.getX() + Math.sin(Math.toRadians(angle)) * length,
                                   sourceLocation.getY() + Math.cos(Math.toRadians(angle)) * length);
    }

    static double absoluteBearing(Point2D source, Point2D target) {
        return Math.toDegrees(Math.atan2(target.getX() - source.getX(), target.getY() - source.getY()));
    }

    static double normalRelativeAngle(double angle) {
        double relativeAngle = angle % 360;
        if (relativeAngle <= -180 )
            return 180 + (relativeAngle % 180);
        else if ( relativeAngle > 180 )
            return -180 + (relativeAngle % 180);
        else
            return relativeAngle;
    }

    public static double rollingAvg(double value, double newEntry, double n, double weighting ) {
        return (value*n + newEntry*weighting)/(n + weighting);
    } 

    private double percentageRounded(double observations, double population) {
        return Math.round(10000.0 * observations / population) / 100.0;
    }

    private double percentageRounded(long observations, long population) {
        return percentageRounded((double)observations, (double)population);
    }
    
    private double percentageRounded(double observations, long population) {
        return percentageRounded(observations, (double)population);
    }

    private double percentageRounded(long observations, double population) {
        return percentageRounded((double)observations, population);
    }
    
    private void printStats() {
        totalEncounters++;
        System.out.println("> " + hits + " / " + me.getShotCount() + " = " +
            percentageRounded(hits, me.getShotCount()) + "%");
        System.out.println("< " + enemyHits + " / " + enemyShots + " = " +
            percentageRounded(enemyHits, enemyShots) + "%");
        /*
        VirtualGun[][][] guns = enemy.getVirtualGuns();
        for (int segment1 = 0; segment1 < guns.length; segment1++) {
            for (int segment2 = 0; segment2 < guns[segment1].length; segment2++) {
                Arrays.sort(guns[segment1][segment2], ratingComparator);
                for (int gun = 0; gun < guns[segment1][segment2].length; gun++) {
                    VirtualGun vg = guns[segment1][segment2][gun];
                    System.out.println(segment1 + ":" + segment2 + ":" +
                        vg.getID() + " (" + vg.getUses() + ") " + " - " + vg.getRating());
                }
            }
        }
        if (getRoundNum() == getNumRounds() - 1) {
            System.out.println("Total encounters: " + totalEncounters);
        }
        */
        System.out.println("Wins: " + wins + " (" + percentageRounded(wins, getRoundNum() +1) + "%)");
    }

    private void saveStats(String enemyName) {
        try {
            PrintStream w = new PrintStream(new RobocodeFileOutputStream(getDataFile(fileName(enemyName))));
            w.println(totalEncounters);
            if (w.checkError()) {
                out.println("I could not write the stats!");
            }
            w.close();
        }
        catch (Exception e) {
                out.println("An exception occured:" + e);
        }
    }

    private void restoreStats(String enemyName) {
        try {
            BufferedReader r = new BufferedReader(new FileReader(getDataFile(fileName(enemyName))));
            totalEncounters = Integer.parseInt(r.readLine());
        } catch (Exception e) {
            out.println("Hmmm, maybe this guy is new in town: " + e);
        }
        statsRestored = true;
    }

    private String fileName(String name) {
        int index = name.indexOf(" ");
        String fName;
        if (index != -1) {
            fName = name.substring(0, index);
        }
        else {
            fName = name;
        }
        if (!isOneOnOne) {
            fName = fName + "-Melee";
        }
        return fName;
    }
}

class CheckVirtualGunsCondition extends Condition {
    private long time;
    private VirtualGun[] guns;
    private Bot target;
    private Bot gunner;
    private AdvancedRobot robot;
    private double bulletVelocity;
    private double bulletPower;
    private Point2D oldRLocation = new Point2D.Double();
    private Point2D oldELocation = new Point2D.Double();
    private Point2D impactLocation = new Point2D.Double();
    private Point2D guessedLocation = new Point2D.Double();
    private double oldBearing;
    private double oldBearingDelta;

    public CheckVirtualGunsCondition(Bullet bullet, double bearingDelta, double enemyDistance,
                                     Bot gunner, Bot target, AdvancedRobot robot) {
        this.time = robot.getTime();
        this.bulletVelocity = bullet.getVelocity();
        this.bulletPower = bullet.getPower();
        this.gunner = gunner;
        this.target = target;
        this.robot = robot;
        this.oldBearingDelta = bearingDelta;
        this.guns = target.getVirtualGuns(oldBearingDelta, enemyDistance);
        this.oldRLocation.setLocation(gunner.getLocation());
        this.oldELocation.setLocation(target.getLocation());
        this.oldBearing = Mako.absoluteBearing(oldRLocation, oldELocation);
    }

    public boolean test() {
        if (robot.getOthers() == 0) {
            return false;
        }
        double bulletDistance = bulletVelocity * (robot.getTime() - time);
        if (bulletDistance > oldRLocation.distance(target.getLocation()) - 10) {
            gunner.incrementShotCount();
            if (Math.abs(oldBearingDelta) > 0.05) {
                double impactBearing = Mako.absoluteBearing(oldRLocation, target.getLocation());
                Mako.toLocation(impactBearing, bulletDistance, oldRLocation, impactLocation);
                double bearingDiff = Mako.normalRelativeAngle(impactBearing - oldBearing);
                double factor = bearingDiff / oldBearingDelta;
                for (int i = 0; i < guns.length; i++) {
                    VirtualGun gun = guns[i];
                    if (gun instanceof FactorGun) {
                        ((FactorGun)gun).updateFactor(factor, bearingDiff, bulletPower);
                    }
                    // Consider moving this test into the virtual guns
                    Mako.toLocation(((FactorGun)gun).guessedBearing(oldBearing, oldBearingDelta),
                                    bulletDistance, oldRLocation, guessedLocation);
                    if (impactLocation.distance(guessedLocation) < 20) {
                        gun.updateRating(100, bulletPower);
                    }
                    else {
                        gun.updateRating(0, bulletPower);
                    }
                }
            }
            robot.removeCustomEvent(this);
        }
        return false;
    }
}

class Bot {
    private static final int SEGMENT_LEFT       = 0;
    private static final int SEGMENT_STRAIGHT   = 1;
    private static final int SEGMENT_RIGHT      = 2;
    private static final int SEGMENT_CLOSE  = 0;
    private static final int SEGMENT_NORMAL    = 1;
    private static final int SEGMENT_FAR = 2;
    private String name;
    private Point2D location = new Point2D.Double();
    private Point2D oldLocation = new Point2D.Double();
    private VirtualGun[][][] virtualGuns;
    private long shotCount;
    
    Bot(String name) {
        this.name = name;
    }

    void setVirtualGuns(VirtualGun[][][] guns) {
        this.virtualGuns = guns;
    }

    VirtualGun[][][] getVirtualGuns() {
        return this.virtualGuns;
    }

    VirtualGun[] getVirtualGuns(double deltaBearing, double enemyDistance) {
        return this.virtualGuns[getBearingSegment(deltaBearing)][getDistanceSegment(enemyDistance)];
    }

    void updateLocation(double x, double y) {
        oldLocation.setLocation(location);
        location.setLocation(x, y);
    }

    Point2D getLocation() {
        return this.location;
    }

    Point2D getOldLocation() {
        return this.oldLocation;
    }

    void incrementShotCount() {
        this.shotCount++;
    }

    long getShotCount() {
        return this.shotCount;
    }

    private int getBearingSegment(double deltaBearing) {
        int segment = SEGMENT_STRAIGHT;
        if (deltaBearing < -0.3) {
            segment = SEGMENT_LEFT;
        }
        else if (deltaBearing > 0.3) {
            segment = SEGMENT_RIGHT;
        }
        return segment;
    }

    private int getDistanceSegment(double enemyDistance) {
        int segment = SEGMENT_NORMAL;
        if (enemyDistance < 350) {
            segment = SEGMENT_CLOSE;
        }
        else if (enemyDistance > 500) {
            segment = SEGMENT_FAR;
        }
        return segment;
    }
}

/*
class Target {
    private Bot gunner;
    private Bot target;
    private double absoluteBearing;
    private double distance;
    private double deltaBearing;
    private double advancingVelocity;
    private double lateralVelocity;
    private double[] guessedBearing;
    private boolean haveGuessedBearing;

    Target(Bot gunner, Bot target) {
        this.gunner = gunner;
        this.target = target;
        this.distance = gunner.distance(target);
        this.absoluteBearing = gunner.absoluteBearing(target);
        this.advancingVelocity = gunner.advancingVelocity(target);
        this.lateralVelocity = gunner.lateralVelocity(target);
    }

    double getDistance() {
        return this.distance();
    }

    void setDeltaBearing(double delta) {
        this.deltaBearing = delta;
    }

    double getAbsoluteBearing() {
        return this.absoluteBearing;
    }

    double getDistanceSegment() {
        return this.advancingVelocity;
    }

    double[] getGuessedBearing() {
    }
}
*/

interface Rateable extends Comparable {
    double getRating();
}

class RatingComparator implements Comparator {
    public int compare(Object a, Object b) {
        if (((Rateable)a).getRating() > ((Rateable)b).getRating()) return(-1);
        if (((Rateable)a).getRating() == ((Rateable)b).getRating()) return(0);
        return(1);
    }
}

class TuningFactor implements Rateable {
    private int id;
    private double value;
    private int useLength;
    private int uses;
    private double rating = 50;
    private long ratingDepth = 200;
    private static int totFactors;

    TuningFactor(double value, int useLength) {
        this.id = totFactors++;
        this.value = value;
        this.useLength = useLength;
    }

    public boolean equals(Object o) {
        if (o instanceof TuningFactor) {
            return ((TuningFactor)this).getID() == ((TuningFactor)o).getID();
        }
        return false;
    }

    public int compareTo(Object o) {
        TuningFactor tf = (TuningFactor) o;
        if (this.getID() < tf.getID()) {
            return -1;
        }
        if (this.getID() < tf.getID()) {
            return +1;
        }
        return 0;
    }

    public double getRating() {
        return this.rating;
    }

    void updateRating(double rating, double weight) {
        this.rating = Mako.rollingAvg(this.rating, rating, Math.min(++uses, ratingDepth), weight);
    }

    boolean isFinished() {
        return uses >= useLength;
    }

    int getID() {
        return this.id;
    }

    int getUses() {
        return this.uses;
    }

    void setUses(int uses) {
        this.uses = uses % useLength;
    }

    double getValue() {
        return this.value;
    }
}

abstract class VirtualGun implements Rateable {
    private int id;
    private int uses;
    private double rating;
    private double ratings;
    private static int vgCount;
    private static final double VG_RATING_DEPTH = 1000;

    public VirtualGun() {
        this.id = vgCount++;
    }

    public int compareTo(Object o) {
        VirtualGun vg = (VirtualGun) o;
        if (this.getID() < vg.getID()) {
            return -1;
        }
        if (this.getID() < vg.getID()) {
            return +1;
        }
        return 0;
    }

    void updateRating(double rating, double weight) {
        uses++;
        this.rating = Mako.rollingAvg(this.rating, rating, Math.min(++ratings, VG_RATING_DEPTH), weight);
    }

    void setRating(double rating) {
        this.rating = rating;
    }

    public double getRating() {
        return this.rating;
    }

    int getUses() {
        return this.uses;
    }

    int getID() {
        return this.id;
    }
}

abstract class FactorGun extends VirtualGun {
    double factor;
    double factorDepth;
    long factorUpdates;

    FactorGun() {
    }

    FactorGun(double factorDepth) {
        this.factorDepth = factorDepth;
    }

    abstract double guessedBearing(double bearing, double delta);

    abstract void updateFactor(double factor, double bearingDiff, double weight);

    double getFactor() {
        return factor;
    }
}

class DirectGun extends FactorGun {
    void updateFactor(double factor, double bearingDiff, double weight) {
    }

    double guessedBearing(double bearing, double delta) {
        return bearing;
    }
}

/*
class AngularFactoredGun extends FactorGun {
    AngularFactoredGun(double factorDepth) {
        super(factorDepth);
    }

    void updateFactor(double factor, double bearingDiff, double weight) {
        this.factor = Mako.rollingAvg(this.factor, factor, Math.min(++factorUpdates, factorDepth), weight);
    }

    double guessedBearing(double bearing, double delta) {
        return bearing + delta * factor;
    }
}
*/

class BearingOffsetGun extends FactorGun {
    BearingOffsetGun(double factorDepth) {
        super(factorDepth);
    }

    void updateFactor(double factor, double bearingDiff, double weight) {
        this.factor = Mako.rollingAvg(this.factor, bearingDiff, Math.min(++factorUpdates, factorDepth), weight);
    }

    double guessedBearing(double bearing, double delta) {
        return bearing + factor;
    }
}

class LaserGun extends FactorGun {
    private static RatingComparator ratingComparator = new RatingComparator();
    TuningFactor[] sectors = new TuningFactor[21];
    double sectorWidth = 90 / sectors.length;

    LaserGun(double factorDepth) {
        super(factorDepth);
        for (int i = 0; i < sectors.length; i++) {
            sectors[i] = new TuningFactor(-45 + (i * sectorWidth), 1);
        }
    }

    void updateFactor(double factor, double bearingDiff, double weight) {
        for (int i = 0; i < sectors.length; i++) {
            if ((bearingDiff - sectors[i].getValue()) <= sectorWidth / 0.8) {
                sectors[i].updateRating(100, weight);
            }
            else {
                sectors[i].updateRating(0, weight);
            }
        }
    }

    double guessedBearing(double bearing, double delta) {
        Arrays.sort(sectors, ratingComparator);
        return bearing + sectors[0].getValue();
    }
}

class AngularFactoredGun extends FactorGun {
    private static RatingComparator ratingComparator = new RatingComparator();
    TuningFactor[] sectors = new TuningFactor[21];
    double sectorWidth = 100 / sectors.length;

    AngularFactoredGun(double factorDepth) {
        super(factorDepth);
        for (int i = 0; i < sectors.length; i++) {
            sectors[i] = new TuningFactor(-50 + (i * sectorWidth), 1);
        }
    }

    void updateFactor(double factor, double bearingDiff, double weight) {
        for (int i = 0; i < sectors.length; i++) {
            if ((factor - sectors[i].getValue()) <= sectorWidth / 0.8) {
                sectors[i].updateRating(100, weight);
            }
            else {
                sectors[i].updateRating(0, weight);
            }
        }
    }

    double guessedBearing(double bearing, double delta) {
        Arrays.sort(sectors, ratingComparator);
        return bearing + delta * sectors[0].getValue();
    }
}

class PatternMatcherGun extends VirtualGun {
    StringBuffer pattern = new StringBuffer(10000);

    double guessedBearing(double bearing) {
        return bearing;
    }
}
