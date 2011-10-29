# Makefile for Marshmallow the robot, by PEZ
# $Id: Makefile,v 1.12 2004/03/10 11:16:26 peter Exp $

J_DIR = C:\Program\Java\j2re1.4.1_05\lib
#J_DIR = C:\Program\Java\j2re1.4.2_03\lib
ROBOCODE_DIR = ../../..


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
	${ROBOCODE_DIR}/jikes-1.16/bin/jikes -g -deprecation -Xstdout -classpath "${ROBOCODE_DIR};${J_DIR}\rt.jar;${J_DIR}\ext\gl4java.jar;${ROBOCODE_DIR}/robocode;${ROBOCODE_DIR}/robots" *.java

gl2:
	javac -classpath "${CLASSPATH};${ROBOCODE_DIR};${ROBOCODE_DIR}/robocode;${ROBOCODE_DIR}/robots" *.java

pugilist: gl
	java -jar ${ROBOCODE_DIR}/codesize.jar Pugilist.class Wave.class EnemyWave.class | awk '/^[0-9]/ { print; size += $$2 } END { print "size: " size ", left = " 1499 - size }'

verti: gl
	java -jar ${ROBOCODE_DIR}/codesize.jar VertiLeach.class VWave.class VEnemyWave.class | awk '/^[0-9]/ { print; size += $$2 } END { print "size: " size ", left = " 1499 - size }'

chiro: gl
	java -jar ${ROBOCODE_DIR}/codesize.jar Chiro*.class | awk '/^[0-9]/ { print; size += $$2 } END { print "size: " size ", left = " 1499 - size }'
