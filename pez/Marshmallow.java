package pez;

import pez.Rutils;
import pez.segment.Segment;
import pez.movement.MovementData;
import pez.movement.RandomMovement;
import pez.movement.MovementStrategy;
import pez.movement.AMovement;

import robocode.*;
import java.awt.geom.*;
import java.awt.Color;
import java.util.*;
import java.util.zip.*;
import java.io.*;

// Marshmallow the Robot, by PEZ. It's good toasted too!
// $Id: Marshmallow.java,v 1.119 2004/02/20 23:30:23 peter Exp $

//Todo: Fix Melee - scanner, terrain and enemy selection
//      Don't shoot at Marshmallows when other enemies exists.
//      Team idea: Group bots around team leader in a rather tight circle.
//                 Move like a school of fish turning the strongest shield robot against
//                 the most agressive enemy. Dodge with team leaders. Take terrain off center
//                 but not totally cornered.

public class Marshmallow extends AdvancedRobot implements MarshmallowConstants {
    private boolean threadIsStarted = false;
    private static Point2D location = new Point2D.Double();
    private static Point2D destination = new Point2D.Double();
    private static Rectangle2D fieldRectangle;
    private static Rectangle2D fluffedFieldRectangle;
    private static Point2D center;
    private static int bulletsFired;
    private static int bulletsHit;
    private static int bulletsFiredAtMe;
    private static int bulletsHitMe;
    private static int bulletsFired_400_500;
    private static int bulletsHit_400_500;
    private static int skippedTurns;
    private static int totalTurns;
    private static int wins;
    private static int wallHits;
    private static String statisticsFilePostfix;
    private Enemy weakestEnemy = null;
    private Enemy strongestEnemy = null;
    private Enemy closestEnemy = null;
    private Enemy farthestEnemy = null;
    private Enemy currentEnemy = null;
    private static Enemies school = null;
    private boolean isAiming = false;
    private boolean hasFired = false;
    private boolean schoolHasFired = false;
    private long timeSinceSchoolHasFired;
    private long time;
    private boolean isMeleeBattle;
    private boolean isMelee;
    private boolean isOneOnOne;
    private boolean calculateDeltas;
    private double sectorAimWidth = (MC_SECTOR_AIM_MAX - MC_SECTOR_AIM_MIN) / MC_SECTOR_AIMS;
    private double distanceStepWidth = (MC_DISTANCE_MAX - MC_DISTANCE_MIN) / MC_DISTANCES;
    private Random random = new Random();
    private boolean m_movementUpdated;
    private MovementStrategy m_movement;
    private MovementData m_movementData = new MovementData();

    private AGun aGun = new AGun();
    private static int deaths;
    
    public void run() {
        threadIsStarted = true;
        initBattle();
        initRound();
        initTurn();
        execute();

        while (true) {
            initTurn();
            execute();
            hasFired = false;
            schoolHasFired = false;
        }
    }
    
    private void initBattle() { 
        if (fieldRectangle == null) {
            fieldRectangle = new Rectangle2D.Double(0, 0, getBattleFieldWidth(), getBattleFieldHeight());
            fluffedFieldRectangle = new Rectangle2D.Double(-145, -145, getBattleFieldWidth() + 145, getBattleFieldHeight() + 145);
            center = new Point2D.Double(fieldRectangle.getWidth() / 2.0, fieldRectangle.getHeight() / 2.0);
            statisticsFilePostfix = getOthers() > 1 ? MC_STATISTICS_FILE_POSTFIX_MELEE : MC_STATISTICS_FILE_POSTFIX_1V1;
            setColors(
                new Color(255, (int)(20 + Math.random() * 80), (int)(60 + Math.random() * 180)),
                new Color((int)(Math.random() * 80), 100 + (int)(Math.random() * 150), 255),
                new Color((int)(20 + Math.random() * 80), 255, (int)(20 + Math.random() * 180))
            );
        }
    }
    
