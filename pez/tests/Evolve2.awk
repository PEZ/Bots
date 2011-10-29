#!/usr/bin/gawk -f
BEGIN {
    FS="[ ;]+";
    srand(r);
}
/class Bot/ {
    sub(/Bot[0-9_]+/, bot)
    print
    next
}
/TUNER1 =/ {
    print "    static final double TUNER1 = " ($7 - 2.0 + rand() * 4.0) ";"
    next
}
/TUNER2 =/ {
    print "    static final double TUNER2 = " ($7 - 0.5 + rand() * 1.0) ";"
    next
}
{
    print
}
