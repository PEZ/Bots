package pez.tests;
import robocode.*;
import robocode.util.Utils;
import java.util.*;
import java.awt.geom.*;
import java.awt.Color;

//import robocode.robocodeGL.*; // GL
//import robocode.robocodeGL.system.*; // GL
//import java.lang.reflect.*; // GL

// This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
// http://robowiki.net/?RWPCL
//
// AAGF, by PEZ. Although a pugilist needs strong and accurate fists, he/she even more needs an evasive movement.
//
// Ideas and concepts are often my own, but i have borrowed from many places too. Quite often from Jamougha and Kawigi.
//
// $Id: AAGF.java,v 1.38 2004/03/19 12:32:28 peter Exp $

public class AAGF extends AdvancedRobot {
    static final double MAX_VELOCITY = 8;

    static final double MAX_WALL_SMOOTH_TRIES = 100;
    static final double WALL_MARGIN = 28;

    static final double MAX_DISTANCE = 900;
    static final double MAX_BULLET_POWER = 3.0;
    static final double BULLET_POWER = 1.9;

    static final int FACTORS = 27;
    static final int MIDDLE_FACTOR = (FACTORS - 1) / 2;
    static final int DISTANCE_INDEXES = 5;
    static final int BULLET_POWER_INDEXES = 5;
    static final int VELOCITY_INDEXES = 5;
    static final int LAST_VELOCITY_INDEXES = 5;
    static final int LAST_HIT_INDEXES = 5;
    static final int WALL_INDEXES = 3;
    static final int DECCEL_TIME_INDEXES = 6;

    static final double WALL_BOUNCE_LIMIT = 27;
    static final double WALL_BOUNCE_INCREMENT = 9;

    static Rectangle2D fieldRectangle;
    static Point2D robotLocation = new Point2D.Double();
    static Point2D enemyLocation = new Point2D.Double();
    static double enemyAbsoluteBearing;
    static double enemyDistance;
    static int distanceIndex;
    static double enemyVelocity;
    double enemyEnergy;
    static int enemyTimeSinceDeccel;
    static double enemyBearingDirection = 0.75;
    static int[][][][][][][] aimFactors = new int[DISTANCE_INDEXES][VELOCITY_INDEXES][LAST_VELOCITY_INDEXES]
	[DECCEL_TIME_INDEXES][WALL_INDEXES][LAST_HIT_INDEXES][FACTORS];

    static double enemyFirePower = 2.5;
    static int[][][] moveFactors = new int[DISTANCE_INDEXES][BULLET_POWER_INDEXES][FACTORS];
    static double robotVelocity;
    static AAGFEnemyWave passingWave;
    static double visitsForward;
    static double visitsReverse;
    static int enemyHits;
    Point2D robotDestination;

