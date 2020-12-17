#!/bin/bash

cd data
git stash push --quiet
git reset --hard
./strip_non_en_de.sh
./generate_db.sh --noninteractive
cp qawHaq.db ../app/src/main/assets/
./stats.sh
git reset --hard
cd ..
