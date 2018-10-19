#!/bin/bash
# fail if any commands fails
set -e

./ci/start_emulator.sh

./ci/wait_for_emulator.sh

echo "Execute UI tests"
./gradlew clean assembleDebug connectedDebugAndroidTest --stacktrace