    public void run() {
	setAdjustRadarForGunTurn(true);
	setAdjustGunForRobotTurn(true);

	fieldRectangle = new Rectangle2D.Double(WALL_MARGIN, WALL_MARGIN,
	    getBattleFieldWidth() - WALL_MARGIN * 2, getBattleFieldHeight() - WALL_MARGIN * 2);

	passingWave = null;

	do {
	    turnRadarRightRadians(Double.POSITIVE_INFINITY); 
	} while (true);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
	AAGFWave wave = new AAGFWave();
	wave.robot = this;
	AAGFEnemyWave ew = new AAGFEnemyWave();
	ew.robot = this;
	ew.gunLocation = new Point2D.Double(enemyLocation.getX(), enemyLocation.getY());
	ew.startBearing = ew.gunBearing(robotLocation);

	double enemyDeltaEnergy = enemyEnergy - e.getEnergy();
	if (enemyDeltaEnergy > 0 && enemyDeltaEnergy <= 3.0) {
	    enemyFirePower = enemyDeltaEnergy;
	    ew.surfable = true;
	}
	enemyEnergy = e.getEnergy();
	ew.bulletVelocity = bulletVelocity(enemyFirePower);

	ew.bearingDirection = 0.85 * robotBearingDirection(ew.startBearing) / (double)MIDDLE_FACTOR;

	ew.visits = moveFactors
	    [(int)Math.min(3, (enemyDistance / (MAX_DISTANCE / 4)))]
	    [(int)(enemyFirePower / 0.66)]
	    ;
	ew.targetLocation = robotLocation;

	robotLocation.setLocation(new Point2D.Double(getX(), getY()));
	enemyAbsoluteBearing = getHeadingRadians() + e.getBearingRadians();
	enemyLocation.setLocation(project(wave.gunLocation = new Point2D.Double(getX(), getY()), enemyAbsoluteBearing, enemyDistance));
	wave.targetLocation = enemyLocation;
	enemyDistance = e.getDistance();

	ew.advance(2);
	addCustomEvent(ew);

	// <gun>
	distanceIndex = (int)Math.min(DISTANCE_INDEXES - 1, (enemyDistance / (MAX_DISTANCE / DISTANCE_INDEXES)));
	int lastVelocityIndex = (int)Math.abs(enemyVelocity) / 2;
	int velocityIndex = (int)Math.abs((enemyVelocity = e.getVelocity()) / 2);
	if (velocityIndex < lastVelocityIndex) {
	    enemyTimeSinceDeccel = 0;
	}

	//double bulletPower = MAX_BULLET_POWER; // TargetingChallenge
	double bulletPower = Math.min(enemyEnergy / 4, distanceIndex > 0 ? BULLET_POWER : MAX_BULLET_POWER);
	wave.bulletVelocity = bulletVelocity(bulletPower);

	if (enemyVelocity != 0) {
	    enemyBearingDirection = 0.73 * sign(enemyVelocity * Math.sin(e.getHeadingRadians() - enemyAbsoluteBearing));
	}
	wave.bearingDirection = enemyBearingDirection / (double)MIDDLE_FACTOR;

	wave.startBearing = enemyAbsoluteBearing;

	wave.visits = aimFactors[distanceIndex][velocityIndex][lastVelocityIndex]
	    [(int)minMax(Math.pow(500 * enemyTimeSinceDeccel++ / enemyDistance, 0.5) - 1, 0, 5)][wallIndex(wave, 20)][AAGFWave.lastHitIndex];

	setTurnGunRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getGunHeadingRadians() +
		    wave.bearingDirection * (wave.mostVisited() - MIDDLE_FACTOR)));

	if (getEnergy() >= BULLET_POWER) {
	    if (setFireBullet(bulletPower) != null) {
		wave.isReal = true;
		wave.shotBearing = getGunHeadingRadians();
	    }
	    addCustomEvent(wave);
	}
	// </gun>

	if (robotDestination == null) {
	    updateDirectionStats(ew);
	}
	double angle;
	setAhead(Math.cos(angle = wave.gunBearing(robotDestination) - getHeadingRadians()) * 100);
	setTurnRightRadians(Math.tan(angle));

	setTurnRadarRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getRadarHeadingRadians()) * 2);

	visitsForward = visitsReverse = 0;

//	if (ew.surfable) { // GL
//	    ew.grapher = new WaveGrapher(ew); // GL
//	} // GL
    }

    public void onHitByBullet(HitByBulletEvent e) {
	AAGFEnemyWave wave = passingWave;
	if (wave != null) {
	    wave.registerVisits(++enemyHits);
	}
    }

    static Point2D wallSmoothedDestination(double deltaBearing) {
	Point2D destination;
	double direction = -1;
	double tries;
	do {
	    direction = -direction;
	    tries = 0;
	    while (!fieldRectangle.contains(destination = project(enemyLocation,
			    absoluteBearing(enemyLocation, robotLocation) + deltaBearing * direction,
			    robotLocation.distance(enemyLocation) * (1.2 - tries / 100))) && tries < MAX_WALL_SMOOTH_TRIES) {
		tries++;
	    }
	} while (tries > WALL_BOUNCE_LIMIT + distanceIndex * WALL_BOUNCE_INCREMENT && direction > 0);
	return destination;
    }

    void updateDirectionStats(AAGFEnemyWave wave) {
	double bearingDirection = robotBearingDirection(enemyAbsoluteBearing + Math.PI);
	double timeToImpact = (wave.distanceToTarget() / wave.bulletVelocity);
	Point2D reverseDestination = wallSmoothedDestination(-Math.atan2(Math.max(2, timeToImpact - 4) * MAX_VELOCITY * bearingDirection, enemyDistance));
	visitsReverse += wave.smoothedVisits(reverseDestination);

	timeToImpact *= Math.pow(enemyDistance / 600, 0.5);
	Point2D forwardDestination = wallSmoothedDestination(Math.atan2(timeToImpact * MAX_VELOCITY * bearingDirection, enemyDistance));
	visitsForward += wave.smoothedVisits(forwardDestination);

	robotDestination = forwardDestination;
	if (visitsReverse < visitsForward) {
	    robotDestination = reverseDestination;
	}
//	wave.grapher.drawReverseDestination(reverseDestination, wave.smoothedVisits(reverseDestination)); // GL
//	wave.grapher.drawForwardDestination(forwardDestination, wave.smoothedVisits(forwardDestination)); // GL
    }

    static int wallIndex(AAGFWave wave, double d) {
	return fieldRectangle.contains(project(wave.gunLocation, wave.startBearing + wave.bearingDirection * (d / 2.0), enemyDistance)) ? 2 :
	    fieldRectangle.contains(project(wave.gunLocation, wave.startBearing + wave.bearingDirection * d, enemyDistance)) ? 1 : 0;
    }

    double robotBearingDirection(double enemyBearing) {
	return sign(getVelocity() * Math.sin(getHeadingRadians() - enemyBearing));
    }

    static double bulletVelocity(double bulletPower) {
	return 20 - 3 * bulletPower;
    }

    static Point2D project(Point2D sourceLocation, double angle, double length) {
	return new Point2D.Double(sourceLocation.getX() + Math.sin(angle) * length,
		sourceLocation.getY() + Math.cos(angle) * length);
    }

    static double absoluteBearing(Point2D source, Point2D target) {
	return Math.atan2(target.getX() - source.getX(), target.getY() - source.getY());
    }

    static int sign(double v) {
        return v < 0 ? -1 : 1;
    }

    static double minMax(double v, double min, double max) {
	return Math.max(min, Math.min(max, v));
    }
}

