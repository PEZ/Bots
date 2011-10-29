package pez;
import java.awt.geom.*;
import java.util.*;
import pez.Rutils.*;
import robocode.*;

// Enemy. Keeping track of a Marshmallow Enemy.
// $Id: Enemy.java,v 1.100 2004/02/20 09:55:35 peter Exp $

// Todo: 
//       Add sector aim factors for enemy in corner.
//       Add virtual gun factors for enemy in corner.
//       More factors for long term learning.

public class Enemy implements MarshmallowConstants {
    static final long UNINITIALIZED = -1;
    
    Marshmallow robot;
    String name = "John Dough";
    boolean active;
    boolean disabled = false;
    long updates;
    double energy;
    double energyDelta;
    double bearing;
    double bearingDelta;
    CalculusList bearingDeltaList;
    double bearingDeltaAverage;
    double absoluteBearing;
    double absoluteBearingDelta;
    double distance;
    double distanceDelta;
    double heading;
    double headingDelta;
    double velocity;
    double velocityDelta;
    double retreatingVelocity;
    CalculusList velocityDeltaList;
    double velocityDeltaAverage;
    long time;
    long timeDelta = MC_RECENT_ENEMY + 1;
    
    Point2D location = new Point2D.Double();
    Point2D robotLocation = new Point2D.Double();
    Point2D oldLocation = new Point2D.Double();
    Point2D oldRobotLocation = new Point2D.Double();
    double xDelta;
    double yDelta;
    Point2D impactLocation = new Point2D.Double();
    boolean isFiring;
    double eFirePower = 3;
    double rFirePower;
    boolean shouldFire = true;
    long eFiredCount;
    long eTotalFiredCount;
    long eHits;
    long eTotalHits;
    long rFiredCount;
    long rTotalFiredCount;
    long rHits;
    double lastDamage;
    long rTotalHits;
    double rBulletVelocity;
    double rBulletTravelTime;
    double guessedGunHeading;
    Tracker eTracker;
    TuningFactor vGunFactor;
    TuningFactor previousVGunFactor;
    VirtualGun[] virtualGuns;
    TuningFactor sectorAimFactor;
    Point2D sectorAimLocation;
    double sectorAimWidth;
    TuningFactor escapeAreaFactor;
    Point2D escapeAreaLocation;
    double minEscapeAngle;
    double maxEscapeAngle;
    double bestBulletPower = 3.0;

    double width = 36;
    double height = 36;
    
    double cachedFieldWidth = UNINITIALIZED;
    double cachedFieldHeight = UNINITIALIZED;
    
    CalculusList eRunningFirePower;
    Map tuningFactors;
    TuningFactorMap tuningFactorMap = null;
    Point2D center;
    double angularFactor;
    double offsetFactor;

    public Enemy(String name) {
        this.name = name;
    }
    
    private double getBattleFieldHeight() {
        if( cachedFieldHeight ==  UNINITIALIZED ) {
            cachedFieldHeight = robot.getBattleFieldHeight();
        }
        
        return cachedFieldHeight;
    }
    
    private double getBattleFieldWidth() {
        if( cachedFieldWidth ==  UNINITIALIZED ) {
            cachedFieldWidth = robot.getBattleFieldWidth();
        }
        
        return cachedFieldWidth;
    }
    
    public Enemy(Marshmallow robot, String name, Map tuningFactors,
                 double energy, double bearing, double distance, double heading, double velocity, long time) {
        this.robot = robot;
        this.name = name;
        this.tuningFactors = tuningFactors;        
        this.tuningFactorMap = new TuningFactorMap();
        this.tuningFactorMap.putAll( tuningFactors );
        eRunningFirePower = new CalculusList(20);
        eTracker = new Tracker(this, robot);
        this.energy = energy;
        this.bearing = bearing;
        this.distance = distance;
        this.heading = heading;
        this.velocity = velocity;        
        
        updatePosition(false);
        this.time = time;
        guessedGunHeading = absoluteBearing;
        bearingDeltaList = new CalculusList(4);
        velocityDeltaList = new CalculusList(3);
        sectorAimLocation = new Point2D.Double();
        escapeAreaLocation = new Point2D.Double();
        sectorAimWidth = (MC_SECTOR_AIM_MAX - MC_SECTOR_AIM_MIN) / MC_SECTOR_AIMS;
        center = robot.getCenter();
    }

    public boolean equals(Object object) {
        if (object instanceof Enemy) {
            return (((Enemy)object).getName().equals(this.getName()));
        }
        return false;
    }

