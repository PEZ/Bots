package pez.etc;
import robocode.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.awt.Color;
import java.util.zip.*;
import java.io.*;

// LeachPMC by PEZ - the Leach pattern matcher for the:
// http://robowiki.dyndns.org/?PatternMatcherChallenge
// $Id: LeachPMC.java,v 1.13 2003/08/20 08:15:58 peter Exp $

// http://robowiki.dyndns.org/?LeachPMC

// This is now open source. Meaning you should consider making any bot using significant parts
// of this code open source as well. Or,if you choose not to, at least give me the credit you think I
// deserve. I worked many days with this.

// Todo: Create a Movie class. Use different movies for the pre and post pattern phases
public class LeachPMC extends AdvancedRobot {
    static final long PM_LENGTH = 7000;
    static final char BREAK_KEY = (char)0;
    static final int NO_BEARING = -1000;

    static StringBuffer pattern = new StringBuffer((int)PM_LENGTH);
    static ArrayList movie = new ArrayList((int)PM_LENGTH);
    static boolean movieIsFull = false;
    static long movieSize = 0;
    Point2D robotLocation = new Point2D.Double();
    Point2D enemyLocation = new Point2D.Double();
    static double bulletPower = 3.0; //0.5;
    static double bulletVelocity;
    String enemyName;
    double enemyDistance;
    double enemyBearing = NO_BEARING;
    double enemyHeading;
    double enemyEnergy = 100;
    double guessedEnemyBearing;
    double enemyLateralVelocity;
    long timeSinceLastScan;
    long time;
    long maxMatchLength;
    long accumulatedMatchLength;
    long searches;
    static long skippedTurns;
    static boolean movieIsRestored = false;
    static boolean movieIsSaved = false;
    boolean isInitialPhase = true;

    public void run() {
        setColors(Color.black, Color.yellow.brighter().brighter(), Color.yellow.brighter());
        setEventPriority("ScannedRobotEvent", 99);
        recordBreak(1);
        robotLocation.setLocation(getX(), getY());
        do {
            doScanner();
	    bulletVelocity = 20 - 3 * Math.min(getEnergy(), bulletPower);
            double gunAlignmentDelta = normalRelativeAngle(getGunHeadingRadians() - guessedEnemyBearing);
            if (time > 0 && Math.abs(gunAlignmentDelta) < Math.atan2(16.5, enemyDistance)) {
                if (!(isInitialPhase && time < 40 && enemyDistance > 410)) {
                    setFire(bulletPower);
                }
            }
            execute();
        } while (true);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        enemyName = e.getName();
        if (!movieIsRestored) {
            restoreMovie(enemyName);
            movieIsRestored = true;
        }
        enemyDistance = e.getDistance();
        double headingDelta = e.getHeadingRadians() - enemyHeading;
        enemyHeading += headingDelta;
        enemyBearing = getHeadingRadians() + e.getBearingRadians();
        enemyLateralVelocity = e.getVelocity() * Math.sin(e.getHeadingRadians() - enemyBearing); 
        toLocation(enemyBearing, enemyDistance, robotLocation, enemyLocation);
        long patternStartTime = 0;
        double energyDelta = e.getEnergy() - enemyEnergy;
        if (isInitialPhase && Math.abs(energyDelta) > 0.4 && Math.abs(energyDelta) < 0.6) {
            patternStartTime = getTime();
            isInitialPhase = false;
        }
        enemyEnergy = e.getEnergy();
        long timeDelta = getTime() - time;
        time += timeDelta;
        record(normalRelativeAngle(headingDelta), e.getVelocity(), timeDelta, isInitialPhase);
        if (timeDelta > 1) {
            //System.out.println("timeDelta: " + timeDelta);
        }
        aim();
        timeSinceLastScan = 0;
    }

    public void onDeath(DeathEvent e) {
        finishRound();
    }

    public void onWin(WinEvent e) {
        finishRound();
    }

    public void onSkippedTurn(SkippedTurnEvent e) {
        skippedTurns++;
    }

    private void doScanner() {
        double radarOffset = Double.POSITIVE_INFINITY;
        if(getOthers() == 1 && timeSinceLastScan < 3) {
            radarOffset = normalRelativeAngle(getRadarHeadingRadians() - enemyBearing);
            radarOffset += sign(radarOffset) * 0.02;
        }
        setTurnRadarLeftRadians(radarOffset);
        timeSinceLastScan++;
    }

    private void aim() {
        if (enemyBearing != NO_BEARING || getGunHeat() / getGunCoolingRate() < 5) {
            guessedEnemyBearing = projectedBearing(similarPeriodEndIndex());
        }
        setTurnGunRightRadians(normalRelativeAngle(guessedEnemyBearing - getGunHeadingRadians()));
    }

    private int sign(double v) {
        return v > 0 ? 1 : -1;
    }

    private void toLocation(double angle, double length, Point2D sourceLocation, Point2D targetLocation) {
        targetLocation.setLocation(sourceLocation.getX() + Math.sin(angle) * length,
            sourceLocation.getY() + Math.cos(angle) * length);
    }

