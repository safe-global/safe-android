#!/bin/bash
# fail if any commands fails
set -e

repo_url="$(git config --get remote.origin.url)"
release_path="/releases/tag/${TRAVIS_TAG}"
release_url="${repo_url/.git/$release_path}"
curl -X POST -H 'Content-type: application/json' \
--data "{\"text\":\"<${release_url}|Version ${APP_VERSION_NAME}> for ${TRAVIS_REPO_SLUG} is out!\"}" \
https://hooks.slack.com/services/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