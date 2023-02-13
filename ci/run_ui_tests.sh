#!/bin/bash
# fail if any commands fails
set -e

# Setup fake google service json
cp ci/google-services.json.sample app/google-services.json

./ci/start_emulator.sh

./ci/wait_for_emulator.sh

# Disable animations on emulator
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0

# Buildkite uses a clean state for each step (for concurrency)
./ci/prepare_env_buildkite.sh

# Clean state
./gradlew clean

# Install app
./gradlew installDebug --stacktrace

# Start app (this is required on some emulators to get the app in a valid state -> Signature failure)
adb shell am start -n io.gnosis.safe.debug/io.gnosis.safe.ui.splash.SplashActivity

echo "Execute Coverage verification"
./gradlew jacocoTestCoverageVerification --stacktrace

#TODO: fix and enable test report
#echo "Execute UI tests"
#./gradlew jacocoTestReport --stacktrace
#
## https://docs.codecov.com/docs/about-the-codecov-bash-uploader
#echo Generating report form coverage result
#bash <(curl -s https://codecov.io/bash) -f '*TestReport.xml'