class AAGFWave extends Condition {
    static int[] lastHitIndex = 2;
    boolean isReal = false;
    double shotBearing;

    AAGF robot;
    double bulletVelocity;
    Point2D gunLocation;
    Point2D targetLocation;
    double startBearing;
    double bearingDirection;
    int[] visits;
    double distanceFromGun;

    public boolean test() {
	advance(1);
	if (passing(-18)) {
	    if (robot.getOthers() > 0) {
		registerVisits(1);
		if (isReal && targetLocation.distance(AAGF.project(gunLocation, shotBearing, distanceFromGun)) < 30) {
		    lastHitIndex = (int)(visitingIndex(targetLocation) / 5.5);
System.out.println(this + " - hit: " + lastHitIndex);
		}
	    }
	    robot.removeCustomEvent(this);
	}
	return false;
    }

    public boolean passing(double distanceOffset) {
	return distanceFromGun > gunLocation.distance(targetLocation) + distanceOffset;
    }

    void advance(int ticks) {
	distanceFromGun += ticks * bulletVelocity;
    }

    int visitingIndex(Point2D target) {
	return (int)AAGF.minMax(
	    Math.round(((Utils.normalRelativeAngle(gunBearing(target) - startBearing)) / bearingDirection) + AAGF.MIDDLE_FACTOR), 0 , AAGF.FACTORS - 1);
    }

    void registerVisits(int count) {
	visits[visitingIndex(targetLocation)] += count;
    }

    double gunBearing(Point2D target) {
	return AAGF.absoluteBearing(gunLocation, target);
    }

    double distanceToTarget() {
	return gunLocation.distance(targetLocation) - distanceFromGun;
    }

    int mostVisited() {
	int mostVisited = AAGF.MIDDLE_FACTOR, i = AAGF.FACTORS - 1;
	do  {
	    if (visits[--i] > visits[mostVisited]) {
		mostVisited = i;
	    }
	} while (i > 0);
	return mostVisited;
    }

    double smoothedVisits(Point2D destination) {
	return smoothedVisits(visitingIndex(destination));
    }

    double smoothedVisits(int index) {
	index = (int)AAGF.minMax(index, 0, AAGF.FACTORS - 2);
	double smoothed = 0;
	int i = 0;
	do {
	    smoothed += (double)visits[i] / Math.sqrt((double)(Math.abs(index - i) + 1.0));
	    i++;
	} while (i < AAGF.FACTORS);
	return smoothed / Math.sqrt(distanceToTarget() / bulletVelocity);
    }
}

class AAGFEnemyWave extends AAGFWave {
//    WaveGrapher grapher; // GL

    boolean surfable;

    public boolean test() {
	advance(1);
	if (passing(-18)) {
	    surfable = false;
	    AAGF.passingWave = this;
	}
	if (passing(18)) {
//	    if (grapher != null) { // GL
//		grapher.remove(); // GL
//	    } // GL
	    robot.removeCustomEvent(this);
	}
	else if (surfable) {
	    robot.updateDirectionStats(this);
	}
//	if (grapher != null) { // GL
//	    grapher.drawWave(); // GL
//	} // GL
	return false;
    }
}

