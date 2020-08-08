#!/bin/bash

cd data
./generate_db.sh --noninteractive
cp qawHaq.db ../app/src/main/assets/
./stats.sh
cd ..
