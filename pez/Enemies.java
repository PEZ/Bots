package pez;
import pez.Rutils.*;
import robocode.*;
import java.util.*;

// Enemies Cache for Marshmallow the Robot, by PEZ
// $Id: Enemies.java,v 1.22 2004/02/20 09:55:35 peter Exp $

//Todo: Make this a HashMap.
//      Use Comparators.

class Enemies extends ArrayList implements MarshmallowConstants {
    private Marshmallow robot;
    private Enemy closestEnemy = null;
    private Enemy weakestEnemy = null;
    private Enemy easiestEnemy = null;
    private Enemy oldestEnemy = null;
    private int numRecent = 0;
    
    public Enemies(Marshmallow robot) {
        this.robot = robot;
    }

    void categorize() {
        double shortestDistance = 9999; // get rid of this
        double leastEnergy = MC_MAX_ENERGY;
        double easiest = 99999.0;
        long oldest = -1;
        closestEnemy = null;
        weakestEnemy = null;
        easiestEnemy = null;
        oldestEnemy = null;
        numRecent = 0;
        Iterator iterator = this.iterator();
        while (iterator.hasNext()) {
            Enemy enemy = (Enemy)iterator.next();
            if (enemy.isActive()) {
                if (enemy.isRecent()) {
                    numRecent++;
                }
                double distance = enemy.getDistance();
                if (distance < shortestDistance) {
                    shortestDistance = distance;
                    closestEnemy = enemy;
                }
                double energy = enemy.getEnergy();
                if (energy < leastEnergy) {
                    leastEnergy = energy;
                    weakestEnemy = enemy;
                }
                double difficulty = enemy.getDifficulty();
                if (difficulty < easiest) {
                    easiest = difficulty;
                    easiestEnemy = enemy;
                }
                long age = enemy.getAge();
                if (age > oldest) {
                    oldest = age;
                    oldestEnemy = enemy;
                }
            }
        }
    }

    void deactivate() {
        Iterator iterator = this.iterator();
        while (iterator.hasNext()) {
            Enemy enemy = (Enemy)iterator.next();
            enemy.deactivate();
        }
    }

    int getNumRecent() {
        return this.numRecent;
    }

    Enemy getOneOnOneEnemy() {
        if (size() < 1) {
            return null;
        }
        return (Enemy)get(0);
    }

    Enemy getClosest() {
        return this.closestEnemy;
    }

    Enemy getWeakest() {
        return this.weakestEnemy;
    }

    Enemy getEasiest() {
        return this.easiestEnemy;
    }

    double getRadarSweepTurn(double direction) {
        if (numRecent == robot.getOthers() && oldestEnemy != null) {
            double radarTurn = oldestEnemy.getAbsoluteBearing() - robot.getRadarHeading();
            return Rutils.normalRelativeAngle(radarTurn +
                Rutils.sign(radarTurn) * Math.min(Math.abs(oldestEnemy.getAbsoluteBearingDelta()) * 5, 22.5));
        }
        else {
            return 180 * direction;
        }
    }
}
