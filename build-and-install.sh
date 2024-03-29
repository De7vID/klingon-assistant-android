#!/bin/bash

# Generate the database.
cd data
./generate_db.sh
cp qawHaq.db ../app/src/main/assets/
cd ..

# Install the debug build.
./gradlew installDebug
if [ $? -eq 0 ]
then
    # Pause before pushing.
    read -n1 -r -p "[Optional] Press any key to copy apk to device..."
    echo

    # Push apk to device.
    adb push ./app/build/outputs/apk/debug/app-debug.apk /sdcard/
else
    echo ERROR: Failed to installDebug.
fi
