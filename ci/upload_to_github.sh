#!/bin/bash
# fail if any commands fails
set -e

echo "apiSecret=$FABRIC_API_SECRET" > app/fabric.properties
echo "apiKey=$FABRIC_API_KEY" >> app/fabric.properties

echo "INFURA_API_KEY=$INFURA_API_KEY" > project_keys

./gradlew assembleRinkeby assembleRelease


# Define variables.
REPO="https://api.github.com/repos/gnosis/safe-android"
TAGS="$REPO/releases/tags/$tag"
AUTH="Authorization: token $github_api_token"
WGET_ARGS="--content-disposition --auth-no-challenge --no-cookie"
CURL_ARGS="-LJO#"

# Read asset tags.
response=$(curl -sH "$AUTH" $TAGS)

# Get ID of the asset based on given filename.
eval $(echo "$response" | grep -m 1 "id.:" | grep -w id | tr : = | tr -cd '[[:alnum:]]=')
[ "$id" ] || { echo "Error: Failed to get release id for tag: $tag"; echo "$response" | awk 'length($0)<100' >&2; exit 1; }

# Upload asset
echo "Uploading asset... $localAssetPath" >&2

# Construct url
ASSET="https://uploads.github.com/repos/$owner/$repo/releases/$id/assets?name=$(basename $filename)"

curl --data-binary @"app/build/outputs/apk/*/*-rinkeby.apk" -H "$AUTH" -H "Content-Type: application/octet-stream" $ASSET
curl --data-binary @"app/build/outputs/apk/*/*-release.apk" -H "$AUTH" -H "Content-Type: application/octet-stream" $ASSET


