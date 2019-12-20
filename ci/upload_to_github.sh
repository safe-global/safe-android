#!/bin/bash
# fail if any commands fails
set -e

export APP_VERSION_CODE=$((BUILDKITE_BUILD_NUMBER / 2 + 2207))
# strip the first char as that should always be "v" (as tags should be in the format "vX.X.X")
description="$(git describe --tags --always)"
export APP_VERSION_NAME=${description:1}
export APP_RELEASE_NOTES=$BUILDKITE_MESSAGE

echo "apiSecret=$FABRIC_API_SECRET" > app/fabric.properties
echo "apiKey=$FABRIC_API_KEY" >> app/fabric.properties

echo "INFURA_API_KEY=$INFURA_API_KEY" > project_keys

./gradlew assembleRinkeby assembleRelease


# Define variables.$APP_VERSION_CODE$APP_VERSION_CODE
REPO="https://api.github.com/repos/gnosis/safe-android"
TAGS="$REPO/releases/tags/$BUILDKITE_BRANCH"
AUTH="Authorization: token $GITHUB_API_KEY"
WGET_ARGS="--content-disposition --auth-no-challenge --no-cookie"
CURL_ARGS="-LJO#"

# Read asset tags.
response=$(curl -sH "$AUTH" $TAGS)

# Get ID of the asset based on given filename.
eval $(echo "$response" | grep -m 1 "id.:" | grep -w id | tr : = | tr -cd '[[:alnum:]]=')
[ "$id" ] || { echo "Error: Failed to get release id for tag: $BUILDKITE_BRANCH"; echo "$response" | awk 'length($0)<100' >&2; exit 1; }

# Upload asset
echo "Uploading asset... $localAssetPath" >&2

# Construct url
ASSET="https://uploads.github.com/repos/gnosis/safe-android/releases/$id/assets"

curl --data-binary @"app/build/outputs/apk/rinkeby/gnosis-safe-${APP_VERSION_CODE}-rinkeby.apk" -H "$AUTH" -H "Content-Type: application/octet-stream" $ASSET?name=gnosis-safe-${APP_VERSION_CODE}-rinkeby.apk
curl --data-binary @"app/build/outputs/apk/release/gnosis-safe-${APP_VERSION_CODE}-release.apk" -H "$AUTH" -H "Content-Type: application/octet-stream" $ASSET?name=gnosis-safe-${APP_VERSION_CODE}-release.apk


