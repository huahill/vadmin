#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
VERIFY_SCRIPT="${ROOT_DIR}/scripts/verify-release-workflows.sh"

if ! bash "${VERIFY_SCRIPT}" "${ROOT_DIR}"; then
    echo "Expected the repository release workflows to pass verification." >&2
    exit 1
fi

temp_dir="$(mktemp -d)"
trap 'rm -rf "${temp_dir}"' EXIT
mkdir -p "${temp_dir}/.github/workflows"
cp "${ROOT_DIR}/.github/workflows/publish-release.yml" \
    "${ROOT_DIR}/.github/workflows/dispatch-stable-release.yml" \
    "${ROOT_DIR}/.github/workflows/prepare-snapshot.yml" \
    "${temp_dir}/.github/workflows/"

printf '\ngit push origin main\n' >> "${temp_dir}/.github/workflows/prepare-snapshot.yml"
if bash "${VERIFY_SCRIPT}" "${temp_dir}" >/dev/null 2>&1; then
    echo "Expected release workflow verification to reject a direct push to main." >&2
    exit 1
fi

cp "${ROOT_DIR}/.github/workflows/prepare-snapshot.yml" "${temp_dir}/.github/workflows/prepare-snapshot.yml"
printf '\nMAVEN_CENTRAL_PASSWORD: blocked\n' >> "${temp_dir}/.github/workflows/dispatch-stable-release.yml"
if bash "${VERIFY_SCRIPT}" "${temp_dir}" >/dev/null 2>&1; then
    echo "Expected release workflow verification to reject secrets in the tag dispatcher." >&2
    exit 1
fi

python3 - "${ROOT_DIR}/.github/workflows/prepare-snapshot.yml" <<'PY'
import os
import sys
import tempfile
from pathlib import Path

import yaml

workflow = yaml.safe_load(Path(sys.argv[1]).read_text())
script = workflow["jobs"]["prepare"]["steps"][2]["run"]
start = script.index("python3 - <<'PY'\n") + len("python3 - <<'PY'\n")
python = script[start:script.index("\nPY\n", start)]
compile(python, "prepare-snapshot.py", "exec")

with tempfile.TemporaryDirectory() as directory:
    pom = Path(directory, "pom.xml")
    pom.write_text(
        "<project>\n"
        "  <artifactId>vadmin-parent</artifactId>\n"
        "  <version>0.5.0</version>\n"
        "  <properties><version>0.5.0</version></properties>\n"
        "</project>\n"
    )
    os.environ["CURRENT_VERSION"] = "0.5.0"
    os.environ["NEXT_VERSION"] = "0.5.1-SNAPSHOT"
    previous = Path.cwd()
    os.chdir(directory)
    try:
        exec(python, {"__name__": "__main__"})
    finally:
        os.chdir(previous)
    updated = pom.read_text()
    if updated.count("<version>0.5.1-SNAPSHOT</version>") != 1 or "<version>0.5.0</version>" not in updated:
        raise SystemExit(f"Root version replacement was incorrect:\n{updated}")
PY

echo "Release workflow verification tests passed."
