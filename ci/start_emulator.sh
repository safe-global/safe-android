#!/bin/bash
# kill emulators that are potentially still running
echo "Stop running emulators"
adb -e emu kill || true
# fail if any commands fails
set -e

echo "Setup env variables"
EMULATOR=`which emulator`
echo "Emulator: $EMULATOR"
EMULATOR_PATH=`dirname $EMULATOR`
echo "Emulator Path: $EMULATOR_PATH"

cd $EMULATOR_PATH

echo "Start emulator"
emulator @`emulator -list-avds | head -1` -wipe-data -no-window -noaudio -no-snapshot-load -verbose &
