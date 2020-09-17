#!/bin/bash
# fail if any commands fails
set -e

export APP_VERSION_CODE=$((BUILDKITE_BUILD_NUMBER))

# use version name from gradle for release builds - needs to be updated manually
if [[ $BUILDKITE_BRANCH  != "release" ]]; then
  # get newest tags from origin
  git fetch
  # strip the first char as that should always be "v" (as tags should be in the format "vX.X.X")
  description="$(git describe --tags --always)"
  export APP_VERSION_NAME=${description:1}
else
  version="$(./gradlew -q pV | tail -1)"
  export APP_VERSION_NAME="${version}-${APP_VERSION_CODE}"
fi

export APP_RELEASE_NOTES=$BUILDKITE_MESSAGE
