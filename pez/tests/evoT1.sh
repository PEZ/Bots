#!/bin/bash
LIMIT=100
for ((n=1; n <= LIMIT; n++)) 
do
    bot=Bot${n}
    ./Evolve1.awk -v bot=${bot} -v r=$RANDOM < ../micro/Aristocles.java > "${bot}.java"
done