    private double absoluteBearing(Point2D source, Point2D target) {
        return Math.atan2(target.getX() - source.getX(), target.getY() - source.getY());
    }

    private double normalRelativeAngle(double angle) {
        return Math.atan2(Math.sin(angle), Math.cos(angle));
    }

    long maxBulletTravelTime() {
        return (long)(enemyDistance * 1.3 / bulletVelocity);
    }

    void record(double eHeadingDelta, double eVelocity, long timeDelta, boolean initialFlag) {
        eHeadingDelta /= timeDelta;
        eVelocity /= timeDelta;
        for (int i = 0; i < timeDelta; i++) {
            record(new Frame(eHeadingDelta, eVelocity, initialFlag));
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
            movieIsFull = movieSize >= PM_LENGTH;
        }
    }

    double projectedBearing(long index) {
        double newX = enemyLocation.getX();
        double newY = enemyLocation.getY();
        long travelTime = 0;
        long bulletTravelTime = 0;
        if (index > 0) {
            Frame.setHeading(enemyHeading);
            for (int i = (int)index; i < movieSize && travelTime <= bulletTravelTime; i++) {
                Frame frame = (Frame)movie.get(i);
                newX += frame.deltaX();
                newY += frame.deltaY();
                bulletTravelTime = (long)(robotLocation.distance(newX, newY) / bulletVelocity);
                travelTime++;
                frame.advanceHeading();
            }
        }
        return absoluteBearing(robotLocation, new Point2D.Double(newX, newY));
    }

    private long similarPeriodEndIndex() {
        long index = -1;
        long matchIndex = -1;
        long matchLength = 0;
        long maxTryLength = Math.min(getTime(), 600);
        searches++;
        if (maxTryLength > 1 && movieSize > maxTryLength + 1 + maxBulletTravelTime()) {
            long patternLength = movieSize - maxBulletTravelTime();
            String patternString = pattern.substring(0, (int)patternLength);
            String searchString  = pattern.substring((int)(movieSize - maxTryLength));
            long tryLength = maxTryLength;
            long upper = maxTryLength;
            do {
                boolean foundMatch = false;
                if (time > 600) {
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
        updateStats(matchLength);
        if (matchIndex >= 0) {
            return matchIndex + matchLength;
        }
        return matchIndex;
    }

    private void saveMovie(String enemyName) {
        try {
            ZipOutputStream zipout = new ZipOutputStream(new RobocodeFileOutputStream(getDataFile(enemyName + ".zip")));
            zipout.putNextEntry(new ZipEntry(enemyName));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(zipout)); 
            for (int i = 0; i < Math.min(movieSize, PM_LENGTH); i++) {
                Frame frame = (Frame)movie.get(i);
                out.println(frame.headingDelta);
                out.println(frame.velocity);
                out.println(frame.isInitialPhase ? "1" : "0");
            }
            out.flush();
            out.close();
        }
        catch (Exception e) {
            System.out.println("Error saving movie: " + e);
        }
    }

    private void restoreMovie(String enemyName) {
        try {
            ZipInputStream zipin = new ZipInputStream(new FileInputStream(getDataFile(enemyName + ".zip")));
            zipin.getNextEntry();
            BufferedReader in = new BufferedReader(new InputStreamReader(zipin));
            String line;
            do {
                line = in.readLine();
                if (line != null) {
                    double headingDelta = Double.parseDouble(line);
                    line = in.readLine();
                    double velocity = Double.parseDouble(line);
                    line = in.readLine();
                    boolean initialFlag = (Integer.parseInt(line) == 1 ? true : false);
                    record(headingDelta, velocity, 1, initialFlag);
                }
            } while (line != null);
            in.close();
        }
        catch (Exception e) {
            System.out.println("Error restoring movie: " + e);
        }
    }

    private void updateStats(long matchLength) {
        if (matchLength > maxMatchLength) {
            maxMatchLength = matchLength;
        }
        accumulatedMatchLength += matchLength;
    }

    private void finishRound() {
        if (getRoundNum() == getNumRounds() -1 && !movieIsSaved) {
            movieIsSaved = true;
            saveMovie(enemyName);
        }
        System.out.println("maxMatchLength: " + maxMatchLength);
        System.out.println("average: " + accumulatedMatchLength / searches);
        System.out.println("Total skipped turns: " + skippedTurns);
    }
}

class Frame {
    static double heading;
    double headingDelta;
    double velocity;
    boolean isInitialPhase;

    Frame() {
    }

    Frame(double headingDelta, double velocity, boolean isInitialPhase) {
        this.headingDelta = headingDelta;
        this.velocity = velocity;
        this.isInitialPhase = isInitialPhase;
    }

    static void setHeading(double newHeading) {
        heading = newHeading;
    }

    void advanceHeading() {
        heading += headingDelta;
    }

    char getKey() {
        int key = 3;
        key = key + 100 * (isInitialPhase ? 1 : 0);
        key = key + 11 * (int)((10.0 + Math.toDegrees(headingDelta)) * 3);
        key = key + (int)((8.0 + velocity));
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
        return LeachPMC.BREAK_KEY;
    }

    double deltaX() {
        return 0;
    }

    double deltaY() {
        return 0;
    }
}
