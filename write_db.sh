#!/bin/bash

cd data
git stash push # --quiet
git reset --hard
./generate_db.sh --noninteractive
cp qawHaq.db ../app/src/main/assets/
./stats.sh
cd ..
