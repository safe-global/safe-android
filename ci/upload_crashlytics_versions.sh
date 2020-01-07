#!/bin/bash
# fail if any commands fails
set -e

# Buildkite uses a clean state for each step (for concurrency)
./ci/prepare_env_buildkite.sh


echo "apiSecret=$FABRIC_API_SECRET" > app/fabric.properties
echo "apiKey=$FABRIC_API_KEY" >> app/fabric.properties

echo "INFURA_API_KEY=$INFURA_API_KEY" > project_keys

./gradlew assembleInternal assembleRinkeby assembleRelease
./gradlew crashlyticsUploadDistributionInternal crashlyticsUploadDistributionRinkeby crashlyticsUploadDistributionRelease
