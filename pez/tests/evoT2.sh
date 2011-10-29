#!/bin/bash
LIMIT=9
for p in 100 92 74 15 98 78 96 86 26 40 0
do
    cp ~/evolve1/Bot${p}.java .
    for ((n=1; n <= LIMIT; n++)) 
    do
	bot=Bot${p}_${n}
	./Evolve2.awk -v bot=${bot} -v r=$RANDOM < ~/evolve1/Bot${p}.java > "${bot}.java"
    done
done
