#!/usr/bin/gawk -f
BEGIN {
    FS="[ ,;]";
    srand(r);
}
/^package/ {
    print "package pez.tests." bot ";"
    next
}
/Pugilist/ {
    gsub(/Pugilist/, bot)
    print
    next
}
/WALL_BOUNCE_LIMIT =/ {
    print "    static final double WALL_BOUNCE_LIMIT = " (5 + rand() * 95) ";"
    next
}
/WALL_BOUNCE_INCREMENT =/ {
    print "    static final double WALL_BOUNCE_INCREMENT = " (rand() * 25) ";"
    next
}
{
    print
}
