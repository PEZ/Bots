# Makefile for Marshmallow the robot, by PEZ
# $Id: Makefile,v 1.8 2004/09/22 21:16:25 peter Exp $

J_DIR = C:\Program\Java\j2re1.4.1_05\lib
#J_DIR = C:\Program\Java\j2re1.4.2_03\lib
ROBOCODE_DIR = ../../..

all: gl

macbot:
	/usr/bin/jikes -g -deprecation -Xstdout -classpath /System/Library/Frameworks/JavaVM.framework/Versions/1.4.2/Classes/classes.jar:${ROBOCODE_DIR}/robocode.jar:${ROBOCODE_DIR}/robots *.java pgun/*.java pmove/*.java

winbot:
	${ROBOCODE_DIR}/jikes-1.16/bin/jikes -g -deprecation -Xstdout -classpath "C:\Program\Java\j2re1.4.1_05\lib\rt.jar;${ROBOCODE_DIR}/robocode.jar;${ROBOCODE_DIR}/robots" *.java

winbot2:
	/cygdrive/c/Program/Java/j2sdk_nb/j2sdk1.4.2/bin/javac -classpath "C:\Program\Java\j2re1.4.1_05\lib\rt.jar;${ROBOCODE_DIR}/robocode.jar;${ROBOCODE_DIR}/robots" *.java
	
linuxbot:
	${ROBOCODE_DIR}/jikes-1.16/bin/jikes -g -deprecation -Xstdout +T4 -classpath ${JAVA_HOME}/jre/lib/rt.jar:${ROBOCODE_DIR}/robocode.jar:${ROBOCODE_DIR}/robots *.java
	
cleanall: clean cleantildes

clean:
	rm -f *.class pgun/*.class pmove/*.class

cleantildes:
	rm -f *~ pgun/*~ pmove/*~

gl:
	${ROBOCODE_DIR}/jikes-1.16/bin/jikes -g -deprecation -Xstdout -classpath "${ROBOCODE_DIR};${J_DIR}\rt.jar;${J_DIR}\ext\gl4java.jar;${ROBOCODE_DIR}/robocode;${ROBOCODE_DIR}/robots" *.java pgun/*.java pmove/*.java

gl2:
	javac -classpath "${CLASSPATH};${ROBOCODE_DIR};${ROBOCODE_DIR}/robocode;${ROBOCODE_DIR}/robots" *.java
