#!/bin/bash
# fail if any commands fails
set -e

# Buildkite uses a clean state for each step (for concurrency)
source ./ci/prepare_env_buildkite.sh

echo "INFURA_API_KEY=$INFURA_API_KEY" > project_keys
echo "INTERCOM_API_KEY=$INTERCOM_API_KEY" > project_keys
echo "INTERCOM_APP_ID=$INTERCOM_APP_ID" > project_keys

./gradlew assembleRelease

# -------- Upload to github ----------
REPO="https://api.github.com/repos/safe-global/safe-android"
TAGS="$REPO/releases/tags/$BUILDKITE_BRANCH"
AUTH="Authorization: token $GITHUB_API_KEY"

# Read asset tags.
response=$(curl -sH "$AUTH" $TAGS)

# Get ID of the asset based on given filename.
eval "$(echo "$response" | grep -m 1 "id.:" | grep -w id | tr : = | tr -cd '[[:alnum:]]=')"
[ "$id" ] || { echo "Error: Failed to get release id for tag: $BUILDKITE_BRANCH"; echo "$response" | awk 'length($0)<100' >&2; exit 1; }

# Upload asset
echo "Uploading assets..." >&2

curl -L \
  -X POST \
  -H "Accept: application/vnd.github+json" \
  -H "Authorization: Bearer $GITHUB_API_KEY" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  -H "Content-Type: application/octet-stream" \
  "https://uploads.github.com/repos/safe-global/safe-android/releases/$id/assets?name=safe-${APP_VERSION_CODE}-release.apk" \
  --data-binary "@app/build/outputs/apk/release/safe-${APP_VERSION_CODE}-release.apk"

ci/notify_slack.sh
