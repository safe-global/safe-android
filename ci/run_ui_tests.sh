#!/bin/bash
# fail if any commands fails
set -e

./ci/start_emulator.sh

./ci/wait_for_emulator.sh

# Buildkite uses a clean state for each step (for concurrency)
./ci/prepare_env_buildkite.sh

# Clean state
./gradlew clean

# Install app
./gradlew installDebug --stacktrace

# Start app (this is required on some emulators to get the app in a valid state -> Signature failure)
adb shell am start -n pm.gnosis.heimdall.debug/pm.gnosis.heimdall.ui.splash.SplashActivity

echo "Execute UI tests"
./gradlew createDebugTestCoverage --stacktrace

bash <(curl -s https://codecov.io/bash) -f '*TestCoverage.xml'