    void activate() {
        if (!this.active) {
            this.rTotalHits += rHits;
            this.rFiredCount = 0;
            this.eFiredCount = 0;
            this.rHits = 0;
            this.eHits = 0;
            this.active = true;
            bearingDeltaAverage = 0.0;
            velocityDeltaAverage = 0.0;
        }
    }

    void deactivate() {
        this.active = false;
        this.time = 0;
    }

    void setRobot(Marshmallow robot) {
        this.robot = robot;
    }

    void update(boolean calculateDeltas, double newEnergy, double newBearing, double newDistance,
                double newHeading, double newVelocity, long newTime) {
        
        updates++;
        if (calculateDeltas) {
            energyDelta = newEnergy - energy;
            headingDelta = newHeading - heading;
            timeDelta = newTime - time;
        }
        else {
            timeDelta = 0;
            eTracker.recordBreak(1);
        }

        double delta = energyDelta - lastDamage;
        isFiring = false;
        lastDamage = 0;
        if (delta <= -0.1 && delta >= -3) {
            eFirePower = 0 - delta;
            isFiring = true;
        }
        
        energy = newEnergy;
        if (energy == 0) {
            disabled = true;
        }
        bearing = newBearing;
        distance = newDistance;
        heading = newHeading;
        double oldVelocity = velocity;
        velocity = newVelocity;
        oldLocation.setLocation(location);
        oldRobotLocation.setLocation(robotLocation);
        updatePosition(calculateDeltas);
        this.time = newTime;
        bearingDeltaAverage = bearingDeltaList.average();
        if (calculateDeltas && timeDelta > 0 && isActive()) {
            bearingDelta = Rutils.normalRelativeAngle(Rutils.pointsToAngle(oldRobotLocation, location) -
                Rutils.pointsToAngle(oldRobotLocation, oldLocation)) / timeDelta;
            bearingDeltaList.addValue(bearingDelta);
            bearingDeltaAverage = bearingDeltaList.average();
            distanceDelta = (oldRobotLocation.distance(location) - oldRobotLocation.distance(oldLocation)) / timeDelta;
            xDelta = (location.getX() - oldLocation.getX()) / timeDelta;
            yDelta = (location.getY() - oldLocation.getY()) / timeDelta;
            velocityDelta = (newVelocity - oldVelocity) / timeDelta;
            velocityDeltaList.addValue(velocityDelta);
            velocityDeltaAverage = velocityDeltaList.average();
            eTracker.record(Math.toRadians(headingDelta), velocity, timeDelta);
        }
        retreatingVelocity = Rutils.cos(heading - absoluteBearing) * velocity;
    }

    void updateAimGuessing() {
        virtualGuns = new VirtualGun[MC_AIM_METHODS];
        for (int i = 0; i < MC_AIM_METHODS; i++) {
            virtualGuns[i] = new VirtualGun(this);
        }
        int gun = selectGun();
        rBulletTravelTime = distance / rBulletVelocity;

        impactLocation.setLocation(location);
        virtualGuns[MC_AIM_STRAIGHT].setBearing(guessedGunHeading());

        impactLocation.setLocation(location);
        guessLocation(8.0, 0, impactLocation);
        virtualGuns[MC_AIM_RANDOM_NARROW].setBearing(guessedGunHeading());

        impactLocation.setLocation(location);
        guessLocation(24.0, 0, impactLocation);
        virtualGuns[MC_AIM_RANDOM_NORMAL].setBearing(guessedGunHeading());

        /*
        impactLocation.setLocation(guessedLocationAngular());
        guessLocation(1.0, 0, impactLocation);
        virtualGuns[MC_AIM_ANGULAR].setBearing(guessedGunHeading());

        impactLocation.setLocation(guessedLocationAngularFactored());
        guessLocation(1.0, 0, impactLocation);
        virtualGuns[MC_AIM_ANGULAR_FACTORED].setBearing(guessedGunHeading());
        */

        impactLocation.setLocation(guessedLocationOffsetFactored());
        guessLocation(1.0, 0, impactLocation);
        virtualGuns[MC_AIM_OFFSET_FACTORED].setBearing(guessedGunHeading());

        if (!robot.isMelee()) {
            impactLocation.setLocation(eTracker.guessedLocation());
            translateInsideField(impactLocation);
        }
        virtualGuns[MC_AIM_PATTERN_MATCH].setBearing(guessedGunHeading());

        guessLocation(1.0, 0, sectorAimLocation);
        impactLocation.setLocation(sectorAimLocation);
        virtualGuns[MC_AIM_SECTOR_AIM].setBearing(guessedGunHeading());

        guessLocation(1.0, 0, escapeAreaLocation);
        impactLocation.setLocation(escapeAreaLocation);
        virtualGuns[MC_AIM_ESCAPE_AREA_AIM].setBearing(guessedGunHeading());

        impactLocation.setLocation(guessedLocationMirrored());
        guessLocation(1.0, 0, impactLocation);
        virtualGuns[MC_AIM_ANTI_MIRROR].setBearing(guessedGunHeading());

        if (vGunFactor.getUses() < 5) {
            gun = MC_AIM_RANDOM_NORMAL;
        }

//gun = MC_AIM_PATTERN_MATCH;
        impactLocation.setLocation(virtualGuns[gun].impactPoint);
        rBulletTravelTime = virtualGuns[gun].bulletTravelTime;
        guessedGunHeading = virtualGuns[gun].guessedBearing;
    }

