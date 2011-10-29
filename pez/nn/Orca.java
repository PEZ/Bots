package pez.nn;
import robocode.*;
import pez.nn.nrlibj.*;
import java.awt.geom.*;
import java.awt.Polygon;
import java.awt.Color;
import java.util.*;

// Orca - Trying to be smart about things... - by PEZ
// $Id: Orca.java,v 1.20 2003/04/23 10:44:41 peter Exp $

public class Orca extends AdvancedRobot {
    private static Random rand = new Random();
    private static Point2D location = new Point2D.Double();
    private static Point2D oldLocation = new Point2D.Double();
    private static Point2D enemyLocation = new Point2D.Double();
    private static Point2D oldEnemyLocation = new Point2D.Double();
    private static Point2D centerLocation = new Point2D.Double();
    private static Rectangle2D fieldRectangle;
    private static double wantedDistance = 575;
    private static double maxX;
    private static double maxY;
    private boolean haveEnemy;
    private static String enemyName;
    private double enemyDistance;
    private double enemyDistanceFromCenter;
    private double enemyDistanceDelta;
    private double enemyHeading;
    private double enemyHeadingDelta;
    private double enemyEnergy;
    private double enemyEnergyDelta;
    private double enemyVelocity;
    private double rollingEnemyVelocity;
    private double absoluteBearing;
    private double enemyBearingDelta;
    private double rollingEnemyBearingDelta;
    private double bulletPower;
    private double velocity = 8;
    private double enemyBulletPower = 3;
    private long roundNum;
    private boolean roundOver;
    private int waitBeforeRam;
    private double timeSinceLastFire;
    private static long time;
    private static double meanOffsetFactor;
    private static double meanAimFactor;
    private static long wins;
    private static long shots;
    private static long hits;
    private static long skippedTurns;
    private static long enemyShots;
    private static long enemyHits;

    private double timeDelta;
    private double minVelocity = 1;
    private static double maxDistance;
    private static double maxDistanceFromCenter;
    private static double maxVelocity = 8;
    private static double maxEnemyBearingDiff = 48;
    private static double maxEnemyBearingDelta = 7;
    private static double maxEnemyDistanceDelta = 11;
    private static double maxHeadingDelta = 6;
    private static double maxTimeBetweenFires;
    private final static int HISTORY_DEPTH = 2;
    private final static int HISTORY_ITEMS = 4;
    private static LinkedList nnHistoryList = new LinkedList();
    private float[] nnHistoryArray = new float[HISTORY_DEPTH * HISTORY_ITEMS + 1];
    private float[] nnCorrectAnswer = new float[1];
    private float[] nnAnswer = new float[1];
    private final static String NNdescr2[] = {
        "layer=0 tnode=6 nname=NodeLin",
        "layer=1 tnode=18 nname=NodeSigm",
        "layer=2 tnode=12 nname=NodeSigm",
        "layer=3 tnode=1 nname=NodeSigm",
        "linktype=all fromlayer=0 tolayer=1",
        "linktype=all fromlayer=1 tolayer=2",
        "linktype=all fromlayer=2 tolayer=3"
    };
    private final static String NNdescr[] = {
        "layer=0 tnode=9 nname=NodeLin",
        "layer=1 tnode=5 nname=NodeSigm",
        "layer=2 tnode=1 nname=NodeSigm",
        "linktype=all fromlayer=0 tolayer=1",
        "linktype=all fromlayer=1 tolayer=2"
    };
    private static pez.nn.nrlibj.NNet nnet;
    private static int nnTrainings;
    private static double nnError = 1;

