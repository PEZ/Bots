#!/usr/bin/gawk -f
BEGIN {
    FS="[ ;]+";
    srand(r);
}
/class Bot/ {
    sub(/Bot[0-9]+/, bot)
    print
    next
}
/REVERSE_TUNER =/ {
    print "    static final double REVERSE_TUNER = " ($7 - 1.25 + rand() * 2.5) ";"
    next
}
/WALL_BOUNCE_TUNER =/ {
    print "    static final double WALL_BOUNCE_TUNER = " ($7 - 0.4 + rand() * 0.8) ";"
    next
}
{
    print
}
