#!/bin/bash
# fail if any commands fails
set -e

[[ -z "${SLACK_WEBHOOK}" ]] && exit 0

repo_url="$(git config --get remote.origin.url)"
release_path=/releases/tag/${BUILDKITE_BRANCH}
release_url="${repo_url/.git/$release_path}"
product_name=${BUILDKITE_ORGANIZATION_SLUG}/$(echo ${BUILDKITE_REPO} | rev | cut -d '/' -f 1 | rev)
curl -X POST -H 'Content-type: application/json' \
--data "{\"text\":\"<${release_url}|Version ${APP_VERSION_NAME}> for ${product_name} is out!\"}" \
$SLACK_WEBHOOK
