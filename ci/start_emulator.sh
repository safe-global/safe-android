#!/bin/bash
# kill emulators that are potentially still running
echo "Stop running emulators"
adb -e emu kill || true
echo "Stop running emulators -----"
# fail if any commands fails
set -e

EMULATOR=`which emulator`
echo "Emulator: $EMULATOR"
EMULATOR_PATH=`dirname $EMULATOR`
echo "Emulator Path: $EMULATOR_PATH"

cd $EMULATOR_PATH

echo "Start emulator"
emulator @`emulator -list-avds | head -1` -no-window &
