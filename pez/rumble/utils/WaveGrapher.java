package pez.rumble.utils;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import pez.rumble.pmove.MovementWave;

public class WaveGrapher{
	static GRenderer renderer = new GRenderer();
	static int counter = 0;

	String id;
	MovementWave wave;
	GPoint[] dots;
	GPoint forwardDestination = new GPoint();
	GPoint reverseDestination = new GPoint();
	GPoint stopDestination = new GPoint();
	GLabel forwardLabel = new GLabel("");
	GLabel reverseLabel = new GLabel("");
	GLabel stopLabel = new GLabel("");

	public WaveGrapher(MovementWave wave) {
		this.id = "" + counter++;
		this.wave = wave;
		this.dots = new GPoint[MovementWave.FACTORS];
		for (int i = 0; i < dots.length; i++) {
			dots[i] = new GPoint();
			if (i == MovementWave.MIDDLE_FACTOR) {
				dots[i].addLabel(new GLabel(id));
			}
			renderer.addRenderElement(dots[i]);
		}
		forwardDestination.addLabel(forwardLabel);
		forwardDestination.setFillColor(Color.GREEN);
		forwardDestination.setSize(15);
		forwardDestination.setPosition(-100, -100);
		reverseDestination.addLabel(reverseLabel);
		reverseDestination.setFillColor(Color.RED);
		reverseDestination.setSize(15);
		reverseDestination.setPosition(-100, -100);
		stopDestination.addLabel(stopLabel);
		stopDestination.setFillColor(Color.YELLOW);
		stopDestination.setSize(15);
		stopDestination.setPosition(-100, -100);
		renderer.addRenderElement(forwardDestination);
		renderer.addRenderElement(reverseDestination);
		renderer.addRenderElement(stopDestination);
	}

	public static void onPaint(Graphics2D g) {
		renderer.render(g);
	}

	public void drawWave() {
		for (int i = 0; i < dots.length; i++) {
			Point2D dot = PUtils.project(wave.getGunLocation(),
					wave.getStartBearing() + wave.getOrbitDirection() * (i - MovementWave.MIDDLE_FACTOR),
					wave.distanceFromGun());
			dots[i].setPosition((float)dot.getX(), (float)dot.getY());
			dots[i].setFillColor(Color.BLUE);
			dots[i].setSize((float)wave.dangerUnWeighed(i) / 7.0f);
		}
	}

	void drawDestination(GPoint destination, GLabel label, Point2D coords, double value) {
		destination.setPosition((float)coords.getX(), (float)coords.getY());
		label.setString(id + " : " + (int)value);
	}

	public void drawForwardDestination(Point2D coords, double value) {
		drawDestination(forwardDestination, forwardLabel, coords, value);
	}

	public void drawReverseDestination(Point2D coords, double value) {
		drawDestination(reverseDestination, reverseLabel, coords, value);
	}

	public void drawStopDestination(Point2D coords, double value) {
		drawDestination(stopDestination, stopLabel, coords, value);
	}

	public void remove() {
		for (int i = 0; i < dots.length; i++) {
			renderer.remove(dots[i]);
		}
		renderer.remove(forwardDestination);
		renderer.remove(reverseDestination);
		renderer.remove(stopDestination);
	}
	
	static GRectangle forwardRect;
	static GRectangle stopRect;
	static GRectangle reverseRect;

	public static void drawDangerGraph(double dangerForward, double dangerStop, double dangerReverse) {
		forwardRect.setSize(15, (float)dangerForward);
		stopRect.setSize(15, (float)dangerStop);
		reverseRect.setSize(15, (float)dangerReverse);
	}

