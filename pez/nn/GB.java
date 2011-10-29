package pez.nn;
import robocode.*;
import pez.nn.nrlibj.*;
import java.awt.geom.*;
import java.awt.Color;
import java.util.*;

// GB, by PEZ. Stupid and violent
// $Id: GB.java,v 1.8 2003/09/19 21:30:57 peter Exp $

public class GB extends AdvancedRobot {
    private final static double MAX_BEARING_DIFF = 47;
    private final static double MAX_VELOCITY = 8;
    private final static double MAX_BEARING_DELTA = 7;
    private final static double MAX_ARC_MOVEMENT = 8;
    private final static double MAX_DISTANCE_DELTA = 11;
    private final static double MAX_HEADING_DELTA = 6;
    private final static double MAX_ACCUMULATED_ANGLE = 46;
    private final static int HISTORY_DEPTH = 2;
    private final static int HISTORY_ITEMS = 1;
    private final static String NNdescr[] = {
        "layer=0 tnode=4 nname=NodeLin",
        "layer=1 tnode=7 nname=NodeSigm",
        "layer=2 tnode=1 nname=NodeSigm",
        "linktype=all fromlayer=0 tolayer=1",
        "linktype=all fromlayer=1 tolayer=2"
    };
    private final static int VIRTUAL_GUNS = 5;
    private final static double MIN_NETS_IN_TRAINING = 3;
    private final static int MAX_TRAININGS = 300;
    private final static int TRAINING_SESSION_LENGTH = 70;
    private final static double VIRTUAL_GUN_RATE_DEPTH = 150;
    private final static double MAX_HIT_RATE = 100;

    private static Point2D location = new Point2D.Double();
    private static Point2D oldLocation = new Point2D.Double();
    private static Point2D enemyLocation = new Point2D.Double();
    private static Point2D oldEnemyLocation = new Point2D.Double();
    private static Rectangle2D fieldRectangle;
    private boolean haveEnemy;
    private static String enemyName;
    private double enemyDistance;
    private double enemyDistanceDelta;
    private double enemyHeading;
    private double enemyHeadingDelta;
    private double enemyEnergy;
    private double enemyBulletPower = 1;
    private double enemyBulletVelocity;
    private static long enemyShots;
    private double enemyVelocity;
    private double absoluteBearing;
    private double enemyBearingDelta;
    private double enemyArcMovement;
    private double bulletPower;
    private long roundNum;
    private boolean roundOver;
    private int waitBeforeRam;
    private static long time;
    private static long wins;
    private static long shots;
    private static long skippedTurns;

    private double timeDelta;
    private static double maxTimeDelta;
    private static double maxDistance;
    private static double rollingBearingDelta;
    private static double rollingDistanceDelta;
    private static int nNetsInTraining;
    private static VirtualGun[] virtualGuns = new VirtualGun[VIRTUAL_GUNS];
    private static LinkedList nnHistoryList = new LinkedList();
    private static float[] nnHistoryArray = new float[HISTORY_DEPTH * HISTORY_ITEMS + 2];
    private double nnError;

    private final static double DEFAULT_DISTANCE = 520;
    private double accumulatedAngle;
    private double enemyAccumulatedAngle;

    private static double centerX;
    private static double centerY;

    public void run() {
        if (fieldRectangle == null) {
            initBattle();
        }
        roundOver = false;
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);

