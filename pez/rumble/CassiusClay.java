package pez.rumble;

import java.awt.Color;

import pez.rumble.pgun.Bee;
import pez.rumble.pmove.Butterfly;

//CassiusClay - by PEZ - Float like a butterfly. Sting like a bee.
//http://robowiki.net/?CassiusClay

//This code is released under the RoboWiki Public Code Licence (RWPCL), datailed on:
//http://robowiki.net/?RWPCL
//(Basically it means you must keep the code public if you base any bot on it.)

//$Id: CassiusClay.java,v 1.9 2007-02-27 05:49:05 peters Exp $

public class CassiusClay extends RumbleBot {
	public void run() {
		CassiusClayT.dummy(); // Tricking the Robocode packager to package the T (targeting-only) version
		CassiusClayM.dummy(); // Tricking the Robocode packager to package the M (movement-only) version
		CassiusClayGL.dummy(); // Tricking the Robocode packager to package the RobocodeGL version
		floater = new Butterfly(this);
		stinger = new Bee(this, robotPredictor);
		setColors(new Color(60, 30, 10), Color.green, Color.black);
		setBulletColor(Color.gray);
		setScanColor(Color.gray);
		super.run();
	}
}