/* GL 
class WaveGrapher {
    static GLRenderer renderer = GLRenderer.getInstance();
    static int counter = 0;
    String id;
    AAGFWave wave;
    PointGL[] dots;
    PointGL reverseDestination = new PointGL();
    PointGL forwardDestination = new PointGL();
    LabelGL reverseLabel = new LabelGL("");
    LabelGL forwardLabel = new LabelGL("");

    StatGrapher statGrapher;

    
    WaveGrapher(AAGFWave wave) {
	this.id = "" + counter++;
	this.wave = wave;
	this.dots = new PointGL[wave.visits.length];
	for (int i = 0; i < dots.length; i++) {
	    dots[i] = new PointGL();
	    dots[i].setColor(Color.BLUE);
	    if (i == (dots.length - 1) / 2) {
		dots[i].addLabel(new LabelGL(id));
	    }
	    renderer.addRenderElement(dots[i]);
	}
	reverseDestination.addLabel(reverseLabel);
	reverseDestination.setColor(Color.RED);
	reverseDestination.setSize(10);
	reverseDestination.setPosition(-100, -100);
	forwardDestination.addLabel(forwardLabel);
	forwardDestination.setColor(Color.GREEN);
	forwardDestination.setSize(10);
	forwardDestination.setPosition(-100, -100);
	renderer.addRenderElement(reverseDestination);
	renderer.addRenderElement(forwardDestination);
	statGrapher = new StatGrapher(Color.YELLOW, wave.visits);
	statGrapher.setFrame(0, 0, wave.robot.getBattleFieldWidth(), wave.robot.getBattleFieldHeight());
    }

    void drawWave() {
	float totalVisits = 0;
	for (int i = 0; i < wave.visits.length; i++) {
	    totalVisits += wave.smoothedVisits(i);
	}
	for (int i = 0; i < dots.length; i++) {
	    Point2D dot = AAGF.project(wave.gunLocation,
					  wave.startBearing + wave.bearingDirection * (i - (dots.length - 1) / 2),
					  wave.distanceFromGun);
	    dots[i].setPosition(dot.getX(), dot.getY());
	    float size = 15 * (float)wave.smoothedVisits(i) / totalVisits;
	    //float size = 15 * (float)wave.smoothedVisits(i) / totalVisits;
	    dots[i].setSize(size);
	}
	statGrapher.draw();
    }

    void drawDestination(PointGL destination, LabelGL label, Point2D coords, double value) {
	destination.setPosition(coords.getX(), coords.getY());
	label.setString(id + " : " + (int)value);
    }

    void drawReverseDestination(Point2D coords, double value) {
	drawDestination(reverseDestination, reverseLabel, coords, value);
    }

    void drawForwardDestination(Point2D coords, double value) {
	drawDestination(forwardDestination, forwardLabel, coords, value);
    }

    void remove() {
	for (int i = 0; i < dots.length; i++) {
	    dots[i].remove();
	}
	reverseDestination.remove();
	forwardDestination.remove();
	statGrapher.remove();
    }
}

// A modified version of Nano's http://robowiki.net/?StatGrapher
class StatGrapher {
    private LineGL[] graph;
    private Object stats;
    private Object selected;
    private double x, y, width, height;
    private double lineWidth = 2;
    private Color color = Color.GREEN;

    public StatGrapher(Color color, Object stats) {
	this.color = color;
	this.stats = this.selected = stats;
	graph = new LineGL[Array.getLength(selected) - 1];
	for (int i = 0; i < graph.length; i++) {
	    graph[i] = new LineGL();
	}

    }

    public void setFrame(double x, double y, double width, double height) {
	setX(x); setY(y); setWidth(width); setHeight(height);
    }

    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setWidth(double width) { this.width = width; }
    public void setHeight(double height) { this.height = height; }
    public void setStats(Object stats) { this.stats = this.selected = stats; }
    public void setColor(Color color) { this.color = color; }

    public void selectSegment(int[] indices) {
	Object temp = stats;

	for (int i = 0; indices != null && i < indices.length; i++)
	    temp = Array.get(temp, indices[i]);

	selected = temp;
    }

    public void draw() {
	double highestValue = 1;

	for (int i = 0; i < Array.getLength(selected); i++)
	    if (Array.getDouble(selected, i) > highestValue)
		highestValue = Array.getDouble(selected, i);

	for (int i = 0; i < graph.length; i++) {
	    graph[i].setColor(color);
	    graph[i].setWidth(lineWidth);
	    graph[i].setLine(x + width * i / graph.length, y + height * Array.getDouble(selected, i) / highestValue,
		    x + width * (i + 1) / graph.length, y + height * Array.getDouble(selected, i + 1) / highestValue);
	    GLRenderer.getInstance().addRenderElement(graph[i]);
	}
    }

    public void remove() {
	for (int i = 0; i < graph.length; i++) {
	    if (graph[i] != null)
		graph[i].remove();
	}
    }
}

 GL */
