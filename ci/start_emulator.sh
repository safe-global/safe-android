#!/bin/bash
# kill emulators that are potentially still running
echo "Stop running emulators"
adb -e emu kill || true
# fail if any commands fails
set -e

echo "Setup env variables"
#export QT_DEBUG_PLUGINS=1
#EMULATOR=`which emulator`
#echo "Emulator: $EMULATOR"
#EMULATOR_PATH=`dirname $EMULATOR`
#echo "Emulator Path: $EMULATOR_PATH"


cd $ANDROID_HOME

#cd $EMULATOR_PATH

#emulator -version

echo "Start emulator"
#emulator @`emulator -list-avds | head -1` -wipe-data -no-window -noaudio -no-snapshot-load -verbose &

emulator/emulator @Safe33 -wipe-data -no-window -noaudio -no-snapshot-load -verbose &