    int selectGun() {
        int gun;
        int bestGun = getBestVirtualGun();
        setRFirePower();
        return bestGun;
    }

    int getBestVirtualGun() {
        int gun;
        
        vGunFactor = tuningFactorMap.getVirtualGunFactor(distance, bearingDelta);
        considerShouldFire();
        sectorAimFactor = tuningFactorMap.getSectorAimFactor(distance, bearingDelta);
        escapeAreaFactor = tuningFactorMap.getEscapeAreaFactor(distance, bearingDelta);       
        

        selectSectorAimSector();
        selectEscapeAreaSector();
        double numAlternates = 1.1;
        int index = (int)Math.floor(Math.random() * numAlternates);
        vGunFactor.selectHighestResultsRatio(index);
        if (vGunFactor.getUses() < 10) {
            vGunFactor.select(MC_AIM_RANDOM_NARROW);
        }
        gun = (int)vGunFactor.getValue();
            
        return gun;
    }

    void considerShouldFire() {
        if (vGunFactor != previousVGunFactor || Math.random() < 0.08) {
            if (vGunFactor.getUses() > Math.random() * 100) {
                shouldFire = (vGunFactor.getHighestRatio() > 0.03 + Math.random() * 0.05); 
            }
            else {
                shouldFire = true;
            }
            previousVGunFactor = vGunFactor;
        }
    }

    private void guessLocation(double randomness, int direction, Point2D point) {
        double random = Math.random() * randomness;
        double factor = Math.abs(random / 45);
        double guessedDistance = robotLocation.distance(point);
        double guessedBearing = Rutils.pointsToAngle(robotLocation, point);
        if (direction == 0) {
            guessedBearing += Math.random() < 0.5 ? random : 0 - random;
        }
        else {
            guessedBearing += random * direction;
        }
        rBulletTravelTime = guessedBearing / rBulletVelocity;
        rBulletTravelTime += rBulletTravelTime * factor;
        Rutils.toLocation(guessedBearing, guessedDistance, robotLocation, point);
        translateInsideField(point);
    }

    private void selectSectorAimSector() {
        double numAlternates = 1.2;
        int index = (int)Math.floor(Math.random() * numAlternates);
        sectorAimFactor.selectHighestResultsRatio(index);
        Rutils.toLocation(absoluteBearing + sectorAimFactor.getValue(), distance, robotLocation, sectorAimLocation);
    }

    private void selectEscapeAreaSector() {
        double numAlternates = 1.2;
        int index = (int)Math.floor(Math.random() * numAlternates);
        escapeAreaFactor.selectHighestResultsRatio(index);
        Area escapeArea = Rutils.escapeArea(robotLocation, location, 80.0, 70.0,
                robot.getFieldRectangle(), rFirePower, velocity);
        double[] minMaxAngles = Rutils.escapeMinMaxAngles(robotLocation, location, escapeArea);
        minEscapeAngle = minMaxAngles[0];
        maxEscapeAngle = minMaxAngles[1];
        double relativeAngle = minEscapeAngle + escapeAreaFactor.getValue() *
            (maxEscapeAngle - minEscapeAngle) / MC_ESCAPE_AREA_SECTOR_MAX;
        Rutils.toLocation(absoluteBearing + relativeAngle, distance, robotLocation, escapeAreaLocation);
    }

    private double guessedGunHeading() {
        double relative = Rutils.normalRelativeAngle(Rutils.pointsToAngle(
            robotLocation, impactLocation) - Rutils.pointsToAngle(robotLocation, location));
        if (!isNormalFirePower()) {
            relative *= (rBulletVelocity / MC_MAX_BULLET_VELOCITY);
        }
        Rutils.toLocation(absoluteBearing + relative, robotLocation.distance(impactLocation),
            robotLocation, impactLocation);
        return Rutils.pointsToAngle(robotLocation, impactLocation);
    }

