#!/bin/bash

cd data
./strip_non_en_de.sh
./generate_db.sh --noninteractive
cp qawHaq.db ../app/src/main/assets/
./stats.sh
cd ..
