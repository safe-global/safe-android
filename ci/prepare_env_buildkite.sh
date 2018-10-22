#!/bin/bash
# fail if any commands fails
set -e

# write your script here
export APP_VERSION_CODE=$BUILDKITE_BUILD_NUMBER
# strip the first char as that should always be "v" (as tags should be in the format "vX.X.X")
description="$(git describe --tags --always)"
export APP_VERSION_NAME=${description:1}
export APP_RELEASE_NOTES=$BUILDKITE_MESSAGE

# We currently don't need Fabric working for our buildkite flows
# echo "apiSecret=$FABRIC_API_SECRET" > app/fabric.properties
# echo "apiKey=$FABRIC_API_KEY" >> app/fabric.properties

# Setup fake google service json
cp ci/google-services.json.sample app/google-services.json