    private void translateInsideField(Point2D point) {
        double margin = width / 2;
        double X = Math.max(margin, Math.min( getBattleFieldWidth() - margin, point.getX()));
        double Y = Math.max(margin, Math.min( getBattleFieldHeight() - margin, point.getY()));
        point.setLocation(X, Y);
    }

    private void updatePosition(boolean calculateDeltas) {
        double oldAbsoluteBearing = absoluteBearing;
        absoluteBearing = robot.getHeading() + bearing;
        robotLocation.setLocation(robot.getLocation() );
        location.setLocation(robotLocation.getX() + Rutils.sin(absoluteBearing) * distance,
            robotLocation.getY() + Rutils.cos(absoluteBearing) * distance);
        if (calculateDeltas) {
            absoluteBearingDelta = Rutils.normalRelativeAngle(absoluteBearing - oldAbsoluteBearing);
        }
    }

    private double rBulletTravelTimeLinear() {
        double linearTime = rBulletTravelTime;
        double xVelocity = xDelta / timeDelta;
        double yVelocity = yDelta / timeDelta;
        linearTime *= 1 + Rutils.sign(location.getX() - robot.getX()) * xVelocity / 14;
        linearTime *= 1 + Rutils.sign(location.getY() - robot.getY()) * yVelocity / 14;
        return linearTime;
    }

    private Point2D guessedLocationLinear() {
        rBulletTravelTime = rBulletTravelTimeLinear();
        double xVelocity = xDelta / timeDelta;
        double yVelocity = yDelta / timeDelta;
        double impactXDelta = xVelocity * rBulletTravelTime;
        double impactYDelta = yVelocity * rBulletTravelTime;
        return new Point2D.Double(location.getX() + impactXDelta, location.getY() + impactYDelta);
    }

    private Point2D guessedLocationAngular() {
        Point2D newLocation = new Point2D.Double();
        Rutils.toLocation(absoluteBearing + Rutils.sign(bearingDelta) * 15.0, distance, robotLocation, newLocation);
        translateInsideField(newLocation);
        rBulletTravelTime = location.distance(newLocation) / rBulletVelocity;
        return newLocation;
    }

    private Point2D guessedLocationAngularFactored() {
        Point2D newLocation = new Point2D.Double();
        Rutils.toLocation(absoluteBearing + bearingDelta * angularFactor, distance, robotLocation, newLocation);
        translateInsideField(newLocation);
        rBulletTravelTime = location.distance(newLocation) / rBulletVelocity;
        return newLocation;
    }

    private Point2D guessedLocationOffsetFactored() {
        Point2D newLocation = new Point2D.Double();
        Rutils.toLocation(absoluteBearing + offsetFactor, distance, robotLocation, newLocation);
        translateInsideField(newLocation);
        rBulletTravelTime = location.distance(newLocation) / rBulletVelocity;
        return newLocation;
    }

    private Point2D guessedLocationMirrored() {
        Point2D newLocation = new Point2D.Double();
        Point2D robotDestination = robot.getNextLocation();
        double newX = center.getX() - (robotDestination.getX() - center.getX());
        double newY = center.getY() - (robotDestination.getY() - center.getY());
        newLocation.setLocation(newX, newY);
        rBulletTravelTime = location.distance(newLocation) / rBulletVelocity;
        return newLocation;
    }

    private void setRFirePower() {
        rFirePower = bestBulletPower;
        if (vGunFactor.getUses() > 100) {
            rFirePower = Math.max(0.1, bestBulletPower - (50 * (0.075 - Math.min(0.075, vGunFactor.getHighestRatio()))));
        }
        rFirePower = Math.min(rFirePower, energy / 4);
        if (distance > 100) {
            rFirePower = Math.min(rFirePower, robot.getEnergy() / 4);
        }
        rBulletVelocity = Rutils.bulletVelocity(rFirePower);
    }

    double getRFirePower() {
        return this.rFirePower;
    }

    public boolean isWinning() {
        return energy / robot.getEnergy() > 7.0;
    }

    long impactTime() {
        return robot.getTime() + (long)Math.round(rBulletTravelTime);
    }

    double impactX() {
        return impactLocation.getX();
    }

    double impactY() {
        return impactLocation.getY();
    }

    boolean isActive() {
        return active;
    }

    boolean isClose() {
        return distance < MC_CLOSE;
    }

    boolean isRecent() {
        return getAge() <= MC_RECENT_ENEMY;
    }

    boolean isRecent1v1() {
        return getAge() <= MC_RECENT_ENEMY_1V1;
    }

    boolean getIsFiring() {
        return isFiring;
    }

    public double getEFirePower() {
        return this.eFirePower;
    }

    public String getName() {
        return this.name;
    }

