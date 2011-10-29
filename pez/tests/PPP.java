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
// Precise Prediction Pugilist, by PEZ. Just a test.
//
// Ideas and concepts are often my own, but i have borrowed from many places too. Quite often from Jamougha and Kawigi.
//
// This particular bot contains a movement predictor devoloped by Rozu and tuned by Jim and myself. See
// http://robowiki.net/?Apollon for some more info on the predictor.
//
// $Id: PPP.java,v 1.38 2004/03/19 12:32:28 peter Exp $

public class PPP extends AdvancedRobot {
    static final double BOT_WIDTH = 36;
    static final double MAX_VELOCITY = 8;

    static final double BATTLE_FIELD_WIDTH = 800;
    static final double BATTLE_FIELD_HEIGHT = 600;
    static final double MAX_WALL_SMOOTH_TRIES = 100;
    static final double WALL_MARGIN = 35;

    static final double MAX_DISTANCE = 1000;
    static final double MAX_BULLET_TRAVEL_TIME = 80;
    static final double MAX_BULLET_POWER = 3.0;
    static final double BULLET_POWER = 2.3;

    static final int FACTORS = 27;
    static final int MIDDLE_FACTOR = (FACTORS - 1) / 2;
    static final int DISTANCE_INDEXES = 5;
    static final int VELOCITY_INDEXES = 5;
    static final int LAST_VELOCITY_INDEXES = 5;
    static final int WALL_INDEXES = 2;
    static final int DECCEL_TIME_INDEXES = 6;
    static final int BULLET_POWER_INDEXES = 5;

    static Rectangle2D fieldRectangle;
    static Point2D robotDestination;
    static Point2D robotLocation = new Point2D.Double();
    static Point2D enemyLocation = new Point2D.Double();
    static double enemyAbsoluteBearing;
    static double enemyDistance;
    static int distanceIndex;
    static double enemyVelocity;
    double enemyEnergy;
    static int enemyTimeSinceDeccel;
    static double enemyBearingDirection = 0.75;
    static int[][][][][][] aimFactors = new int[DISTANCE_INDEXES][VELOCITY_INDEXES][LAST_VELOCITY_INDEXES][DECCEL_TIME_INDEXES][WALL_INDEXES][FACTORS];

    static double enemyFirePower = 2.5;
    static double robotBearingDirection = 1;
    static double robotVelocity;
    static double lastRobotVelocity;
    static int[][][][][] moveFactors = new int[DISTANCE_INDEXES][BULLET_POWER_INDEXES][VELOCITY_INDEXES][VELOCITY_INDEXES][FACTORS];
    static int[][][][][] hitFactors = new int[DISTANCE_INDEXES][BULLET_POWER_INDEXES][VELOCITY_INDEXES][VELOCITY_INDEXES][FACTORS];
    static EnemyWave passingWave;
    static EnemyWave closestWave;
    static double visitsForward;
    static double visitsReverse;
    static double orbitDirection = 1;
    static final double WALL_BOUNCE_LIMIT = 75;
    static int enemyHits;

    public void run() {
	setAdjustRadarForGunTurn(true);
	setAdjustGunForRobotTurn(true);

	fieldRectangle = new Rectangle2D.Double(WALL_MARGIN, WALL_MARGIN,
	    getBattleFieldWidth() - WALL_MARGIN * 2, getBattleFieldHeight() - WALL_MARGIN * 2);
	passingWave = null;
	closestWave = null;

	do {
	    turnRadarRightRadians(Double.POSITIVE_INFINITY); 
	} while (true);
    }

