package pez.rumble.utils;
import java.util.*;
import java.awt.geom.*;

// WaveHistory, for comparing Waves over time. By PEZ.
// http://robowiki.net/?PEZ
//
// This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
// http://robowiki.net/?RWPCL
// (Basically it means you must keep the code public if you use it in any way.)
//
// $Id: Wave.java,v 1.10 2004/09/27 23:00:14 peter Exp $

public class WaveHistory {
    int depth;
    List history;

    public WaveHistory(int depth) {
	this.depth = depth;
	history = new ArrayList(depth);
    }

    public void add(Wave wave) {
	history.add(0, wave);
	if (history.size() > depth) {
	    history.remove(depth);
	}
    }

    public Wave getHistoricWave(int historyIndex) {
	if (history.size() > 0) {
	    historyIndex = Math.min(history.size() - 1, historyIndex);
	    return (Wave)history.get(historyIndex);
	}
	return null;
    }

    public double GFDistance(Wave wave, int historyIndex) {
	Wave historicWave = getHistoricWave(historyIndex);
	if (historicWave != null) {
	    return Math.abs(wave.getGF() - historicWave.getGF());
	}
	return 0;
    }

    public double traveledDistance(Wave wave, int historyIndex) {
	Wave historicWave = getHistoricWave(historyIndex);
	if (historicWave != null) {
	    return historicWave.startTargetLocation.distance(wave.targetLocation);
	}
	return 0;
    }
}
