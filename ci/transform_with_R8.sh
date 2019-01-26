#!/bin/bash
# fail if any commands fails
set -e

cp ci/google-services.json.sample app/google-services.json
./gradlew clean --stacktrace
./gradlew :app:transformClassesAndResourcesWithR8ForRelease --stacktrace
