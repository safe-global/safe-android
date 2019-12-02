#!/bin/bash
# fail if any commands fails
set -e

if [[ $TRAVIS_BRANCH == 'master' ]]
then
    ./gradlew assembleInternal assembleRinkeby assembleRelease
    ./gradlew crashlyticsUploadDistributionInternal crashlyticsUploadDistributionRinkeby crashlyticsUploadDistributionRelease
fi
