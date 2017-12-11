#!/bin/bash
# fail if any commands fails
set -e

less app/fabric.properties

./gradlew clean assembleDebug createDebugTestCoverage  --stacktrace