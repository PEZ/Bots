# Makefile for Marshmallow the robot, by PEZ
# $Id: Makefile,v 1.12 2004/03/10 11:16:26 peter Exp $

ROBOCODE_DIR = ../../..
#CLASSPATH = /System/Library/Frameworks/JavaVM.framework/Versions/1.4.2/Classes/classes.jar:${ROBOCODE_DIR}/robocode.jar:${ROBOCODE_DIR}/robots

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

gl:
	${ROBOCODE_DIR}/jikes-1.16/bin/jikes -g -deprecation -Xstdout -classpath "${ROBOCODE_DIR};C:\Program\Java\j2sdk_nb\j2sdk1.4.2\jre\lib\rt.jar;C:\Program\Java\j2sdk_nb\j2sdk1.4.2\jre\lib\ext\gl4java.jar;${ROBOCODE_DIR}/robocode;${ROBOCODE_DIR}/robots" *.java

gl2:
	javac -classpath "${CLASSPATH};${ROBOCODE_DIR};${ROBOCODE_DIR}/robocode;${ROBOCODE_DIR}/robots" *.java

pugilist: gl
	java -jar ${ROBOCODE_DIR}/codesize.jar Pugilist.class Wave.class EnemyWave.class | awk '/^[0-9]/ { print; size += $$2 } END { print "size: " size ", left = " 1499 - size }'

verti: gl
	java -jar ${ROBOCODE_DIR}/codesize.jar VertiLeach.class VWave.class VEnemyWave.class | awk '/^[0-9]/ { print; size += $$2 } END { print "size: " size ", left = " 1499 - size }'
