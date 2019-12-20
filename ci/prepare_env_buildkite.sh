#!/bin/bash
# fail if any commands fails
set -e

export APP_VERSION_CODE=$((BUILDKITE_BUILD_NUMBER / 2 + 2207))
# strip the first char as that should always be "v" (as tags should be in the format "vX.X.X")
description="$(git describe --tags --always)"
export APP_VERSION_NAME=${description:1}
export APP_RELEASE_NOTES=$BUILDKITE_MESSAGE




