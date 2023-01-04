#!/bin/bash
# kill emulators that are potentially still running
echo "Stop running emulators"
adb -e emu kill || true
# fail if any commands fails
set -e

cd $ANDROID_HOME

echo "Start emulator"
emulator/emulator @Safe33 -wipe-data -no-window -noaudio -no-snapshot-load -verbose &
