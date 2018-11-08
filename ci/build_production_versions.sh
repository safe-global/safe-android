#!/bin/bash
# fail if any commands fails
set -e

./gradlew clean --stacktrace
./gradlew assembleRinkeby --stacktrace
./gradlew assembleRelease --stacktrace
