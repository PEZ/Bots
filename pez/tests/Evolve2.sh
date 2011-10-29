#!/bin/bash
LIMIT=5
for p in 17_7 84_10 101_6
do
    cp ~/evolve/Bot${p}.java .
    for ((n=1; n <= LIMIT; n++)) 
    do
	bot=Bot${p}_${n}
	./Evolve2.awk -v bot=${bot} -v r=$RANDOM < Bot${p}.java > "${bot}.java"
    done
done
