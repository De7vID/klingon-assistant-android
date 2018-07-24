#!/bin/bash

cd data
./strip_non_en_de.sh
./renumber.py
./generate_db.sh --noninteractive
cd ..
