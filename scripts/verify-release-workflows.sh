#!/usr/bin/env bash

set -euo pipefail

root="${1:-.}"
publish="${root}/.github/workflows/publish-release.yml"
dispatcher="${root}/.github/workflows/dispatch-stable-release.yml"
prepare="${root}/.github/workflows/prepare-snapshot.yml"

require_file() {
    if [[ ! -f "$1" ]]; then
        echo "Release workflow file does not exist: $1" >&2
        exit 1
    fi
}

require_text() {
    local file="$1"
    local text="$2"
    if ! grep -Fq -- "${text}" "${file}"; then
        echo "${file} must contain: ${text}" >&2
        exit 1
    fi
}

forbid_text() {
    local file="$1"
    local text="$2"
    if grep -Fq -- "${text}" "${file}"; then
        echo "${file} must not contain: ${text}" >&2
        exit 1
    fi
}

require_file "${publish}"
require_file "${dispatcher}"
require_file "${prepare}"

require_text "${publish}" "workflow_dispatch:"
require_text "${publish}" "release_tag:"
require_text "${publish}" 'test "${GITHUB_REF}" = "refs/heads/main"'
require_text "${publish}" 'ref: ${{ inputs.release_tag }}'
require_text "${publish}" "git merge-base --is-ancestor"
require_text "${publish}" "environment: vadmin"
require_text "${publish}" "github.actor == 'github-actions[bot]'"
require_text "${publish}" 'gh workflow run prepare-snapshot.yml --repo "${GITHUB_REPOSITORY}" --ref main'
forbid_text "${publish}" "tags:"
forbid_text "${publish}" "git push"
forbid_text "${publish}" "versions:set"

require_text "${dispatcher}" "push:"
require_text "${dispatcher}" "tags:"
require_text "${dispatcher}" "v[0-9]+.[0-9]+.[0-9]+"
require_text "${dispatcher}" "actions: write"
require_text "${dispatcher}" "contents: read"
require_text "${dispatcher}" '^v[0-9]+\.[0-9]+\.[0-9]+$'
require_text "${dispatcher}" "git cat-file -t"
require_text "${dispatcher}" "git merge-base --is-ancestor"
require_text "${dispatcher}" 'gh workflow run publish-release.yml --repo "${GITHUB_REPOSITORY}" --ref main -f "release_tag=${RELEASE_TAG}"'
for forbidden in \
    "MAVEN_CENTRAL_USERNAME" \
    "MAVEN_CENTRAL_PASSWORD" \
    "GPG_PRIVATE_KEY" \
    "GPG_PASSPHRASE" \
    "environment:" \
    "git push" \
    "id-token:" \
    "secrets." \
    "./mvnw" \
    " deploy"
do
    forbid_text "${dispatcher}" "${forbidden}"
done

require_text "${prepare}" "workflow_run:"
require_text "${prepare}" "- Publish Release"
require_text "${prepare}" "contents: write"
require_text "${prepare}" "pull-requests: write"
require_text "${prepare}" "git tag --points-at"
require_text "${prepare}" 'next_version="${BASH_REMATCH[1]}.${BASH_REMATCH[2]}.$((BASH_REMATCH[3] + 1))-SNAPSHOT"'
require_text "${prepare}" "<artifactId>vadmin-parent</artifactId>"
require_text "${prepare}" "git add pom.xml"
require_text "${prepare}" "git push --set-upstream origin"
require_text "${prepare}" "gh pr create"
require_text "${prepare}" "--base main"
forbid_text "${prepare}" "git push origin main"
forbid_text "${prepare}" "git push --force"
forbid_text "${prepare}" "versions:set"
forbid_text "${prepare}" 'next_version="${BASH_REMATCH[1]}.$((BASH_REMATCH[2] + 1)).0-SNAPSHOT"'

echo "Release workflow verification passed: ${root}"