    public double getEnergy() {
        return this.energy;
    }

    double getBearing() {
        return this.bearing;
    }

    public double getAbsoluteBearing() {
        return this.absoluteBearing;
    }

    double getAbsoluteBearingDelta() {
        return this.absoluteBearingDelta;
    }

    public double getDistance() {
        return this.distance;
    }

    double getHeading() {
        return this.heading;
    }

    double getVelocity() {
        return this.velocity;
    }

    long getTime() {
        return this.time;
    }

    public Point2D getLocation() {
        return this.location;
    }

    double getX() {
        return this.location.getX();
    }

    double getY() {
        return this.location.getY();
    }

    public double getBestBulletPower() {
        return bestBulletPower;
    }

    boolean isNormalFirePower() {
        return (energy > 12 && robot.getEnergy() > 12);
    }

    void registerRFired() {
        this.rTotalFiredCount++;
        this.rFiredCount++;
        if (isNormalFirePower()) {
            for (int i = 0; i < virtualGuns.length; i++) {
                vGunFactor.incUses((double)i);
            }
            for (double sector = MC_SECTOR_AIM_MIN; sector <= MC_SECTOR_AIM_MAX; sector += sectorAimWidth) {
                sectorAimFactor.incUses(sector);
            }
            for (double sector = 0; sector <= MC_ESCAPE_AREA_SECTOR_MAX; sector += 1) {
                escapeAreaFactor.incUses(sector);
            }
        }
    }

    void registerEFired() {
        eTotalFiredCount++;
        eFiredCount++;
        eRunningFirePower.addValue(eFirePower);
    }

    void registerEHit() {
        this.eHits++;
        this.eTotalHits++;
    }

    void registerRHit() {
        this.rHits++;
    }

    void setDamage(double energy) {
        this.lastDamage = energy;
    }

    void registerRMiss() {
    }

    boolean isDisabled() {
        return this.disabled;
    }

    double getGuessedGunHeading() {
        return this.guessedGunHeading;
    }

    double getImpactX() {
        return this.impactLocation.getX();
    }

    double getImpactY() {
        return this.impactLocation.getY();
    }

    long getAge() {
        return Math.abs(robot.getTime() - this.time);
    }

    void setTuningFactors(Map factors) {
        this.tuningFactors = factors;
    }

    boolean checkVGunStatus(Object[] oVGuns, TuningFactor vgVirtualGunFactor,
                            TuningFactor vgSectorAimFactor, TuningFactor vgEscapeAreaFactor) {
        boolean keepChecking = false;
        VirtualGun[] vGuns = (VirtualGun[])oVGuns;
        for (int i = 0 ; i < vGuns.length; i++) {
            if (vGuns[i].active) {
                if (isNormalFirePower()) {
                    if (vGuns[i].hasHit()) {
                        vgVirtualGunFactor.incTuning((double)i);
                        vgVirtualGunFactor.addResult((double)i, 1.0);
                        if (i == MC_AIM_SECTOR_AIM) {
                            vGuns[i].updateStatisticalAims(vgSectorAimFactor, vgEscapeAreaFactor);
                        }
                    }
                    else if (vGuns[i].hasMissed()) {
                        vgVirtualGunFactor.addResult((double)i, 0.0);
                        if (i == MC_AIM_SECTOR_AIM) {
                            vGuns[i].updateStatisticalAims(vgSectorAimFactor, vgEscapeAreaFactor);
                        }
                    }
                }
                keepChecking = keepChecking || vGuns[i].active;
            }
        }
        return keepChecking;
    }

    VirtualGun[] getVirtualGuns() {
        return virtualGuns;
    }

    TuningFactor getVirtualGunFactor() {
        return vGunFactor;
    }

    TuningFactor getSectorAimFactor() {
        return sectorAimFactor;
    }

    TuningFactor getEscapeAreaFactor() {
        return escapeAreaFactor;
    }

    Map getTuningFactors() {
        return tuningFactors;
    }

    double getDifficulty() {
        double difficulty = 0;
        if (distance < MC_CLOSE) {
            difficulty *= 0.2;
        }
        return difficulty;
    }

    private double getRHitRatio() {
        return rFiredCount > 0 ? (double)rHits / (double)rFiredCount : 0.0;
    }

    private double getRTotalHitRatio() {
        return rTotalFiredCount > 0 ? (double)rTotalHits / (double)rTotalFiredCount : 0.0;
    }

    private double getEHitRatio() {
        return eFiredCount > 0 ? (double)eHits / (double)eFiredCount : 0.0;
    }

    private double getETotalHitRatio() {
        return eTotalFiredCount > 0 ? (double)eTotalHits / (double)eTotalFiredCount : 0.0;
    }

