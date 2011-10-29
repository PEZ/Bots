# Makefile for Marshmallow the robot, by PEZ
# $Id: Makefile,v 1.3 2004/02/02 16:13:00 peter Exp $

ROBOCODE_DIR = ../../..
#CLASSPATH = /System/Library/Frameworks/JavaVM.framework/Versions/1.4.1/Classes/classes.jar:${ROBOCODE_DIR}/robocode.jar:${ROBOCODE_DIR}/robots

all: macbot

macbot:
	/usr/bin/jikes -g -deprecation -Xstdout -classpath ${CLASSPATH} *.java

winbot:
	${ROBOCODE_DIR}/jikes-1.16/bin/jikes -g -deprecation -Xstdout -classpath "C:\Program\Java\j2re1.4.1_05\lib\rt.jar;${ROBOCODE_DIR}/robocode.jar;${ROBOCODE_DIR}/robots" *.java

winbot2:
	/cygdrive/c/Program/Java/j2sdk_nb/j2sdk1.4.2/bin/javac -classpath "C:\Program\Java\j2re1.4.1_05\lib\rt.jar;${ROBOCODE_DIR}/robocode.jar;${ROBOCODE_DIR}/robots" *.java
	
linuxbot:
	${ROBOCODE_DIR}/jikes-1.16/bin/jikes -g -deprecation -Xstdout +T4 -classpath ${JAVA_HOME}/jre/lib/rt.jar:${ROBOCODE_DIR}/robocode.jar:${ROBOCODE_DIR}/robots *.java
	
cleanall: clean cleantildes

clean:
	rm -f *.class

cleantildes:
	rm -f *~