    public void onScannedRobot(ScannedRobotEvent e) {
	Wave wave = new Wave();
	wave.robot = this;
	EnemyWave ew = new EnemyWave();
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

	robotBearingDirection = sign(getVelocity() * Math.sin(getHeadingRadians() - ew.startBearing));
	ew.bearingDirection = (robotBearingDirection * Math.asin(MAX_VELOCITY / ew.bulletVelocity)) / (double)MIDDLE_FACTOR;

	distanceIndex = (int)(enemyDistance / (MAX_DISTANCE / DISTANCE_INDEXES));

	ew.visits = moveFactors[distanceIndex]
	    [(int)(enemyFirePower / 1.51)]
	    [(int)(lastRobotVelocity / 2)]
	    [(int)(robotVelocity / 2)]
	    ;
	/*
	ew.hits = hitFactors[distanceIndex]
	    [(int)(enemyFirePower / 1.51)]
	    [(int)(lastRobotVelocity / 2)]
	    [(int)(robotVelocity / 2)]
	    ;
	    */

	ew.targetLocation = robotLocation;

	lastRobotVelocity = robotVelocity;
	robotVelocity = Math.abs(getVelocity());

	ew.advance(2);
	addCustomEvent(ew);

	robotLocation.setLocation(new Point2D.Double(getX(), getY()));
	enemyAbsoluteBearing = getHeadingRadians() + e.getBearingRadians();
	enemyLocation.setLocation(project(wave.gunLocation = new Point2D.Double(getX(), getY()), enemyAbsoluteBearing, enemyDistance));
	wave.targetLocation = enemyLocation;
	enemyDistance = e.getDistance();

	// <gun>
	int lastVelocityIndex = (int)Math.abs(enemyVelocity) / 2;
	int velocityIndex = (int)Math.abs((enemyVelocity = e.getVelocity()) / 2);
	if (velocityIndex < lastVelocityIndex) {
	    enemyTimeSinceDeccel = 0;
	}

	//double bulletPower = MAX_BULLET_POWER; // TargetingChallenge
	double bulletPower = Math.min(enemyEnergy / 4, distanceIndex > 2 ? BULLET_POWER : MAX_BULLET_POWER);
	wave.bulletVelocity = bulletVelocity(bulletPower);

	if (enemyVelocity != 0) {
	    enemyBearingDirection = 0.75 * sign(enemyVelocity * Math.sin(e.getHeadingRadians() - enemyAbsoluteBearing));
	}
	wave.bearingDirection = enemyBearingDirection / (double)MIDDLE_FACTOR;

	wave.visits = aimFactors[distanceIndex][velocityIndex][lastVelocityIndex][Math.min(5, enemyTimeSinceDeccel++ / 13)]
	    [fieldRectangle.contains(project(wave.gunLocation, enemyAbsoluteBearing + wave.bearingDirection * 13, enemyDistance)) ? 1 : 0];

	wave.startBearing = enemyAbsoluteBearing;

	setTurnGunRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getGunHeadingRadians() +
		    wave.bearingDirection * (wave.mostVisited() - MIDDLE_FACTOR)));

	if (getEnergy() >= BULLET_POWER) {
	    setFire(bulletPower);
	    addCustomEvent(wave);
	}
	// </gun>

	if (closestWave != null) {
	    updateDirectionStats(closestWave);
	}
	else {
	    updateDirectionStats(ew);
	}
	goTo(robotDestination);
	setMaxVelocity(Math.abs(getTurnRemaining()) > 30 ? 0 : MAX_VELOCITY);

	setTurnRadarRightRadians(Utils.normalRelativeAngle(enemyAbsoluteBearing - getRadarHeadingRadians()) * 2);

	visitsForward = visitsReverse = 0;