    void printStats() {
        System.out.println("Enemy : " + name);
        //((TuningFactor)tuningFactors.get("sectorAimFactorCorneredLeft")).printStats("sectorAimFactorCorneredLeft");
        //((TuningFactor)tuningFactors.get("sectorAimFactorCornered")).printStats("sectorAimFactorCornered");
        //((TuningFactor)tuningFactors.get("sectorAimFactorCorneredRight")).printStats("sectorAimFactorCorneredRight");
            //((TuningFactor)tuningFactors.get("sectorAimFactorCloseLeft")).printStats("sectorAimFactorCloseLeft");
            //((TuningFactor)tuningFactors.get("sectorAimFactorClose")).printStats("sectorAimFactorClose");
            //((TuningFactor)tuningFactors.get("sectorAimFactorCloseRight")).printStats("sectorAimFactorCloseRight");
            //((TuningFactor)tuningFactors.get("virtualGunFactorCloseLeft")).printStats("virtualGunFactorCloseLeft");
            //((TuningFactor)tuningFactors.get("virtualGunFactorClose")).printStats("virtualGunFactorClose");
            //((TuningFactor)tuningFactors.get("virtualGunFactorCloseRight")).printStats("virtualGunFactorCloseRight");
            //((TuningFactor)tuningFactors.get("escapeAreaFactorNormalLeft")).printStats("escapeAreaFactorNormalLeft");
            //((TuningFactor)tuningFactors.get("escapeAreaFactorNormal")).printStats("escapeAreaFactorNormal");
            //((TuningFactor)tuningFactors.get("escapeAreaFactorNormalRight")).printStats("escapeAreaFactorNormalRight");
            //((TuningFactor)tuningFactors.get("sectorAimFactorNormalLeft")).printStats("sectorAimFactorNormalLeft");
            //((TuningFactor)tuningFactors.get("sectorAimFactorNormal")).printStats("sectorAimFactorNormal");
            //((TuningFactor)tuningFactors.get("sectorAimFactorNormalRight")).printStats("sectorAimFactorNormalRight");
        /*
        if (Math.random() < 0.33) {
            ((TuningFactor)tuningFactors.get("virtualGunFactorVeryCloseLeft")).printStats("virtualGunFactorVeryCloseLeft");
            ((TuningFactor)tuningFactors.get("virtualGunFactorCloseLeft")).printStats("virtualGunFactorCloseLeft");
            ((TuningFactor)tuningFactors.get("virtualGunFactorNormalLeft")).printStats("virtualGunFactorNormalLeft");
            ((TuningFactor)tuningFactors.get("virtualGunFactorFarLeft")).printStats("virtualGunFactorFarLeft");
            ((TuningFactor)tuningFactors.get("virtualGunFactorVeryFarLeft")).printStats("virtualGunFactorVeryFarLeft");
        }
        else if (Math.random() < 0.5) {
            ((TuningFactor)tuningFactors.get("virtualGunFactorVeryClose")).printStats("virtualGunFactorVeryClose");
            ((TuningFactor)tuningFactors.get("virtualGunFactorClose")).printStats("virtualGunFactorClose");
            ((TuningFactor)tuningFactors.get("virtualGunFactorNormal")).printStats("virtualGunFactorNormal");
            ((TuningFactor)tuningFactors.get("virtualGunFactorFar")).printStats("virtualGunFactorFar");
            ((TuningFactor)tuningFactors.get("virtualGunFactorVeryFar")).printStats("virtualGunFactorVeryFar");
        }
        else {
            ((TuningFactor)tuningFactors.get("virtualGunFactorVeryCloseRight")).printStats("virtualGunFactorVeryCloseRight");
            ((TuningFactor)tuningFactors.get("virtualGunFactorCloseRight")).printStats("virtualGunFactorCloseRight");
            ((TuningFactor)tuningFactors.get("virtualGunFactorNormalRight")).printStats("virtualGunFactorNormalRight");
            ((TuningFactor)tuningFactors.get("virtualGunFactorFarRight")).printStats("virtualGunFactorFarRight");
            ((TuningFactor)tuningFactors.get("virtualGunFactorVeryFarRight")).printStats("virtualGunFactorVeryFarRight");
        }
        */
            //((TuningFactor)tuningFactors.get("sectorAimFactorFarLeft")).printStats("sectorAimFactorFarLeft");
            //((TuningFactor)tuningFactors.get("sectorAimFactorFar")).printStats("sectorAimFactorFar");
            //((TuningFactor)tuningFactors.get("sectorAimFactorFarRight")).printStats("sectorAimFactorFarRight");
            //((TuningFactor)tuningFactors.get("virtualGunFactorFarLeft")).printStats("virtualGunFactorFarLeft");
            //((TuningFactor)tuningFactors.get("virtualGunFactorFar")).printStats("virtualGunFactorFar");
            //((TuningFactor)tuningFactors.get("virtualGunFactorFarRight")).printStats("virtualGunFactorFarRight");
        //((TuningFactor)tuningFactors.get("virtualGunFactorClose")).printStats("virtualGunFactorClose");
        System.out.println("  Firepower average:" + eRunningFirePower.average());
    }

