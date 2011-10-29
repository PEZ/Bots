#!/usr/local/bin/perl
# $Id: vw.pl,v 1.1.1.1 2003/05/05 20:55:09 peter Exp $
# vw.pl - variable watch (mainly for robocoders) - by PEZ  - pez at pezius.com
# Use like: vw.pl <file>
#  where <file> is a, often growing, file with lines of the format:
#  name=value[=format]
#  The optional format is of standard printf() style.
# Modify at your fancy, but please e-mail me any modifications that could be of
# general use.
use strict;
$|=1;
use Curses;
use File::Tail;
my $win = new Curses;
my $file = File::Tail->new(name=>"$ARGV[0]", interval=>0.01, maxinterval=>0.02, tail=>-1);
my $nameCount = 1;
my (%seen, %watch);
my ($watchLabel, $watchValue);
$win->addstr(0, 0, sprintf('%3s %20s - %s', '###', 'VARIABLE NAME', 'VALUE'));
while (defined(my $line=$file->read())) {
    my ($name, $value, $format) = split('=', $line);
    unless (defined($format)) {
        $format = '%s                         ' if $value =~ /\D/;
        $format = '%10.2f                     ' if $value =~ /^-?(\d+(\.\d*)?|\.\d+)/;
        $format = '%7d                        ' if $value =~ /^-?\d+$/;
    }
    $seen{$name} = $nameCount++ unless defined($seen{$name});
    $watchLabel = sprintf('%3d %20s', $seen{$name}, $name);
    $watchValue = sprintf($format, $value);
    $watch{$watchLabel} = $watchValue;
    my $c = 1;
    foreach my $label (sort(keys(%watch))) {
        $win->addstr($c++, 0, "$label - $watch{$label}");
    }
    $win->refresh();
}
