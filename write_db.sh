#!/bin/bash

cd data
./renumber.py
./generate_db.sh --noninteractive
cp qawHaq.db ../app/src/main/assets/
./unnumber.sh
./stats.sh
cd ..