    class VirtualGun {
        private double oldBearing;
        private Point2D oldRLocation = new Point2D.Double();
        private double guessedBearing;
        private double bulletTravelTime;
        private double bulletPower;
        private double bulletVelocity;
        private Point2D impactPoint = new Point2D.Double();
        private Enemy enemy;
        private Point2D bulletLocation = new Point2D.Double();
        private long firedTime;
        private boolean active = true;
        private double precision = 20.0;
        private double escapeMinAngle;
        private double escapeMaxAngle;
        private double deltaBearing;

        public VirtualGun(Enemy enemy) {
            this.enemy = enemy;
        }

        void setBearing(double guessedBearing) {
            this.guessedBearing = guessedBearing;
            this.oldBearing = enemy.absoluteBearing;
            this.oldRLocation.setLocation(enemy.robotLocation);
            this.impactPoint.setLocation(enemy.impactLocation);
            this.bulletTravelTime = enemy.rBulletTravelTime;
            this.bulletPower = enemy.rFirePower;
            this.bulletVelocity = Rutils.bulletVelocity(bulletPower);
            this.firedTime = enemy.time;
            this.escapeMinAngle = enemy.minEscapeAngle;
            this.escapeMaxAngle = enemy.maxEscapeAngle;
            this.deltaBearing = enemy.bearingDelta;
        }

        void updateStatisticalAims(TuningFactor vgSectorAimFactor, TuningFactor vgEscapeAreaFactor) {
            double impactBearing = Rutils.pointsToAngle(oldRLocation, enemy.location);
            double bearingDiff = Rutils.normalRelativeAngle((impactBearing - oldBearing));
            if (Math.abs(bearingDelta) > 0) {
                enemy.angularFactor = Rutils.rollingAvg(enemy.angularFactor, bearingDiff / bearingDelta, 50, bulletPower);
            }
            enemy.offsetFactor = Rutils.rollingAvg(enemy.offsetFactor, bearingDiff, 50, bulletPower);
            for (double sector = MC_SECTOR_AIM_MIN; sector <= MC_SECTOR_AIM_MAX; sector += sectorAimWidth) {
                if (Math.abs(bearingDiff - sector) <= sectorAimWidth / 1.4) {
                    vgSectorAimFactor.incTuning(sector);
                    vgSectorAimFactor.addResult(sector, 1.0);
                }
                else {
                    vgSectorAimFactor.addResult(sector, 0.0);
                }
            }
            double sectorWidth = (escapeMaxAngle - escapeMinAngle) / MC_ESCAPE_AREA_SECTOR_MAX;
            for (double sector = 0; sector <= MC_ESCAPE_AREA_SECTOR_MAX; sector += 1) {
                double sectorRelativeAngle = escapeMinAngle + sector * sectorWidth;
                if (Math.abs(bearingDiff - sectorRelativeAngle) <= sectorWidth) {
                    vgEscapeAreaFactor.incTuning(sector);
                    vgEscapeAreaFactor.addResult(sector, 1.0);
                }
                else {
                    vgEscapeAreaFactor.addResult(sector, 0.0);
                }
            }
        }

        boolean hasHit() {
            Rutils.toLocation(guessedBearing, bulletVelocity * (enemy.time - firedTime), oldRLocation, bulletLocation);
            if (bulletLocation.distance(enemy.location) < precision) {
                active = false;
                return true;
            }
            else {
                return false;
            }
        }

        boolean hasMissed() {
            Rutils.toLocation(guessedBearing, bulletVelocity * (enemy.time - firedTime), oldRLocation, bulletLocation);
            if (oldRLocation.distance(bulletLocation) > oldRLocation.distance(enemy.location)) {
                active = false;
                return true;
            }
            else {
                return false;
            }
        }
    }
}

class Tracker implements MarshmallowConstants {
    static final char BREAK_KEY = (char)0;
    private StringBuffer pattern = new StringBuffer(MC_RECORDING_SIZE);
    private List movie = new ArrayList(MC_RECORDING_SIZE);
    private boolean movieIsFull = false;
    private int movieSize = 0;
    private Enemy enemy;
    private Marshmallow robot;