	public static boolean initDangerGraph() {
		forwardRect = new GRectangle(10, 0, 15, 0, Color.GREEN, 1);
		stopRect    = new GRectangle(25, 0, 15, 0, Color.YELLOW, 1);
		reverseRect = new GRectangle(40, 0, 15, 0, Color.RED, 1);
		//forwardRect.setFilled(true);
		//stopRect.setFilled(true);
		//reverseRect.setFilled(true);
		renderer.addRenderElement(forwardRect);
		renderer.addRenderElement(stopRect);
		renderer.addRenderElement(reverseRect);
		return true;
	}
}
// GL

interface IRenderElement {
	public void render(Graphics2D g);
}

class GRenderer {
	List<IRenderElement> elements = new ArrayList<IRenderElement>();
	
    void addRenderElement(IRenderElement element) {
    	elements.add(element);
    }

    void remove(IRenderElement element) {
    	elements.remove(element);
    }

	void render(Graphics2D g) {
		for (IRenderElement element : elements) {
			element.render(g);
		}
	}
}

abstract class GShape {
	protected float center_x;
	protected float center_y;
	protected Color fillColor = Color.WHITE;
	List<GLabel> labels = new ArrayList<GLabel>();

	GShape setPosition(float x, float y) {
		this.center_x = x;
		this.center_y = y;
		return this;
	}
	
	GShape setFillColor(Color color) {
		this.fillColor = color;
		return this;
	}

	GShape addLabel(GLabel label) {
		labels.add(label);
		return this;
	}
	
	void positionLabels(Graphics2D g) {
		for (int i = 0, n= labels.size(); i < n; i++) {
			GLabel label = labels.get(i);
			FontMetrics metrics = label.getFont() != null ? g.getFontMetrics(label.getFont()) : g.getFontMetrics();
			label.setPosition(center_x, (float)(center_y + i * metrics.getHeight() * 1.1));
		}
	}

	void renderLabels(Graphics2D g) {
		positionLabels(g);
		for (GLabel label : labels) {
			label.render(g);
		}
	}
}

class GPoint extends GShape implements IRenderElement {
	private Ellipse2D ellipse = new Ellipse2D.Double();
	private float size;
	
	GPoint setSize(float size) {
		this.size = size;
		return this;
	}

	@Override
	public void render(Graphics2D g) {
		g.setColor(fillColor);
		ellipse.setFrameFromCenter(new Point((int)center_x, (int)center_y), new Point((int)(center_x + size / 2f), (int)(center_y + size / 2f)));
		g.fill(ellipse);
		renderLabels(g);
	}
}

class GRectangle extends GShape implements IRenderElement {
	private Rectangle rectangle = new Rectangle();
	private float width;
	private float height;

	public GRectangle(float x, float y, float w, float h, Color color, float alpha) {
		this.width = w;
		this.height = h;
		this.setPosition(x - w / 2.0f, y - h / 2.0f);
		this.fillColor = color;
	}
	
	public GRectangle setSize(float w, float h) {
		this.width = w;
		this.height = h;
		return this;
	}

	@Override
	public void render(Graphics2D g) {
		g.setColor(fillColor);
		rectangle.setFrameFromCenter(new Point((int)center_x, (int)center_y), new Point((int)(center_x + width / 2f), (int)(center_y + height / 2f)));
		g.fill(rectangle);
		renderLabels(g);
	}
}

class GLabel extends GShape implements IRenderElement {
	private Font font;
	private String string = "";
	
	public GLabel(String s) {
		this.string = s;
	}

	GLabel setString(String s) {
		this.string = s;
		return this;
	}
	
	Font getFont() {
		return this.font;
	}

	GLabel setFont(Font font) {
		this.font = font;
		return this;
	}
	
	@Override
	public void render(Graphics2D g) {
		g.setColor(fillColor);
		if (font != null) {
			g.setFont(font);
		}
		FontMetrics metrics = g.getFontMetrics();
		float dy = (float) (metrics.getHeight() / 2.0);
		float dx = (float) (metrics.stringWidth(string) / 2.0);
		g.drawString(string, center_x - dx, center_y - dy);
	}
}