//	if (ew.surfable) { // GL
//	    ew.grapher = new WaveGrapher(ew); // GL
//	} // GL
    }

    public void onHitByBullet(HitByBulletEvent e) {
	EnemyWave wave = passingWave;
	if (wave != null) {
	    wave.registerVisits(++enemyHits * 10);
	}
    }

    static Point2D wallSmoothedDestination(double deltaBearing, double distanceFactor) {
	Point2D destination = null;
	Point2D satelliteLocation = robotLocation;
	Point2D orbitLocation = enemyLocation;
	if (enemyDistance > 300 && closestWave != null) {
	    orbitLocation = closestWave.gunLocation;
	}
	double direction = -1;
	double tries;
	do {
	    direction = -direction;
	    tries = 0;
	    while (!fieldRectangle.contains(destination =
			project(orbitLocation,
			    absoluteBearing(orbitLocation, satelliteLocation) + deltaBearing * direction,
			    robotLocation.distance(orbitLocation) * (distanceFactor - tries / 100))) && tries < MAX_WALL_SMOOTH_TRIES) {
		tries++;
	    }
	} while (tries > WALL_BOUNCE_LIMIT && direction > 0);
	return destination;
    }

    void updateDirectionStats(EnemyWave wave) {
	double bearing = enemyAbsoluteBearing + Math.PI;
	double bearingDirection = sign(getVelocity() * Math.sin(getHeadingRadians() - bearing));
	double maxTravelDistance = MAX_VELOCITY * wave.distanceToTarget() / wave.bulletVelocity;
	Point2D reverseDestination = wallSmoothedDestination(-Math.atan2(maxTravelDistance * bearingDirection, enemyDistance), 1.1);
	reverseDestination = predictLocation(reverseDestination, wave, getVelocity(), getHeadingRadians());
	visitsReverse += wave.smoothedVisits(reverseDestination);
//	wave.grapher.drawReverseDestination(revereseDestination, wave.smoothedVisits(reverseDestination)); // GL

	Point2D forwardDestination = wallSmoothedDestination(Math.atan2(maxTravelDistance * bearingDirection, enemyDistance), 1.1);
	forwardDestination = predictLocation(forwardDestination, wave, getVelocity(), getHeadingRadians());
	visitsForward += wave.smoothedVisits(forwardDestination);
//	wave.grapher.drawForwardDestination(forwardDestination, wave.smoothedVisits(forwardDestination)); // GL

	if (visitsForward + visitsReverse > 0 && visitsReverse < visitsForward) {
	    orbitDirection = -bearingDirection;
	    robotDestination = reverseDestination;
	}
	else {
	    orbitDirection = bearingDirection;
	    robotDestination = forwardDestination;
	}
    }

    // rozu's predictor function, with Jim's and my enhancements
    private Point2D predictLocation(Point2D moveTo, EnemyWave wave, double velocity, double heading) {
	Point2D guessLoc = new Point2D.Double(getX(), getY());
	int time = 0;
	do {
	    double angle = Utils.normalRelativeAngle(absoluteBearing(guessLoc, moveTo) - heading);
	    double turnAngle = Math.atan(Math.tan(angle));

	    int moveDir = angle == turnAngle ? 1 : -1;
	    double maxTurning = Math.PI / 720d * (40d - 3d * Math.abs(velocity));
	    velocity += (velocity * moveDir < 0 ? 2 * moveDir : moveDir);
	    velocity = minMax(velocity, -8, 8);

	    turnAngle = minMax(turnAngle, -maxTurning, maxTurning);
	    heading = Utils.normalRelativeAngle(heading + turnAngle);

	    guessLoc.setLocation(guessLoc.getX() + (Math.sin(heading) * velocity), guessLoc.getY() + (Math.cos(heading) * velocity));
	    time++;
	} while (wave.distance(guessLoc, time) > 18);
	return guessLoc;
    }

    void goTo(Point2D destination) {
        double angle = Utils.normalRelativeAngle(absoluteBearing(robotLocation, destination) - getHeadingRadians());
	double turnAngle = Math.atan(Math.tan(angle));
        setTurnRightRadians(turnAngle);
        setAhead(robotLocation.distance(destination) * (angle == turnAngle ? 1 : -1));
    }

    static double bulletVelocity(double bulletPower) {
	return 20 - 3 * bulletPower;
    }

    static double maxEscapeAngle(double bulletVelocity) {
	return Math.asin(MAX_VELOCITY / bulletVelocity);
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

class Wave extends Condition {
    PPP robot;
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
	return (int)PPP.minMax(
	    Math.round(((Utils.normalRelativeAngle(gunBearing(target) - startBearing)) / bearingDirection) + PPP.MIDDLE_FACTOR), 0 , PPP.FACTORS - 1);
    }

    void registerVisits(int count) {
	try {
	    visits[visitingIndex(targetLocation)] += count;
	}
	catch (ArrayIndexOutOfBoundsException e) {
	}
    }

    double gunBearing(Point2D target) {
	return PPP.absoluteBearing(gunLocation, target);
    }

    double distanceToTarget() {
	return distance(targetLocation, 0);
    }

    double distance(Point2D location, int timeOffset) {
	return gunLocation.distance(location) - distanceFromGun - (double)timeOffset * bulletVelocity;
    }

    int mostVisited() {
	int mostVisited = PPP.MIDDLE_FACTOR, i = PPP.FACTORS - 1;
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
	index = (int)PPP.minMax(index, 0, PPP.FACTORS - 1);
	double smoothed = 0;
	int i = 0;
	do {
	    smoothed += (double)visits[i] / Math.sqrt((double)(Math.abs(index - i) + 1.0));
	    i++;
	} while (i < PPP.FACTORS);
	return smoothed / Math.sqrt(distanceToTarget() / bulletVelocity);
    }
}

class EnemyWave extends Wave {
//    WaveGrapher grapher; // GL
    //int[] hits;
    boolean surfable;

    public boolean test() {
	advance(1);
	if (passing(-18)) {
	    registerVisits(1);
	    surfable = false;
	    PPP.passingWave = this;
	    if (this == PPP.closestWave) {
		PPP.closestWave = null;
	    }
	}
	if (passing(18)) {
//	    if (grapher != null) { // GL
//		grapher.remove(); // GL
//	    } // GL
	    robot.removeCustomEvent(this);
	}
	if (surfable) {
	    if (PPP.closestWave == null || this.distanceToTarget() < PPP.closestWave.distanceToTarget()) {
		PPP.closestWave = this;
	    }
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
    Wave wave;
    PointGL[] dots;
    PointGL reverseDestination = new PointGL();
    PointGL forwardDestination = new PointGL();
    LabelGL reverseLabel = new LabelGL("");
    LabelGL forwardLabel = new LabelGL("");

    StatGrapher statGrapher;

    
    WaveGrapher(Wave wave) {
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
	statGrapher.setFrame(0, 0, PPP.BATTLE_FIELD_WIDTH, PPP.BATTLE_FIELD_HEIGHT);
    }

    void drawWave() {
	float totalVisits = 0;
	for (int i = 0; i < wave.visits.length; i++) {
	    totalVisits += wave.smoothedVisits(i);
	}
	for (int i = 0; i < dots.length; i++) {
	    Point2D dot = PPP.project(wave.gunLocation,
					  wave.startBearing + wave.bearingDirection * (i - (dots.length - 1) / 2),
					  wave.distanceFromGun);
	    dots[i].setPosition(dot.getX(), dot.getY());
	    float size = 100 * (float)wave.smoothedVisits(i) / totalVisits;
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