    Tracker(Enemy enemy, Marshmallow robot) {
        this.enemy = enemy;
        this.robot = robot;
    }

    Point2D guessedLocation() {
        return nextLocation(similarPeriodEndIndex());
    }

    long maxBulletTravelTime() {
        return (long)(enemy.distance * 1.3 / enemy.rBulletVelocity);
    }

    void record(double eHeadingDelta, double eVelocity, long timeDelta) {
        eHeadingDelta /= timeDelta;
        eVelocity /= timeDelta;
        for (int i = 0; i < timeDelta; i++) {
            record(new Frame(eHeadingDelta, eVelocity));
        }
    }

    void recordBreak(long timeDelta) {
        Frame breakFrame = new BreakFrame();
        for (int i = 0; i < timeDelta; i++) {
            record(breakFrame);
        }
    }

    void record(Frame frame) {
        movie.add(frame);
        pattern.append((char)(frame.getKey()));
        if (movieIsFull) {
            pattern.deleteCharAt(0);
            movie.remove(0);
        }
        else {
            movieSize++;
            movieIsFull = movieSize >= MC_RECORDING_SIZE;
        }
    }

    Point2D nextLocation(long index) {
        double newX = enemy.location.getX();
        double newY = enemy.location.getY();
        long travelTime = 0;
        long bulletTravelTime = 0;
        if (index > 0) {
            Frame.setHeading(enemy.heading);
            for (int i = (int)index; i < movieSize && travelTime <= bulletTravelTime; i++) {
                Frame frame = (Frame)movie.get(i);
                newX += frame.deltaX();
                newY += frame.deltaY();
                bulletTravelTime = (long)(robot.getLocation().distance(newX, newY) / enemy.rBulletVelocity);
                travelTime++;
                frame.advanceHeading();
            }
        }
        return new Point2D.Double(newX, newY);
    }

    private long similarPeriodEndIndex() {
        long index = -1;
        long matchIndex = -1;
        long matchLength = 0;
        long maxTryLength = Math.min(enemy.time, 600);
        if (maxTryLength > 1 && movieSize > maxTryLength + 1 + maxBulletTravelTime()) {
            long patternLength = movieSize - maxBulletTravelTime();
            String patternString = pattern.substring(0, (int)patternLength);
            String searchString  = pattern.substring((int)(movieSize - maxTryLength));
            long tryLength = maxTryLength;
            long upper = maxTryLength;
            do {
                boolean foundMatch = false;
                if (enemy.time > 150) {
                    index = patternString.lastIndexOf(searchString.substring((int)(maxTryLength - tryLength)));
                }
                else {
                    index = patternString.indexOf(searchString.substring((int)(maxTryLength - tryLength)));
                }
                if (index >= 0) {
                    long endIndex = index + tryLength;
                    if (patternLength > endIndex + maxBulletTravelTime() + 1 && 
                        patternString.substring((int)endIndex,
                            (int)(endIndex + maxBulletTravelTime() + 1)).indexOf(BREAK_KEY) < 0) {
                        foundMatch = true;
                        matchIndex = index;
                        matchLength = tryLength;
                        if (tryLength == maxTryLength) {
                            break;
                        }
                        tryLength += (upper - tryLength) / 2;
                    }
                }
                if (!foundMatch) {
                    upper = tryLength;
                    tryLength -= (tryLength - matchLength) / 2;
                }
            }
            while (tryLength > 1 && tryLength < upper - 1);
        }
        if (matchIndex >= 0) {
            return matchIndex + matchLength;
        }
        return matchIndex;
    }
}

class Frame implements MarshmallowConstants {
    static double heading;
    double headingDelta;
    double velocity;

    Frame() {
    }

    Frame(double headingDelta, double velocity) {
        this.headingDelta = headingDelta;
        this.velocity = velocity;
    }

    static void setHeading(double newHeading) {
        heading = newHeading;
    }

    void advanceHeading() {
        heading += headingDelta;
    }

    char getKey() {
        int key = 3;
        key = key + 11 * (int)((10.0 + Math.toDegrees(headingDelta)));
        key = key + (int)((MC_MAX_ROBOT_VELOCITY + velocity));
        return (char)(key);
    }

    double deltaX() {
        return Math.sin(heading) * velocity;
    }

    double deltaY() {
        return Math.cos(heading) * velocity;
    }
}

class BreakFrame extends Frame {
    char getKey() {
        return Tracker.BREAK_KEY;
    }

    double deltaX() {
        return 0;
    }

    double deltaY() {
        return 0;
    }
}
