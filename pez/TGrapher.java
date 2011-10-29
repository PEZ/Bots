package pez;
import java.awt.Button;
import java.awt.Canvas;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.zip.ZipInputStream;

// This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
// http://robowiki.dyndns.org/?RWPCL
//
// RoboGrapher by PEZ
// Based on Kawigi's FloodGrapher
// http://robowiki.dyndns.org/?RoboGrapher
// $Id: TGrapher.java,v 1.1 2004/01/31 15:56:32 peter Exp $

public class TGrapher extends Canvas implements ActionListener, ItemListener, WindowListener {
	private static int[] segmentationsLengths = { 5, 3, 3, 4, 4, 5, 2 };
	private static final int SEGMENTATIONS = segmentationsLengths.length;
	private static String[][] segmentationsLabels = {
		{ "Distance", "a", "b", "c", "d", "e" },
		{ "Velocity", "a", "b", "c" },
		{ "Acceleration", "Decelerating", "Constant", "Accelerating" },
		{ "AccelTimer", "a", "b", "c", "d" },
		{ "Wall", "a", "b", "c", "d" },
		{ "Bullet power", "a", "b", "c", "d", "e" },
		{ "Wave type", "Real", "Virtual" }
	};
	private double[][][][][][][][] stats;
	private int[] graph;
	private int buckets;
	private static Choice fileChooser;
	private static Choice[] segmentationsChoosers = new Choice[SEGMENTATIONS];
	private static File folder;
	private static Button refreshFileListButton;
	int graphWidth = 425;
	int graphHeight = 425;

	public static void main(String args[]) {
		TGrapher grapher = new TGrapher();
		java.awt.Frame frame = new java.awt.Frame("TGrapher");
		frame.setSize(715, 540);
		folder = new File("TGB.data/");
		fileChooser = new Choice();
		refreshFileList();
		fileChooser.addItemListener(grapher);

		for (int i = 0; i < SEGMENTATIONS; i++) {
			segmentationsChoosers[i] = new Choice();
			segmentationsChoosers[i].addItem("All");
			for (int j = 1, n = segmentationsLabels[i].length; j < n; j++) {
				segmentationsChoosers[i].addItem(segmentationsLabels[i][j]);
			}
			segmentationsChoosers[i].addItemListener(grapher);
		}

		Panel controls = new Panel(new GridLayout(20, 1));
		refreshFileListButton = new Button("Refresh file list");
		refreshFileListButton.addActionListener(grapher);
		controls.add(refreshFileListButton);
		controls.add(new Label("File:"));
		controls.add(fileChooser);
		for (int i = 0; i < SEGMENTATIONS; i++) {
			controls.add(new Label(segmentationsLabels[i][0]));
			controls.add(segmentationsChoosers[i]);
		}

		segmentationsChoosers[SEGMENTATIONS - 1].select(1);

		frame.setLayout(null);
		frame.add(grapher);
		grapher.setBounds(5, 20, 500, 500);
		frame.add(controls);
		controls.setBounds(505, 20, 200, 500);
		frame.addWindowListener(grapher);
		frame.show();
	}

	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == fileChooser) {
			loadFile();
		}
		resetGraph();
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == refreshFileListButton) {
			refreshFileList();
		}
	}

	static void refreshFileList() {
		String[] filenames = folder.list();
		fileChooser.removeAll();
		fileChooser.addItem("");
		if (filenames != null)
			for (int i=0; i<filenames.length; i++)
				fileChooser.addItem(filenames[i]);
	}

	public void loadFile() {
		try {
			ZipInputStream zipin = new ZipInputStream(new
					FileInputStream(new File(folder, fileChooser.getSelectedItem())));
			zipin.getNextEntry();
			ObjectInputStream in = new ObjectInputStream(zipin);
			stats = (double[][][][][][][][])in.readObject();
			buckets = stats[0][0][0][0][0][0][0].length;
			in.close();
		}
		catch (Exception ex) {
			System.out.println("problem: " + ex);
			ex.printStackTrace();
		}
	}

	public void resetGraph() {
		if (stats == null)
			return;
		graph = new int[buckets];

		int[] min = new int[SEGMENTATIONS];
		int[] max = new int[SEGMENTATIONS];
		int[] s = new int[SEGMENTATIONS];
		for (int i = 0; i < SEGMENTATIONS; i++) {
			if (segmentationsChoosers[i].getSelectedIndex() == 0) {
				min[i] = 0;
				max[i] = segmentationsLengths[i] - 1;
			}
			else {
				min[i] = max[i] = segmentationsChoosers[i].getSelectedIndex() - 1;
			}
		}
		for (s[0] = min[0]; s[0] <= max[0]; s[0]++) {
			for (s[1] = min[1]; s[1] <= max[1]; s[1]++) {
				for (s[2] = min[2]; s[2] <= max[2]; s[2]++) {
					for (s[3] = min[3]; s[3] <= max[3]; s[3]++) {
						for (s[4] = min[4]; s[4] <= max[4]; s[4]++) {
							for (s[5] = min[5]; s[5] <= max[5]; s[5]++) {
								for (s[6] = min[6]; s[6] <= max[6]; s[6]++) {
									double samplesize=0, maxSamples=0;
									for (int bucket = 0; bucket < buckets; bucket++) {
										graph[bucket] += stats[s[0]][s[1]][s[2]][s[3]][s[4]][s[5]][s[6]][bucket];
										samplesize += stats[s[0]][s[1]][s[2]][s[3]][s[4]][s[5]][s[6]][bucket];
										maxSamples = Math.max(maxSamples, stats[s[0]][s[1]][s[2]][s[3]][s[4]][s[5]][s[6]][bucket]);
									}
								}
							}
						}
					}
				}
			}
		}
		repaint();
	}

	public void paint(Graphics g) {
		g.setColor(Color.black);
		g.translate(25, 25);
		g.drawRect(0, 0, graphWidth, graphHeight);
		g.setColor(Color.gray);
		g.drawLine(graphWidth / 4, 0, graphWidth / 4, graphHeight);
		g.drawLine(graphWidth / 2, 0, graphWidth / 2, graphHeight);
		g.drawLine(3 * graphWidth / 4, 0, 3 * graphWidth / 4, graphHeight);

		if (graph == null)
			return;

		int maxgraph = 0;
		int samples = 0;
		for (int i = 0; i < buckets; i++) {
			samples += graph[i];
			if (graph[i] > maxgraph)
				maxgraph = graph[i];
		}

		g.setColor(Color.blue);
		if (maxgraph > 0)
			for (int i = 0; i < buckets - 1; i++)
				g.drawLine((i      * graphWidth) / (buckets - 1), graphHeight - graphHeight * graph[i]     / maxgraph,
						((i + 1) * graphWidth) / (buckets - 1), graphHeight - graphHeight * graph[i + 1] / maxgraph);

		g.setColor(Color.black);
		g.drawString("samples: " + samples + ", highest: " + maxgraph + " (" + 10000 * maxgraph / samples / 100D + "%)", 0, -5);
	}

	public void windowClosing(WindowEvent e) {
		e.getWindow().dispose();
	}

	public void windowActivated(WindowEvent e) {}
	public void windowClosed(WindowEvent e) {}
	public void windowDeactivated(WindowEvent e) {}
	public void windowDeiconified(WindowEvent e) {}
	public void windowIconified(WindowEvent e) {}
	public void windowOpened(WindowEvent e) {}
}
