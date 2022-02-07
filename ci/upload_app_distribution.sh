#!/bin/bash
# fail if any commands fails
set -e

# Buildkite uses a clean state for each step (for concurrency)
source ./ci/prepare_env_buildkite.sh

export FIREBASE_GROUP="$1"

echo "INFURA_API_KEY=$INFURA_API_KEY" > project_keys
echo "INTERCOM_API_KEY=$INTERCOM_API_KEY" > project_keys
echo "INTERCOM_APP_ID=$INTERCOM_APP_ID" > project_keys

# requires app distribution setup in firebase
./gradlew assemble${2^}
./gradlew appDistributionUpload${2^}
