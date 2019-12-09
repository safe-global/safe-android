#!/bin/bash
# fail if any commands fails
set -e

# Buildkite uses a clean state for each step (for concurrency)
./ci/prepare_env_buildkite.sh

./gradlew clean assembleDebug testDebugUnitTest --stacktrace