        while (true) {
            selectNNetsForTraining();
            if (!haveEnemy) {
                setTurnRadarLeft(22.5);
            }
            haveEnemy = false;
            if ((getEnergy() > 0.2 || enemyDistance < 120) && enemyEnergy > 0 && getGunHeat() == 0.0) {
                Bullet bullet = setFireBullet(bulletPower);
                if (bullet != null) {
                    shots++;
                    addCustomEvent(new CheckUpdateFactors(bullet));
                }
            }
            execute();
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        double radarTurn;
        enemyName = e.getName();
        oldLocation.setLocation(location);
        location.setLocation(getX(), getY());
        oldEnemyLocation.setLocation(enemyLocation);
        absoluteBearing = getHeading() + e.getBearing();
        double enemyEnergyDelta = enemyEnergy - e.getEnergy();
        if (enemyEnergyDelta >= 0.1 && enemyEnergyDelta <= 3.0) {
            enemyShots++;
            enemyBulletPower = enemyEnergyDelta;
            enemyBulletVelocity = 20 - 3 * enemyBulletPower;
        }
        enemyEnergy = e.getEnergy();
        enemyVelocity = e.getVelocity();
        enemyDistance = e.getDistance();
        toLocation(absoluteBearing, enemyDistance, location, enemyLocation);
        setBulletPower();
        timeDelta = getTime() - time;
        if (timeDelta > 0) {
            if (timeDelta > maxTimeDelta) {
                maxTimeDelta = timeDelta;
            }
            enemyBearingDelta = normalRelativeAngle(absoluteBearing(oldLocation, enemyLocation) -
                absoluteBearing(oldLocation, oldEnemyLocation)) / timeDelta;
            enemyDistanceDelta = (oldLocation.distance(oldEnemyLocation) - oldLocation.distance(enemyLocation)) / timeDelta;
            enemyHeadingDelta = (enemyHeading - e.getHeading()) / timeDelta;
            time = getTime();
            if (enemyAccumulatedAngle > MAX_ACCUMULATED_ANGLE) {
                enemyAccumulatedAngle = 0;
            }
            enemyAccumulatedAngle += enemyBearingDelta;
            record();
        }
        enemyHeading= e.getHeading();
        enemyArcMovement = enemyVelocity * Math.sin(e.getHeadingRadians() - e.getBearingRadians() - getHeadingRadians()); 
        haveEnemy = true;
        radarTurn = normalRelativeAngle(getHeading() + e.getBearing() - getRadarHeading()) * 2;
        setTurnRadarRight(radarTurn);
        if (getOthers() > 0 && getGunHeat() / getGunCoolingRate() < 2) {
            aim();
        }
        else {
            setTurnGunRight(normalRelativeAngle(absoluteBearing - getGunHeading()));
        }
        move();
    }

    public void onWin(WinEvent e) {
        wins++;
        if (!roundOver) {
            printStats();
        }
        roundOver = true;
    }

    public void onDeath(DeathEvent e) {
        if (!roundOver) {
            printStats();
        }
        roundOver = true;
    }

    public void onSkippedTurn(SkippedTurnEvent e) {
        skippedTurns++;
    }

    private void initBattle() {
        roundNum = getRoundNum();
        fieldRectangle = new Rectangle2D.Double(0, 0 , getBattleFieldWidth(), getBattleFieldHeight());
        centerX = getBattleFieldWidth() / 2;
        centerY = getBattleFieldHeight() / 2;
        setColors(Color.red, Color.blue, Color.white);
        maxDistance = (new Point2D.Double(20,20)).distance(
            new Point2D.Double(fieldRectangle.getWidth() - 20, fieldRectangle.getHeight() - 20));
        for (int i = 0; i < VIRTUAL_GUNS; i++) {
            virtualGuns[i] = new VirtualGun(i);
        }
    }

    private void aim() {
        double guessedDistance = location.distance(enemyLocation);
        Arrays.sort(virtualGuns);
        virtualGuns[0].predict(nnHistoryArray);
        double guessedHeading = absoluteBearing(location, enemyLocation) + virtualGuns[0].getPrediction();
        Point2D impactLocation = new Point2D.Double();
        toLocation(guessedHeading, guessedDistance, location, impactLocation);
        translateInsideField(impactLocation, 1);
        guessedHeading = absoluteBearing(location, impactLocation);
        setTurnGunRight(normalRelativeAngle(guessedHeading - getGunHeading()));
    }

