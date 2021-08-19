#!/bin/bash
# fail if any commands fails
set -e

# Buildkite uses a clean state for each step (for concurrency)
source ./ci/prepare_env_buildkite.sh

echo "INFURA_API_KEY=$INFURA_API_KEY" > project_keys

./gradlew assembleRelease

# -------- Upload to github ----------
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
echo "Uploading assets..." >&2

# Construct url
ASSET="https://uploads.github.com/repos/gnosis/safe-android/releases/$id/assets"

curl --data-binary @"app/build/outputs/apk/release/gnosis-safe-${APP_VERSION_CODE}-release.apk" -H "$AUTH" -H "Content-Type: application/octet-stream" $ASSET?name=gnosis-safe-${APP_VERSION_CODE}-release.apk

ci/notify_slack.sh