    private void initRound() {
        if (getOthers() >= 2) {
            isMelee = true;
            isMeleeBattle = true;
        }
        if (school == null) {
            school = new Enemies(this);
        }
        setAdjustGunForRobotTurn(true);
        setAdjustRadarForGunTurn(true);
        updateLocation();
        isOneOnOne = false;
        calculateDeltas = false;
        m_movement = MovementStrategy.getNewMovement();
        m_movement.initRound();
    }

    private void initTurn() {
        totalTurns++;
        time = getTime();
        if (getOthers() == 1) {
            isOneOnOne = true;
        }
        isMelee = getOthers() >= 2;
        updateLocation();
        timeSinceSchoolHasFired++;
        doScanner();
        if (isMelee) {
            currentEnemy = chooseEnemy(school);
        }
        else {
            currentEnemy = school.getOneOnOneEnemy();
        }
        if (getOthers() == 0) {
            if (timeSinceSchoolHasFired > 60) {
                goTo(new Point2D.Double(getBattleFieldWidth() / 2.0, 400.0));
            }
            else {
                if (currentEnemy != null) {
                    move(currentEnemy);
                }
            }
        }
	if (AMovement.isDeactivated) {
	    setMaxVelocity(Math.abs(getTurnRemaining()) < 45 ? m_movement.getVelocity() : 0.1);
	}
    }

    private void updateLocation() {
        location.setLocation(getX(), getY());
    }

    private Enemy chooseEnemy(Enemies school) {
        school.categorize();
        Enemy closestEnemy = school.getClosest();
        Enemy weakestEnemy = school.getWeakest();
        Enemy easiestEnemy = school.getEasiest();
        if (closestEnemy != null) {
            return closestEnemy;
        }
        if (easiestEnemy != null) {
            return easiestEnemy;
        }
        if (weakestEnemy != null && weakestEnemy.isDisabled()) {
            return weakestEnemy;
        }
        if (weakestEnemy != null) {
            return weakestEnemy;
        }
        return null;
    }

    private void hunt(Enemy enemy) {
        if (enemy != null) {
            if (enemy.getEnergy() > 0 && getGunHeat()/getGunCoolingRate() < 1.4) {
                if (!isAiming || time % 20 == 0) {
                    //enemy.updateAimGuessing();
                    //aimGunAt(enemy);
                    //addCustomEvent(new ReadyToFireCondition(enemy));
                }
            }
            else {
                setTurnGunRight(Rutils.normalRelativeAngle(enemy.getAbsoluteBearing() - getGunHeading()));
            }
            move(enemy);
        }
    }

    private void move(Enemy enemy) {
	if (AMovement.isDeactivated) {
	    m_movementData = m_movement.getMovementData(enemy, this);
	    Point2D thisDestination = m_movementData.getDestination();
	    if (thisDestination != null || Math.abs(getDistanceRemaining()) < 90) {
		if (thisDestination != null) {
		    destination = thisDestination;
		}
		if (destination != null) {
		    translateInsideField(destination, MC_WALL_MARGIN);
		    goTo(destination);
		}
	    }
	}
    }

