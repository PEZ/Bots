#!/bin/bash
LIMIT=$2
for ((n=1; n <= LIMIT; n++)) 
do
    bot=Bot${n}
    mkdir ${bot}
    ./Evolve1.awk -v bot=${bot} -v r=$RANDOM < $1 > "${bot}/${bot}.java"
done
