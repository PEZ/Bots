package pez.frankie;
import robocode.*;
import robocode.util.Utils;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.awt.Color;
import java.util.zip.*;
import java.io.*;

// Frankie - Pattern matching gun and random movement
// http://robowiki.dyndns.org/?Frankie
// $Id: Frankie.java,v 1.14 2003/08/20 08:15:58 peter Exp $

public class Frankie extends AdvancedRobot {
    static boolean isMovementChallenge = false;
    //gun
    static final double MAX_VELOCITY = 8;
    static final long PM_LENGTH = 8000;
    static final char BREAK_KEY = (char)0;
    static final int NO_BEARING = -1000;

    static StringBuffer pattern = new StringBuffer((int)PM_LENGTH);
    static ArrayList movie = new ArrayList((int)PM_LENGTH);
    static boolean movieIsFull = false;
    static long movieSize = 0;
    Point2D robotLocation = new Point2D.Double();
    Point2D enemyLocation = new Point2D.Double();
    double bulletPower = 3;
    double bulletVelocity = 11;
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

    //movement
    static final double DEFAULT_DISTANCE = 890;
    static final double WALL_MARGIN = 18;
    static final double MAX_TRIES = 125;
    static final double REVERSE_TUNER = 0.421075;
    static final double WALL_BOUNCE_TUNER = 0.699484;
    static double direction = 0.4;
    static double enemyFirePower = 100;
    static int GF1Hits;
    static double tries;

    public void run() {
        setColors(Color.green.darker().darker(), Color.blue.brighter().brighter(), Color.yellow.brighter());
        setAdjustRadarForGunTurn(true);
        setAdjustGunForRobotTurn(true);
        setEventPriority("ScannedRobotEvent", 99);
        recordBreak(1);
        do {
	    turnRadarRightRadians(Double.POSITIVE_INFINITY); 
        } while (true);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        robotLocation.setLocation(getX(), getY());
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
        enemyLocation = project(robotLocation, enemyBearing, enemyDistance);
	/*
        double energyDelta = enemyEnergy - e.getEnergy();
        if (energyDelta >= 0.1 && energyDelta <= 3.0) {
            enemyBulletPower = energyDelta;
        }
	*/
        enemyEnergy = e.getEnergy();
        long timeDelta = getTime() - time;
        time += timeDelta;
        record(Utils.normalRelativeAngle(headingDelta), e.getVelocity(), timeDelta);
        if (timeDelta > 1) {
            //System.out.println("timeDelta: " + timeDelta);
        }
        bulletPower = bulletPower(enemyEnergy);
        bulletVelocity = bulletVelocity(bulletPower);
        aim();
	double gunAlignmentDelta = Utils.normalRelativeAngle(getGunHeadingRadians() - guessedEnemyBearing);
	if (time > 0 && Math.abs(gunAlignmentDelta) < Math.atan2(45, enemyDistance)) {
	    if (!isMovementChallenge && getEnergy() > 0.2) {
		setFire(bulletPower);
	    }
	}

	// <movement>
	Point2D robotDestination;
	Rectangle2D fieldRectangle = new Rectangle2D.Double(WALL_MARGIN, WALL_MARGIN,
	    getBattleFieldWidth() - WALL_MARGIN * 2, getBattleFieldHeight() - WALL_MARGIN * 2);
	tries = 0;
	while (!fieldRectangle.contains(robotDestination = project(enemyLocation, enemyBearing + Math.PI + direction, enemyDistance * (1.2 - tries / 100.0))) && tries < MAX_TRIES) {
	    tries++;
	}
	if (GF1Hits > 2 && (Math.random() < (bulletVelocity(enemyFirePower) / REVERSE_TUNER) / enemyDistance ||
		tries > (enemyDistance / bulletVelocity(enemyFirePower) / WALL_BOUNCE_TUNER))) {
	    direction = -direction;
	}
	// Jamougha's cool way
	double angle;
	setAhead(Math.cos(angle = absoluteBearing(robotLocation, robotDestination) - getHeadingRadians()) * 100);
	setTurnRightRadians(Math.tan(angle));
	// </movement>

        timeSinceLastScan = 0;

        setTurnRadarRightRadians(Utils.normalRelativeAngle(enemyBearing - getRadarHeadingRadians()) * 2);
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

    public void onHitByBullet(HitByBulletEvent e) {
	if (tries < 20) {
	    GF1Hits++;
	}
	enemyFirePower = e.getPower();
    }

    private double bulletPower(double enemyEnergy) {
        double power = enemyDistance < 150 ? 3 : 2.4;
        return Math.min(2.4, enemyEnergy / 4);
    }

    private void aim() {
        if (enemyBearing != NO_BEARING || getGunHeat() / getGunCoolingRate() < 4) {
            if (!isMovementChallenge) {
                guessedEnemyBearing = projectedBearing(similarPeriodEndIndex());
            }
            else {
                guessedEnemyBearing = enemyBearing;
            }
        }
        setTurnGunRightRadians(Utils.normalRelativeAngle(guessedEnemyBearing - getGunHeadingRadians()));
    }

    private int sign(double v) {
        return v > 0 ? 1 : -1;
    }

    static double bulletVelocity(double power) {
	return 20 - 3 * power;
    }

    static Point2D project(Point2D sourceLocation, double angle, double length) {
        return new Point2D.Double(sourceLocation.getX() + Math.sin(angle) * length,
            sourceLocation.getY() + Math.cos(angle) * length);
    }

    private double absoluteBearing(Point2D source, Point2D target) {
        return Math.atan2(target.getX() - source.getX(), target.getY() - source.getY());
    }

    long maxBulletTravelTime() {
        return (long)(enemyDistance * 1.3 / bulletVelocity);
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
                if (time > 150) {
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
        if (!isMovementChallenge) {
            try {
                ZipOutputStream zipout = new ZipOutputStream(new RobocodeFileOutputStream(getDataFile(enemyName + ".zip")));
                zipout.putNextEntry(new ZipEntry(enemyName));
                PrintWriter out = new PrintWriter(new OutputStreamWriter(zipout)); 
                for (int i = 0; i < Math.min(movieSize, PM_LENGTH / 7); i++) {
                    Frame frame = (Frame)movie.get(i);
                    out.println(frame.headingDelta);
                    out.println(frame.velocity);
                }
                out.flush();
                out.close();
            }
            catch (Exception e) {
                System.out.println("Error saving movie: " + e);
            }
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
                    record(headingDelta, velocity, 1);
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
        if (!movieIsSaved) {
            movieIsSaved = true;
            saveMovie(enemyName);
        }
    }
}

class Frame {
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
        key = key + (int)((Frankie.MAX_VELOCITY + velocity));
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
        return Frankie.BREAK_KEY;
    }

    double deltaX() {
        return 0;
    }

    double deltaY() {
        return 0;
    }
}
