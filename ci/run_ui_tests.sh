#!/bin/bash
# fail if any commands fails
set -e

./ci/start_emulator.sh

./ci/wait_for_emulator.sh

# Buildkite uses a clean state for each step (for concurrency)
./ci/prepare_env_buildkite.sh

echo "Execute UI tests"
./gradlew clean assembleDebug connectedDebugAndroidTest --stacktrace
