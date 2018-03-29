#!/bin/bash
# fail if any commands fails
set -e

# write your script here
export APP_VERSION_CODE=$TRAVIS_BUILD_NUMBER
# strip the first char as that should always be "v" (as tags should be in the format "vX.X.X")
description="$(git describe --tags --always)"
export APP_VERSION_NAME=${description:1}
export APP_RELEASE_NOTES=$TRAVIS_COMMIT_MESSAGE