    public void run() {
        if (fieldRectangle == null) {
            roundNum = getRoundNum();
            fieldRectangle = new Rectangle2D.Double(0, 0 , getBattleFieldWidth(), getBattleFieldHeight());
            setColors(Color.black, Color.white, Color.white);
            maxDistance = (new Point2D.Double(18,18)).distance(
                new Point2D.Double(fieldRectangle.getWidth() - 18, fieldRectangle.getHeight() - 18));
            centerLocation.setLocation(fieldRectangle.getWidth() / 2, fieldRectangle.getHeight() / 2);
            maxDistanceFromCenter = centerLocation.distance(new Point2D.Double(18,18));
            maxX = (fieldRectangle.getWidth() - 40);
            maxY = (fieldRectangle.getHeight() - 40);
            maxTimeBetweenFires = 3 / getGunCoolingRate();
        }
        waitBeforeRam = 100;
        timeSinceLastFire = maxTimeBetweenFires;
        roundOver = false;
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        while (true) {
            setVelocity();
            setMaxVelocity(Math.abs(getTurnRemaining()) > 45 ? 0.1 : velocity);
            if (!haveEnemy) {
                setTurnRadarLeft(22.5);
            }
            if (getOthers() == 0) {
                dodge();
            }
            timeSinceLastFire++;
            haveEnemy = false;
            if (enemyEnergy > 0.0 && (getEnergy() > 0.3 || (enemyDistance < 200 && (enemyEnergy < 1.0 || enemyEnergy > 10)))) {
                Bullet bullet = setFireBullet(bulletPower);
                if (bullet != null) {
                    timeSinceLastFire = 0;
                    addCustomEvent(new CheckUpdateFactors(bulletPower));
                }
            }
            execute();
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        double radarTurn;
        enemyName = e.getName();
        if (nnet == null) {
            try {
                nnet = new pez.nn.nrlibj.NNet(fileName(enemyName), true, this);
                nnTrainings = 250;
            }
            catch (Exception ex) {
                pez.nn.nrlibj.NrPop.setSeed();
                nnet = new pez.nn.nrlibj.NNet(NNdescr);
            }
        }
        oldLocation.setLocation(location);
        location.setLocation(getX(), getY());
        oldEnemyLocation.setLocation(enemyLocation);
        absoluteBearing = getHeading() + e.getBearing();
        enemyEnergyDelta = enemyEnergy - e.getEnergy();
        enemyEnergy = e.getEnergy();
        enemyVelocity = e.getVelocity();
        enemyDistance = e.getDistance();
        toLocation(absoluteBearing, enemyDistance, location, enemyLocation);
        enemyDistanceFromCenter = centerLocation.distance(enemyLocation);
        setBulletPower(enemyEnergy);
        timeDelta = getTime() - time;
        if (timeDelta > 0) {
            enemyBearingDelta = normalRelativeAngle(absoluteBearing(oldLocation, enemyLocation) -
                absoluteBearing(oldLocation, oldEnemyLocation)) / timeDelta;
            enemyDistanceDelta = (oldLocation.distance(oldEnemyLocation) - oldLocation.distance(enemyLocation)) / timeDelta;
            enemyHeadingDelta = (enemyHeading - e.getHeading()) / timeDelta;
            time = getTime();
            if (enemyBearingDelta <= maxEnemyBearingDelta) {
                record();
            }
        }
        enemyHeading= e.getHeading();
        haveEnemy = true;
        radarTurn = normalRelativeAngle(getHeading() + e.getBearing() - getRadarHeading()) * 2;
        setTurnRadarRight(radarTurn);
        if (getGunHeat() / getGunCoolingRate() < 2) {
            aimGun();
        }
        else {
            setTurnGunRight(normalRelativeAngle(absoluteBearing - getGunHeading()));
        }
        if (enemyEnergy == 0 && getOthers() == 1) {
            if (waitBeforeRam-- == 0) {
                goTo(enemyLocation);
            }
        }
        if (enemyEnergyDelta >= 0.1 && enemyEnergyDelta <= 3.0) {
            enemyShots++;
            enemyBulletPower = enemyEnergyDelta;
        }
        dodge();
    }

    public void onBulletHit(BulletHitEvent e) {
        shots++;
        hits++;
    }

    public void onBulletMissed(BulletMissedEvent e) {
        if (getOthers() > 0) {
            shots++;
        }
    }

    public void onHitByBullet(HitByBulletEvent e) {
        enemyHits++;
    }

    public void onWin(WinEvent e) {
        wins++;
        if (!roundOver) {
            printStats();
            saveNN(fileName(enemyName));
        }
        roundOver = true;
    }

    public void onDeath(DeathEvent e) {
        if (!roundOver) {
            printStats();
            saveNN(fileName(enemyName));
        }
        roundOver = true;
    }

    public void onSkippedTurn(SkippedTurnEvent e) {
        skippedTurns++;
    }

    private void setVelocity() {
        if (Math.random() < 0.02) {
            velocity = minVelocity + Math.random() * (maxVelocity - minVelocity);
        }
        if (enemyDistance < 250) {
            velocity = maxVelocity;
        }
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
        System.out.println("Wins: " + wins + " (" + percentageRounded(wins, getRoundNum() +1) + "%)");
        System.out.println("> " + hits + " / " + shots + " = " +
            percentageRounded(hits, shots) + "%");
        System.out.println("< " + enemyHits + " / " + enemyShots + " = " +
            percentageRounded(enemyHits, enemyShots) + "%");
        System.out.println("nnTrainings: " + nnTrainings);
        System.out.println("nnError: " + nnError);
        //System.out.println("skipped: " + skippedTurns);
    }

    private void saveNN(String fileName) {
       nnet.saveNNet(fileName, this);
    }

    private String fileName(String name) {
        int index = name.indexOf(" ");
        if (index != -1) {
            return name.substring(0, index) + ".nn";
        }
        else {
            return name + ".nn";
        }
    }

    private void dodge() {
        if (Math.abs(getDistanceRemaining()) < Math.random() * 75) {
            double forwardAngle = 85.0;
            double wantedDistanceExtra;
            if (getEnergy() / enemyEnergy > 9.0) {
                wantedDistanceExtra = -0.35;
            }
            else {
                if (enemyDistance < wantedDistance) {
                    wantedDistanceExtra = 0.3;
                }
                else {
                    wantedDistanceExtra = -0.1;
                }
            }
            if (isCornered()) {
                wantedDistanceExtra = -0.3;
                forwardAngle = 60.0;
            }
            Area escapeArea = escapeArea(enemyLocation, location, forwardAngle, 85.0,
                fieldRectangle, enemyBulletPower);
            double[] minMaxAngles = escapeMinMaxAngles(enemyLocation, location, escapeArea);
            double relativeAngle = minMaxAngles[0] + (minMaxAngles[1] - minMaxAngles[0]) * Math.random();
            double wantedDistance = enemyDistance + enemyDistance * wantedDistanceExtra * Math.abs(relativeAngle) / 40.0;
            double angle = absoluteBearing(enemyLocation, location) + relativeAngle;
            Point2D dLocation = new Point2D.Double();
            toLocation(angle, wantedDistance, enemyLocation, dLocation);
            translateInsideField(dLocation, 35.0);
            double distance = location.distance(dLocation);
            minVelocity = location.distance(dLocation) / travelTime(enemyLocation.distance(dLocation), bulletVelocity(enemyBulletPower));
            goTo(dLocation);
        }
    }

    private Area escapeArea(Point2D gunLocation, Point2D targetLocation, double forwardAngle,
        double backwardAngle, Rectangle2D battleField, double bulletPower) {
        double distance = gunLocation.distance(targetLocation);
        double maxTravelDistance = maxVelocity * travelTime(distance, bulletVelocity(bulletPower));
        double bearingToGun = absoluteBearing(targetLocation, gunLocation);
        Area escapeArea;
        Point2D point = new Point2D.Double();
        Polygon escapePolygon = new Polygon();
        toLocation(bearingToGun, 5.0, targetLocation, point);
        escapePolygon.addPoint((int)point.getX(), (int)point.getY());
        toLocation(bearingToGun - forwardAngle, maxTravelDistance, targetLocation, point);
        escapePolygon.addPoint((int)point.getX(), (int)point.getY());
        toLocation(bearingToGun - 90.0, maxTravelDistance, targetLocation, point);
        escapePolygon.addPoint((int)point.getX(), (int)point.getY());
        toLocation(bearingToGun - (180.0 - backwardAngle), maxTravelDistance, targetLocation, point);
        escapePolygon.addPoint((int)point.getX(), (int)point.getY());
        toLocation(bearingToGun - 180.0, 5.0, targetLocation, point);
        escapePolygon.addPoint((int)point.getX(), (int)point.getY());
        toLocation(bearingToGun + (180.0 - backwardAngle), maxTravelDistance, targetLocation, point);
        escapePolygon.addPoint((int)point.getX(), (int)point.getY());
        toLocation(bearingToGun + 90.0, maxTravelDistance, targetLocation, point);
        escapePolygon.addPoint((int)point.getX(), (int)point.getY());
        toLocation(bearingToGun + forwardAngle, maxTravelDistance, targetLocation, point);
        escapePolygon.addPoint((int)point.getX(), (int)point.getY());
        escapeArea = new Area(escapePolygon);
        escapeArea.intersect(new Area(battleField));
        return escapeArea;
    }

    private double[] escapeMinMaxAngles(Point2D gunLocation, Point2D targetLocation, Area escapeArea) {
        double[] angles = new double[2];
        double min = java.lang.Double.POSITIVE_INFINITY;
        double max = java.lang.Double.NEGATIVE_INFINITY;
        double bearingToTarget = absoluteBearing(gunLocation, targetLocation);
        PathIterator pathIterator = escapeArea.getPathIterator(null);
        double[] points = new double[6];
        Point2D point = new Point2D.Double();
        while (!pathIterator.isDone()) {
            int type = pathIterator.currentSegment(points);
            if (type != java.awt.geom.PathIterator.SEG_CLOSE) {
                point.setLocation(points[0], points[1]);
                double angle = absoluteBearing(gunLocation, point);
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

    private boolean isCornered() {
        double m = 180;
        double mnX = m;
        double mnY = m;
        double mxX = fieldRectangle.getWidth() - m;
        double mxY = fieldRectangle.getHeight() - m;
        double x = location.getX();
        double y = location.getY();
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

    private void setBulletPower(double enemyEnergy) {
        bulletPower = 3;
        bulletPower = Math.min(bulletPower, enemyEnergy >= 4 ? 
            (enemyEnergy + 2) / 6 : enemyEnergy / 4);
        bulletPower = Math.min(bulletPower, getEnergy() / 3);
        bulletPower = Math.min(bulletPower, 1500 / enemyDistance);
    }

    private void aimGun() {
        double guessedDistance = location.distance(enemyLocation);
        double guessedHeading = absoluteBearing(location, enemyLocation);
        if (Math.abs(enemyBearingDelta) > 0.05) {
            guessedHeading += enemyBearingDelta * meanAimFactor;
        }
        else {
            guessedHeading += meanOffsetFactor;
        }
        if (nnTrainings > 10) {
            nnet.frwNNet(nnHistoryArray, nnAnswer);
            guessedHeading += (0 - maxEnemyBearingDiff) + maxEnemyBearingDiff * 2 * nnAnswer[0];
        }
        guessedHeading += rand.nextGaussian();
        Point2D impactLocation = new Point2D.Double();
        toLocation(guessedHeading, guessedDistance, location, impactLocation);
        guessedHeading = absoluteBearing(location, impactLocation);
        setTurnGunRight(normalRelativeAngle(guessedHeading - getGunHeading()));
    }

    private void goTo(Point2D point) {
        double distance = location.distance(point);
        double angle = normalRelativeAngle(absoluteBearing(location, point) - getHeading());
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

    private void translateInsideField(Point2D point, double margin) {
        point.setLocation(Math.max(margin, Math.min(fieldRectangle.getWidth() - margin, point.getX())),
                          Math.max(margin, Math.min(fieldRectangle.getHeight() - margin, point.getY())));
    }

    private static void toLocation(double angle, double length, Point2D sourceLocation, Point2D targetLocation) {
        targetLocation.setLocation(sourceLocation.getX() + Math.sin(Math.toRadians(angle)) * length,
                                   sourceLocation.getY() + Math.cos(Math.toRadians(angle)) * length);
    }

    private static double absoluteBearing(Point2D source, Point2D target) {
        return Math.toDegrees(Math.atan2(target.getX() - source.getX(), target.getY() - source.getY()));
    }

    private static double normalRelativeAngle(double angle) {
        double relativeAngle = angle % 360;
        if (relativeAngle <= -180 )
            return 180 + (relativeAngle % 180);
        else if ( relativeAngle > 180 )
            return -180 + (relativeAngle % 180);
        else
            return relativeAngle;
    }

    private static double rollingAvg(double value, double newEntry, double n, double weighting ) {
        return (value * n + newEntry * weighting)/(n + weighting);
    } 

    private static float nnNormalizedValue(double value, double max) {
        return (float)((value + max) / (2 * max));
    }

    private boolean record() {
        rollingEnemyBearingDelta = rollingAvg(rollingEnemyBearingDelta, enemyBearingDelta,
            maxTimeBetweenFires, 1);
        rollingEnemyVelocity = rollingAvg(rollingEnemyVelocity, enemyBearingDelta,
            maxTimeBetweenFires, 1);
        Area escapeArea = escapeArea(location, enemyLocation, 80.0, 80.0,
            fieldRectangle, bulletPower);
        double[] minMaxAngles = escapeMinMaxAngles(location, enemyLocation, escapeArea);
        /*
        //nnHistoryList.addLast(new Double(nnNormalizedValue(enemyDistanceDelta, maxEnemyDistanceDelta)));
        //nnHistoryList.addLast(new Double(nnNormalizedValue(enemyHeadingDelta, maxHeadingDelta)));
        //nnHistoryList.addLast(new Double(nnNormalizedValue(enemyDistanceDelta, maxEnemyDistanceDelta)));
        nnHistoryList.addLast(new Double(location.getX() / maxX));
        nnHistoryList.addLast(new Double(location.getY() / maxY));
        nnHistoryList.addLast(new Double(enemyLocation.getX() / maxX));
        nnHistoryList.addLast(new Double(enemyLocation.getY() / maxY));
        */
        nnHistoryList.addLast(new Double(nnNormalizedValue(enemyBearingDelta, maxEnemyBearingDelta)));
        nnHistoryList.addLast(new Double((float)(enemyDistance / maxDistance)));
        nnHistoryList.addLast(new Double((float)(enemyDistanceFromCenter / maxDistanceFromCenter)));
        nnHistoryList.addLast(new Double((float)(enemyVelocity / maxVelocity)));
        if (nnHistoryList.size() > HISTORY_DEPTH * HISTORY_ITEMS) {
            for (int i = 0; i < HISTORY_ITEMS; i++) {
                nnHistoryList.removeFirst();
            }
        }
        if (nnHistoryList.size() == HISTORY_DEPTH * HISTORY_ITEMS) {
            int i;
            for (i = 0; i < HISTORY_DEPTH * HISTORY_ITEMS; i++) {
                nnHistoryArray[i] = ((Double)(nnHistoryList.get(i))).floatValue();
            }
            nnHistoryArray[i++] = (float)(bulletPower / 3);
            /*
            nnHistoryArray[i++] = nnNormalizedValue(enemyBearingDelta, maxEnemyBearingDelta);
            nnHistoryArray[i++] = (float)(timeSinceLastFire / maxTimeBetweenFires);
            nnHistoryArray[i++] = (float)(enemyDistanceFromCenter / maxDistanceFromCenter);
            nnHistoryArray[i++] = (float)(enemyDistance / maxDistance);
            nnHistoryArray[i++] = nnNormalizedValue(Math.abs(minMaxAngles[0]), maxEnemyBearingDiff);
            nnHistoryArray[i++] = nnNormalizedValue(Math.abs(minMaxAngles[1]), maxEnemyBearingDiff);
            nnHistoryArray[i++] = nnNormalizedValue(rollingEnemyBearingDelta, maxEnemyBearingDelta);
            nnHistoryArray[i++] = (float)(rollingEnemyVelocity / maxVelocity);
            nnHistoryArray[i++] = (float)(enemyLocation.getX() / maxX);
            nnHistoryArray[i++] = (float)(enemyLocation.getY() / maxY);
            nnHistoryArray[i++] = (float)(location.getX() / maxX);
            nnHistoryArray[i++] = (float)(location.getY() / maxY);
            */
            return true;
        }
        return false;
    }

    class CheckUpdateFactors extends Condition {
        private long time;
        private double bVelocity;
        private double bPower;
        private double bearingDelta;
        private Point2D oldRLocation = new Point2D.Double();
        private Point2D oldELocation = new Point2D.Double();
        private double oldBearing;
	private float[] history = new float[HISTORY_DEPTH * HISTORY_ITEMS + 1];

        public CheckUpdateFactors(double bulletPower) {
            this.time = getTime();
            this.bPower = bulletPower;
            this.bVelocity = bulletVelocity(bPower);
            this.bearingDelta = enemyBearingDelta;
            this.oldRLocation.setLocation(location);
            this.oldELocation.setLocation(enemyLocation);
            this.oldBearing = absoluteBearing(oldRLocation, oldELocation);
            System.arraycopy(nnHistoryArray, 0, history, 0, nnHistoryArray.length);
        }

        public boolean test() {
            if (getOthers() == 0) {
                removeCustomEvent(this);
                return false;
            }
            double bulletDistance = bVelocity * (getTime() - time);
            if (bulletDistance > location.distance(enemyLocation) - 1) {
                if (getOthers() > 0) {
                    double impactBearing = absoluteBearing(oldRLocation, enemyLocation);
                    double bearingDiff = normalRelativeAngle(impactBearing - oldBearing);
                    if (bearingDiff <= maxEnemyBearingDiff) {
                        if (nnHistoryList.size() == HISTORY_DEPTH * HISTORY_ITEMS) { 
                            nnCorrectAnswer[0] = nnNormalizedValue(bearingDiff, maxEnemyBearingDiff);
                            if (nnTrainings < 250) {
                                nnError = nnet.ebplearnNNet(history, nnCorrectAnswer);
                                nnTrainings++;
                            }
                        }
                        meanOffsetFactor = rollingAvg(meanOffsetFactor, bearingDiff, 2, this.bPower);
                        if (Math.abs(bearingDelta) > 0.05 && bearingDelta <= maxEnemyBearingDelta) {
                            double factor = bearingDiff / bearingDelta;
                            meanAimFactor = rollingAvg(meanAimFactor, factor, 75, this.bPower);
                        }
                    }
                }
                removeCustomEvent(this);
            }
            return false;
        }
    }
}
