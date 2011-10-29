# Makefile for Marshmallow the robot, by PEZ
# $Id: Makefile,v 1.2 2003/04/11 22:58:17 peter Exp $

ROBOCODE_DIR = "../../.."
CLASSPATH = "/System/Library/Frameworks/JavaVM.framework/Versions/1.4.1/Classes/classes.jar:${ROBOCODE_DIR}/robocode.jar:${ROBOCODE_DIR}/robots:nrlibj"

all: nrlibj macbot

macbot:
	/usr/bin/jikes -deprecation -Xstdout +T4 -classpath ${CLASSPATH} *.java

linuxbot:
	${ROBOCODE_DIR}/jikes-1.16/bin/jikes -deprecation -Xstdout +T4 -classpath ${JAVA_HOME}/jre/lib/rt.jar:${ROBOCODE_DIR}/robocode.jar:${ROBOCODE_DIR}/robots *.java
	
nrlibj:
	/usr/bin/jikes -deprecation -Xstdout +T4 -classpath ${CLASSPATH} nrlibj/*.java

cleanall: clean cleantildes

clean:
	rm -f *.class

cleantildes:
	rm -f *~
