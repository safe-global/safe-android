#!/bin/bash
# fail if any commands fails
set -e

# Setup fake google service json
cp ci/google-services.json.sample app/google-services.json

./gradlew clean assembleDebug testDebugUnitTest --stacktrace
