#!/bin/bash
# fail if any commands fails
set -e

export APP_VERSION_CODE=$((BUILDKITE_BUILD_NUMBER))

# Buildkite branch equals to tag name if build was triggered by tag
if [[ $BUILDKITE_BRANCH  =~ ^v[0-9]+.* ]]; then
    export APP_VERSION_NAME=${BUILDKITE_BRANCH:1}
else
  export RC_INDICATOR=""
  version="$(./gradlew -q pV | tail -1)"
  if [[ $BUILDKITE_BRANCH  == "release" ]]; then
      git fetch
      description="$(git describe --tags --always)"
      tag=${description:1}
      tag_version="$(echo $tag | cut -d'-' -f 1)"
      # if there is a tag with same version => release was already built 
      if [[ $tag_version  != $version ]]; then
          RC_INDICATOR="rc"
      fi
  fi
  export APP_VERSION_NAME="${version}-${APP_VERSION_CODE}${RC_INDICATOR}"
fi

export APP_RELEASE_NOTES=$BUILDKITE_MESSAGE
