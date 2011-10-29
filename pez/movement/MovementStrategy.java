package pez.movement;

import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.util.Arrays;

import pez.Marshmallow;
import pez.Enemy;
import pez.Rutils;
import pez.movement.Driver;

// $Id: MovementStrategy.java,v 1.14 2004/02/20 22:29:12 peter Exp $
public abstract class MovementStrategy implements Comparable, pez.MarshmallowConstants {
    static double c_rateDepth = 100000;
    static MovementStrategy c_currentMovement;

    protected Point2D oldEnemyLocation = new Point2D.Double();
    protected Point2D oldRobotLocation = new Point2D.Double();
    protected Factor[] moveFactors;
    protected double velocity = 8;
    protected int factorIndex = 0;
    protected double moveFactor;
    protected String m_name = "John Dough the Movement";
    protected double m_ratio = 50;
    protected int m_uses = 0;
    
    public MovementStrategy() {
        moveFactors = new Factor[] { new Factor(0.72) };
    }

    public static MovementStrategy getNewMovement() {
        c_currentMovement = new RandomMovement();
        //c_currentMovement = new AMovement();
        return c_currentMovement;
    }
    
    public abstract MovementData getMovementData(Enemy enemy, Marshmallow robot);

    public void updateRatio(double ratio) {
        m_uses++;
        this.m_ratio = Rutils.rollingAvg(m_ratio, ratio, Math.min(getUses(), c_rateDepth), 1);
        moveFactors[factorIndex].updateRatio(ratio);
    }
    
    public boolean isFinished(Driver driver) {
        return driver.isFinished();
    }

    public void setNewVelocity() {
    }

    double distanceExtra(Enemy enemy, Marshmallow robot, double cornerMargin) {
        double distanceExtra = 5;
        if (doRam(enemy, robot)) {
            distanceExtra = -12;
        }
        else if (robot.isLoosing(enemy)) {
            distanceExtra = -3;
        }
        else if (robot.isCornered(cornerMargin) && !(enemy.getDistance() < 200)) {
            distanceExtra = -0.5;
        }
        else if (enemy.getDistance() < 200) {
            distanceExtra = 12;
        }
        else if (enemy.getDistance() > MC_WANTED_DISTANCE) {
            distanceExtra = -1;
        }
        return distanceExtra;
    }
    
    boolean doRam(Enemy enemy, Marshmallow robot) {
        return enemy.getEnergy() == 0.0 && robot.getOthers() == 1;
    }

    public int compareTo(Object o) {
        MovementStrategy movement = (MovementStrategy) o;
        if (this.getRatio() > movement.getRatio()) {
            return -1;
        }
        if (this.getRatio() < movement.getRatio()) {
            return +1;
        }
        return 0;
    }
    
    public boolean equals(Object object) {
        if (object instanceof MovementStrategy) {
            return (((MovementStrategy)object).getRatio() == this.getRatio());
        }
        return false;
    }
    
    public void initRound() {
        int randomIndex = (int)Math.floor(Math.random() * moveFactors.length);
        Arrays.sort(moveFactors);
        double lowestUseCount = moveFactors[moveFactors.length - 1].getUses();
        if (lowestUseCount == 0 || Math.random() > lowestUseCount / 850.0) {
            factorIndex = randomIndex;
        }
        else {
            factorIndex = 0;
        }
        moveFactor = moveFactors[factorIndex].getValue();
    }

    public int getUses() {
        return m_uses;
    }
    
    public String getName() {
        return m_name;
    }
    
    public double getRatio() {
        return m_ratio;
    }
    
    public double getVelocity() {
        return velocity;
    }

    public void printStats() {
        System.out.println("Movement strategies");
        System.out.println("  " + c_currentMovement.getName() + " (" + c_currentMovement.getUses() + ") " +
            ": " + c_currentMovement.getRatio());
        System.out.println("  moveFactors:");
        Arrays.sort(c_currentMovement.moveFactors);
        for (int i = 0; i < c_currentMovement.moveFactors.length; i++) {
            Factor factor = c_currentMovement.moveFactors[i];
            System.out.println("    " + factor.getValue() + " (" + factor.getUses() + ") " + factor.getRatio());
        }
    }
}

class Factor implements Comparable {
    private double value;
    private int uses;
    private double m_ratio = 50;

    Factor(double value) {
        this.value = value;
    }

    public int compareTo(Object o) {
        Factor f = (Factor) o;
        if (this.getRatio() > f.getRatio()) {
            return -1;
        }
        if (this.getRatio() < f.getRatio()) {
            return +1;
        }
        return 0;
    }
    
    public boolean equals(Object object) {
        if (object instanceof Factor) {
            return (((Factor)object).getRatio() == this.getRatio());
        }
        return false;
    }
    
    double getValue() {
        return this.value;
    }

    void updateRatio(double ratio) {
        uses++;
        this.m_ratio = Rutils.rollingAvg(m_ratio, ratio, Math.min(uses, MovementStrategy.c_rateDepth), 1);
    }

    int getUses() {
        return this.uses;
    }

    double getRatio() {
        return this.m_ratio;
    }
}
