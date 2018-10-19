#!/bin/bash
# kill emulators that are potentially still running
adb -e emu kill || true
# fail if any commands fails
set -e

EMULATOR=`which emulator`
EMULATOR_PATH=`dirname $EMULATOR`

cd $EMULATOR_PATH

emulator @`emulator -list-avds | head -1` -no-window &