    private void goTo(Point2D point) {
        double distance = location.distance(point);
        double angle = Rutils.normalRelativeAngle(Rutils.pointsToAngle(location, point) - getHeading());
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

    public void translateInsideField(Point2D point, double margin) {
        point.setLocation(Math.max(margin, Math.min(fieldRectangle.getWidth() - margin, point.getX())),
                          Math.max(margin, Math.min(fieldRectangle.getHeight() - margin, point.getY())));
    }

    private void aimGunAt(Enemy enemy) {
        double guessedGunHeading;
        guessedGunHeading = enemy.getGuessedGunHeading();
        double gunTurn = Rutils.normalRelativeAngle(guessedGunHeading - getGunHeading());
        setTurnGunRight(gunTurn);
    }

    public  boolean isCornered(double m) {
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

    private void doScanner() {
        double radarOffset = MC_FULL_TURN;
        if(isOneOnOne && currentEnemy != null && currentEnemy.isRecent1v1()) {
            radarOffset = Rutils.normalRelativeAngle(getRadarHeading() - 1 - currentEnemy.getAbsoluteBearing());
            radarOffset += Rutils.sign(radarOffset) * 4.5;
        }
        setTurnRadarLeft(radarOffset);
    }

    private Enemy getEnemyByName(String name) {
        Enemy searchFor = new Enemy(name);
        int index = school.indexOf(searchFor);
        if (index >= 0) {
            Enemy found = (Enemy)school.get(index);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private String fileName(String name) {
        return name + statisticsFilePostfix;
    }

    private void saveStatistics(Enemy enemy) {
        String enemyName;
        if (!isMeleeBattle) {
            if (enemy != null) {
                writeObject((Serializable)enemy.getTuningFactors(), fileName(enemy.getName()));
            }
            else {
                Iterator iterator = school.iterator();
                while (iterator.hasNext()) {
                    Enemy schoolEnemy = (Enemy)iterator.next();
                    if (schoolEnemy.isActive()) {
                        writeObject((Serializable)schoolEnemy.getTuningFactors(), fileName(schoolEnemy.getName()));
                    }
                }
            }
        }
    }

    private void printStats(Enemy enemy) {
        if (true) {
            if (enemy != null) {
                enemy.printStats();
            }
            else {
                Iterator iterator = school.iterator();
                while (iterator.hasNext()) {
                    Enemy schoolEnemy = (Enemy)iterator.next();
                    if (schoolEnemy.isActive()) {
                        schoolEnemy.printStats();
                    }
                    if (isMelee) {
                        break;
                    }
                }
            }
            //m_movement.printStats();
            //out.println("< " + bulletsHitMe + " / " + bulletsFiredAtMe + " = " +
            //    Math.round(10000.0 * bulletsHitMe / bulletsFiredAtMe) / 100.0);
            out.println("> " + bulletsHit + " / " + bulletsFired + " = " +
                Math.round(10000.0 * bulletsHit / bulletsFired) / 100.0);
            out.println("> 400-500 " + bulletsHit_400_500 + " / " + bulletsFired_400_500 + " = " +
                Math.round(10000.0 * bulletsHit_400_500 / bulletsFired_400_500) / 100.0);
        }
        out.println("Wins: " + wins + ". Turns skipped/total=ratio: " + skippedTurns + "/" + totalTurns + "=" +
            Math.round(10000.0 * skippedTurns / totalTurns) / 100.0);
        out.println("Hit walls counter: " + wallHits);
    }

    public Point2D getLocation() {
        return location;
    }
    
    public double getHeading() {
        if( !isStarted() ) {
            return 0;
        }
        else {
            return super.getHeading();
        }
    }
    
    public boolean isStarted()  {
        return threadIsStarted;
    }

    public long getTimeSinceEnemyFired() {
        return timeSinceSchoolHasFired;
    }

    double absoluteBearing(double bearing) {
        return getHeading() + bearing;
    }

    boolean schoolHasFired() {
        return this.schoolHasFired;
    }

    boolean hasFired() {
        return this.hasFired;
    }

    boolean isMelee() {
        return this.isMelee;
    }

    public boolean moveFinished() {
        return Math.abs(getDistanceRemaining()) < 20;
    }

    public Rectangle2D getFieldRectangle() {
        return fieldRectangle;
    }

    public Rectangle2D getFluffedFieldRectangle() {
        return fluffedFieldRectangle;
    }

    Point2D getNextLocation() {
        return destination;
    }

    Point2D getCenter() {
        return this.center;
    }

    public void onHitWall(HitWallEvent e) {
        wallHits++;
    }

    public void onHitRobot(HitRobotEvent e) {
        Enemy enemy = getEnemyByName(e.getName());
        if (enemy != null) {
            currentEnemy = enemy;
        }
    }

    public void onHitByBullet(HitByBulletEvent e) {
	if (AMovement.isDeactivated) {
	    Enemy enemy = getEnemyByName(e.getName());
	    if (enemy != null) {
		enemy.registerEHit();
	    }
	} else {
	    AMovement.onHitByBullet(e);
	}
	bulletsHitMe++;
    }

    public void onScannedRobot(ScannedRobotEvent e) {
	AMovement.onScannedRobot(e, this);
	aGun.onScannedRobot(e, this);
        Enemy storedEnemy = getEnemyByName(e.getName());
        if (storedEnemy != null) {
            storedEnemy.update(calculateDeltas, e.getEnergy(), e.getBearing(), e.getDistance(),
                e.getHeading(), e.getVelocity(), e.getTime());
            storedEnemy.activate();
            storedEnemy.setRobot(this);
            if (storedEnemy.getIsFiring()) {
                bulletsFiredAtMe++;
                schoolHasFired = true;
                timeSinceSchoolHasFired = 0;
                storedEnemy.registerEFired();
                if (Math.random() > storedEnemy.getDistance() / (160 * Math.max(2.0, storedEnemy.getEFirePower()))) {
                    destination.setLocation(getX(), getY());
                }
                m_movement.setNewVelocity();
            }
            hunt(storedEnemy);
        }
        else {
            Map factors = getEnemyTuningFactors(e.getName());
            school.add(new Enemy(this, e.getName(), factors,
                    e.getEnergy(), e.getBearing(), e.getDistance(), e.getHeading(), e.getVelocity(), e.getTime()));
        }
        calculateDeltas = true;
    }

    public void onBulletHit(BulletHitEvent e) {
        Enemy enemy = getEnemyByName(e.getName());
        if (enemy != null) {
            enemy.setDamage(enemy.getEnergy() - e.getEnergy());
            if (enemy.getDistance() >= 400 && enemy.getDistance() <= 500) {
                bulletsHit_400_500++;
            }
        }
        bulletsHit++;
    }

    public void onRobotDeath(RobotDeathEvent e) {
        Enemy enemy = getEnemyByName(e.getName());
        if (enemy != null) {
            move(enemy);
            printStats(enemy);
            //saveStatistics(enemy);
            if (getOthers() == 1) {
                statisticsFilePostfix = MC_STATISTICS_FILE_POSTFIX_1V1;
                enemy.setTuningFactors(getEnemyTuningFactors(enemy.getName()));
            }
            enemy.deactivate();
        }
    }

    public void onDeath(DeathEvent e) {
	deaths++;
	AMovement.isDeactivated = AMovement.isFlattening && deaths > 4;
        m_movement.updateRatio(0);
        printStats(null);
        //saveStatistics(null);
        school.deactivate();
    }

    public void onWin(WinEvent e) {
        wins++;
        m_movement.updateRatio(100);
    }

    public void onSkippedTurn(SkippedTurnEvent e) {
        skippedTurns++;
    }

    public void onCustomEvent(CustomEvent e) {
        Condition condition = e.getCondition();
        if (condition instanceof ReadyToFireCondition) {
            Enemy enemy = ((ReadyToFireCondition)condition).enemy;
            Bullet bullet = fireBullet(enemy.getRFirePower());		
            if (bullet != null) {
                enemy.registerRFired();
                addCustomEvent(new BulletHasHitOrMissedCondition(bullet, enemy));
                addCustomEvent(new VirtualGunsFinishedCondition(enemy));
                hasFired = true;
                bulletsFired++;
                if (enemy.getDistance() >= 400 && enemy.getDistance() <= 500) {
                    bulletsFired_400_500++;
                }
            }
        }
    }

    private Map getEnemyTuningFactors(String enemyName) {
        Map factors;
        try {
            factors = (HashMap)readCompressedObject(fileName(enemyName));
        }
        catch (Exception e) {
            factors = TuningFactorMap.createDefault(sectorAimWidth, distanceStepWidth);            
        }
        Iterator iterator = factors.values().iterator();
        while (iterator.hasNext()) {
            ((TuningFactor)iterator.next()).init();
        }
        Segment.connectSegments(factors, "virtualGunFactor");
        Segment.connectSegments(factors, "sectorAimFactor");
        Segment.connectSegments(factors, "escapeAreaFactor");
        return factors;
    }
    
    public Object readCompressedObject(String filename) throws IOException {
        try {
            ZipInputStream zipin = new ZipInputStream(new
                FileInputStream(getDataFile(filename + ".zip")));
            zipin.getNextEntry();
            ObjectInputStream in = new ObjectInputStream(zipin);
            Object obj = in.readObject();
            in.close();
            return obj;
        }
        catch (ClassNotFoundException e) {
            System.out.println("Class not found! :-(");
            e.printStackTrace();
        }
        return null;
    }

    public void writeObject(Serializable obj, String filename) {
        try {
            ZipOutputStream zipout = new ZipOutputStream(new RobocodeFileOutputStream(getDataFile(filename + ".zip")));
            zipout.putNextEntry(new ZipEntry(filename));
            ObjectOutputStream out = new ObjectOutputStream(zipout);
            out.writeObject(obj);
            out.flush();
            zipout.closeEntry();
            out.close();
        }
        catch (IOException e) {
            System.out.println("Error writing Object:" + e);
        }
    } 

    public boolean isLoosing(Enemy enemy) {
        return getEnergy() < 2 && enemy.getEnergy() / getEnergy() > 12;
    }

    public boolean isInBigLead(Enemy enemy) {
        return getEnergy() / enemy.getEnergy() > 6;
    }

    public boolean isInEndGameSmallLead(Enemy enemy) {
        return (getEnergy() > enemy.getEnergy() && getEnergy() < enemy.getEnergy() + enemy.getRFirePower());
    }

    //Todo: Consider holding the fire when statistics aren't good enough
    class ReadyToFireCondition extends Condition {
        Enemy enemy;

        public ReadyToFireCondition(Enemy enemy) {
            this.enemy = enemy;
            isAiming = true;
        }

        public boolean test() {
            if ((!enemy.shouldFire) ||
                    (enemy.getEnergy() < 0.2 && enemy.getDistance() > 190) ||
                    (enemy.getDistance() > 150 && (getEnergy() < 0.4) ||
                    (timeSinceSchoolHasFired > 40 && isInEndGameSmallLead(enemy) && !(enemy.getDistance() < 100)))) {
                removeCustomEvent(this);
                return false;
            }
            if (getGunHeat() == 0.0 &&
                    (enemy.getDistance() < 190 || ((getEnergy() > 2.0 || (getEnergy() > 0.2 && getTime() % 50 == 0))))) {
                isAiming = false;
                removeCustomEvent(this);
                return true;
            }
            return false;
        }
    }

    class BulletHasHitOrMissedCondition extends Condition {
        private Bullet bullet;
        private long impactTime;
        private String enemyName;
        Enemy enemy;
        double bestDistance;
        boolean success;

        public BulletHasHitOrMissedCondition(Bullet bullet, Enemy enemy) {
            this.bullet = bullet;
            this.enemy = enemy;
            enemyName = enemy.getName();
            impactTime = enemy.impactTime() - 1;
        }

        public boolean test() {
            if (bullet.getVictim() != null || time > impactTime) {
                success = bullet.getVictim() != enemyName;
                if (success) {
                    enemy.registerRHit();
                }
                else {
                    enemy.registerRMiss();
                }
                removeCustomEvent(this);
                return true;
            }
            if (!enemy.isActive()) {
                bulletsFired--;
                bulletsFired_400_500--;
                removeCustomEvent(this);
                return true;
            }
            return false;
        }
    }

    class VirtualGunsFinishedCondition extends Condition {
        private Enemy enemy;
        private Object[] vGuns;
        private TuningFactor vgVirtualGunFactor;
        private TuningFactor vgSectorAimFactor;
        private TuningFactor vgEscapeAreaFactor;

        public VirtualGunsFinishedCondition(Enemy enemy) {
            this.enemy = enemy;
            this.vGuns = enemy.getVirtualGuns();
            this.vgVirtualGunFactor = enemy.getVirtualGunFactor();
            this.vgSectorAimFactor = enemy.getSectorAimFactor();
            this.vgEscapeAreaFactor = enemy.getEscapeAreaFactor();
        }

        public boolean test() {
            if (!enemy.isActive() || !enemy.checkVGunStatus(vGuns, vgVirtualGunFactor, vgSectorAimFactor, vgEscapeAreaFactor)) {
                removeCustomEvent(this);
                return true;
            }
            return false;
        }
    }
}
