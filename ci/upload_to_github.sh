#!/bin/bash
# fail if any commands fails
set -e

echo "apiSecret=$FABRIC_API_SECRET" > app/fabric.properties
echo "apiKey=$FABRIC_API_KEY" >> app/fabric.properties

echo "INFURA_API_KEY=$INFURA_API_KEY" > project_keys

./gradlew assembleRinkeby assembleRelease
