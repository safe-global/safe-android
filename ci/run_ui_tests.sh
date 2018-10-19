#!/bin/bash
# fail if any commands fails
set -e

ls

./ci/start_emulator.sh

./ci/wait_for_emulator.sh

./gradlew clean assembleDebug connectedDebugAndroidTest --stacktrace