    private void move() {
        goTo(new Point2D.Double(centerX - (enemyLocation.getX() - centerX), centerY - (enemyLocation.getY() - centerY)));
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

    private void setBulletPower() {
        double power = 2.4;
        power = Math.min(enemyEnergy / 4, power);
        power = Math.min(getEnergy() / 5, power);
        bulletPower = power;
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

    //Paul Evans' excellent function for keeping rolling averages
    private static double rollingAvg(double value, double newEntry, double n, double weighting ) {
        return (value * n + newEntry * weighting) / (n + weighting);
    } 

    private static float nnNormalizedValue(double value, double max) {
        return (float)((value + max) / (2 * max));
    }

    private void record() {
        //rollingBearingDelta = rollingAvg(rollingBearingDelta, enemyBearingDelta, 15, 1);
        //rollingDistanceDelta = rollingAvg(rollingDistanceDelta, enemyDistanceDelta, 15, 1);

        //nnHistoryList.addLast(new Double(nnNormalizedValue(enemyBearingDelta, MAX_BEARING_DELTA)));
        nnHistoryList.addLast(new Double(nnNormalizedValue(enemyArcMovement, MAX_ARC_MOVEMENT)));
        if (nnHistoryList.size() > HISTORY_DEPTH * HISTORY_ITEMS) {
            for (int i = 0; i < HISTORY_ITEMS; i++) {
                nnHistoryList.removeFirst();
            }
            int i;
            for (i = 0; i < HISTORY_DEPTH * HISTORY_ITEMS; i++) {
                nnHistoryArray[i] = ((Double)(nnHistoryList.get(i))).floatValue();
            }
            nnHistoryArray[i++] = (float)(nnNormalizedValue(enemyAccumulatedAngle, MAX_ACCUMULATED_ANGLE));
            nnHistoryArray[i++] = (float)(enemyDistance / maxDistance);
            //nnHistoryArray[i++] = (float)(bulletPower / 3D);
        }
    }

    private void selectNNetsForTraining() {
        int numSelected = 0;
        int numInTraining = 0;
        for (int i = 0; i < VIRTUAL_GUNS; i++) {
            if (virtualGuns[i].isInTraining()) {
                numInTraining++;
                if (virtualGuns[i].getTrainingsThisSession() > TRAINING_SESSION_LENGTH) {
                    virtualGuns[i].setInTraining(false);
                }
                if (virtualGuns[i].getTrainings() > MAX_TRAININGS) {
                    virtualGuns[i].setInTraining(false);
                }
            }
        }
        if (numInTraining < MIN_NETS_IN_TRAINING) {
            for (int i = VIRTUAL_GUNS - 1; i >= 0; i--) {
                if (!(virtualGuns[i].getTrainings() > MAX_TRAININGS)) {
                    virtualGuns[i].setInTraining(true);
                    virtualGuns[i].initiateTraining();
                    break;
                }
            }
        }
    }

    private void selectNNetsForTraining2() {
        int numSelected = 0;
        int numInTraining = 0;
        for (int i = 0; i < VIRTUAL_GUNS; i++) {
            if (virtualGuns[i].isInTraining()) {
                numInTraining++;
                if (virtualGuns[i].getTrainingsThisSession() > TRAINING_SESSION_LENGTH) {
                    virtualGuns[i].setInTraining(false);
                }
                if (virtualGuns[i].getTrainings() > MAX_TRAININGS) {
                    virtualGuns[i].setInTraining(false);
                }
            }
        }
        if (numInTraining < MIN_NETS_IN_TRAINING) {
            int net = (int)Math.floor((Math.random() * VIRTUAL_GUNS));
            if (!(virtualGuns[net].getTrainings() > MAX_TRAININGS)) {
                virtualGuns[net].setInTraining(true);
                virtualGuns[net].initiateTraining();
            }
        }
    }

    private void printStats() {
        System.out.println("wins, %: " + wins + ", " + (((double)wins / (getRoundNum() + 1)) * 10000 / 100) + "%");
        for (int i = 0; i < VIRTUAL_GUNS; i++) {
            System.out.println("Net #: " + virtualGuns[i].getId() +
                ", trainings: " + virtualGuns[i].getTrainings() +
                ", rating: " + virtualGuns[i].getHitRate());
        }
    }

    class CheckUpdateFactors extends Condition {
        private long time;
        private double bulletVelocity;
        private double bulletPower;
        private double bearingDelta;
        private Point2D oldRLocation = new Point2D.Double();
        private Point2D oldELocation = new Point2D.Double();
        private double oldBearing;
	private float[] history = new float[HISTORY_DEPTH * HISTORY_ITEMS + 2];
        private double[] prediction = new double[VIRTUAL_GUNS];

        public CheckUpdateFactors(Bullet bullet) {
            this.time = getTime();
            this.bulletVelocity = bullet.getVelocity();
            this.bulletPower = bullet.getPower();
            this.bearingDelta = enemyBearingDelta;
            this.oldRLocation.setLocation(location);
            this.oldELocation.setLocation(enemyLocation);
            this.oldBearing = absoluteBearing(oldRLocation, oldELocation);
            System.arraycopy(nnHistoryArray, 0, history, 0, nnHistoryArray.length);
            for (int i = 0; i < VIRTUAL_GUNS; i++) {
                this.prediction[i] = virtualGuns[i].getPrediction();
            }
        }

        public boolean test() {
            if (getOthers() == 0) {
                removeCustomEvent(this);
                return false;
            }
            double bulletDistance = bulletVelocity * (getTime() - time);
            if (bulletDistance > oldRLocation.distance(enemyLocation) - 5) {
                double impactBearing = absoluteBearing(oldRLocation, enemyLocation);
                double bearingDiff = normalRelativeAngle(impactBearing - oldBearing);
                for (int i = 0; i < VIRTUAL_GUNS; i++) {
                    if (Math.abs(prediction[i] - bearingDiff) < Math.toDegrees(Math.atan(20 / bulletDistance))) {
                        virtualGuns[i].updateHitRate(MAX_HIT_RATE, bulletPower);
                    }
                    else {
                        virtualGuns[i].updateHitRate(0, bulletPower);
                    }
                }
                if (nnHistoryList.size() == HISTORY_DEPTH * HISTORY_ITEMS) { 
                    float answer[] = { nnNormalizedValue(bearingDiff, MAX_BEARING_DIFF) };
                    for (int i = 0; i < VIRTUAL_GUNS; i++) {
                        if (virtualGuns[i].isInTraining()) {
                            nnError = virtualGuns[i].train(history, answer);
                        }
                    }
                }
                removeCustomEvent(this);
            }
            return false;
        }
    }

    class VirtualGun implements Comparable {
        private int id;
        private wiki.nn.nrlibj.NNet nnet;
        private double nnError;
        private double prediction;
        private long trainings;
        private int trainingsThisSession;
        private double hitRate;
        private double bestHitRate;
        private boolean inTraining;

        public VirtualGun(int id) {
            this.id = id;
            wiki.nn.nrlibj.NrPop.setSeed();
            nnet = new wiki.nn.nrlibj.NNet(NNdescr);
        }

        public VirtualGun(int id, wiki.nn.nrlibj.NNet nnet) {
            this.id = id;
            this.nnet = nnet;
        }

        public int compareTo(Object o) {
            VirtualGun vg = (VirtualGun) o;
            if (this.getHitRate() > vg.getHitRate()) {
                return -1;
            }
            if (this.getHitRate() < vg.getHitRate()) {
                return +1;
            }
            return 0;
        }

        public boolean equals(Object object) {
            if (object instanceof VirtualGun) {
                return (((VirtualGun)object).getHitRate() == this.getHitRate());
            }
            return false;
        }

        wiki.nn.nrlibj.NNet getNNet() {
            return this.nnet;
        }

        double getError() {
            if (trainings > 0) {
                return nnError;
            }
            else {
                return 1;
            }
        }

        double train(float[] input, float[] answer) {
            nnError = nnet.ebplearnNNet(input, answer);
            trainings++;
            trainingsThisSession++;
            return nnError;
        }

        long getTrainings() {
            return this.trainings;
        }

        void predict(float[] input) {
            float[] nnAnswer = new float[1];
            nnet.frwNNet(input, nnAnswer);
            prediction = (0 - MAX_BEARING_DIFF) + MAX_BEARING_DIFF * 2 * nnAnswer[0];
        }

        double getPrediction() {
            return this.prediction;
        }

        void updateHitRate(double hitValue, double weight) {
            this.hitRate = rollingAvg(this.hitRate, hitValue, Math.min(shots, VIRTUAL_GUN_RATE_DEPTH), weight);
        }

        double getHitRate() {
            if (trainings > 0) {
                return this.hitRate;
            }
            else {
                return 0;
            }
        }
        
        boolean isInTraining() {
            return this.inTraining;
        }

        void setInTraining(boolean inTraining) {
            this.inTraining = inTraining;
        }

        void initiateTraining() {
            trainingsThisSession = 0;
        }

        int getTrainingsThisSession() {
            return this.trainingsThisSession;
        }

        int getId() {
            return this.id;
        }
    }
}
