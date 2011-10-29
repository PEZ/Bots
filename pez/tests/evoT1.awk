#!/usr/bin/gawk -f
BEGIN {
    FS="[ ,;]";
    srand(r);
}
/^package/ {
    print "package pez.tests;"
    next
}
/class Aristocles/ {
    sub(/Aristocles/, bot)
    print
    next
}
/REVERSE_TUNER =/ {
    print "    static final double REVERSE_TUNER = " (rand() * 7 + 17) ";"
    next
}
/WALL_BOUNCE_TUNER =/ {
    print "    static final double WALL_BOUNCE_TUNER = " (rand() * 0.8 + 0.4) ";"
    next
}
{
    print
}
