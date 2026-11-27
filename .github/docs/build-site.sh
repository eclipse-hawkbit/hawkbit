#!/usr/bin/env bash
#
# Copyright (c) 2026 Contributors to the Eclipse Foundation
#
# This program and the accompanying materials are made
# available under the terms of the Eclipse Public License 2.0
# which is available at https://www.eclipse.org/legal/epl-2.0/
#
# SPDX-License-Identifier: EPL-2.0
#

# Builds the versioned docs site (hawkbit.eclipse.dev) into a directory:
#   /                 docs/ of the latest GitHub release (or of LATEST_REF, if set)
#   /master/          docs/ of the master branch
#   /<tag>/           docs/ of each tag listed in .github/docs-versions.txt
#   /versions.json    version list, read by version-switcher.js
# The whole site is rebuilt each time, as a GitHub Pages deployment replaces the entire site.
#
# Usage: .github/docs/build-site.sh [output dir, default: site]
# Env:   LATEST_REF  - ref published as / instead of the latest release (e.g. a release branch with a docs fix)
#        MASTER_REF  - ref published as /master/ (default: master; local run: e.g. HEAD)
# Needs: git with all tags fetched, gh (authenticated, GH_TOKEN in CI) unless LATEST_REF is set.
# Local check: .github/docs/build-site.sh && python3 -m http.server -d site 8000

set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

site="${1:-site}"
versions_file=".github/docs-versions.txt"
switcher=".github/docs/version-switcher.js"
master_ref="${MASTER_REF:-master}"
latest_ref="${LATEST_REF:-}"

if [[ -z "$latest_ref" ]]; then
  # "latest" in GitHub terms: the newest published release that is neither a draft nor a prerelease
  latest_ref="$(gh release view --json tagName -q .tagName)"
fi
# latest_ref becomes a versions.json label, which isn't JSON-escaped: allow only the chars of the tag / branch names in
# use (git allows e.g. " in ref names)
if [[ ! "$latest_ref" =~ ^[0-9A-Za-z._/-]+$ ]]; then
  echo "ERROR: invalid ref '${latest_ref}' (allowed: 0-9 A-Z a-z . _ / -)" >&2
  exit 1
fi

# Extracts docs/ of a ref into a directory (docs/ prefix stripped)
extract_docs() {
  local ref="$1" dest="$2"
  if ! git cat-file -e "${ref}:docs/index.html" 2>/dev/null; then
    echo "ERROR: ${ref} has no docs/index.html" >&2
    return 1
  fi
  mkdir -p "$dest"
  git archive "$ref" docs | tar -x -C "$dest" --strip-components=1
}

# versions.json entries - not JSON-escaped: labels are refs validated above or tags matching the version regex below
entries=()

rm -rf "$site"
mkdir -p "$site"

echo "/ <- ${latest_ref}"
extract_docs "$latest_ref" "$site"
entries+=("{\"label\":\"${latest_ref} (latest)\",\"path\":\"/\"}")

echo "/master/ <- ${master_ref}"
extract_docs "$master_ref" "$site/master"
entries+=("{\"label\":\"master\",\"path\":\"/master/\"}")

if [[ -f "$versions_file" ]]; then
  # strip comments and blanks
  while read -r tag; do
    if [[ "$tag" == "$latest_ref" ]]; then
      echo "skip ${tag}: it's the latest release, already published as /"
      continue
    fi
    # the tag becomes a top level dir - a release version can't clash with master/ or the files of the latest docs
    if [[ ! "$tag" =~ ^[0-9]+(\.[0-9]+)+$ ]]; then
      echo "ERROR: ${versions_file}: '${tag}' isn't a release version tag (e.g. 1.2.0)" >&2
      exit 1
    fi
    path="/${tag}/"
    if [[ -e "${site}${path}" ]]; then
      echo "skip ${tag}: listed more than once"
      continue
    fi
    echo "${path} <- ${tag}"
    extract_docs "$tag" "${site}${path}"
    entries+=("{\"label\":\"${tag}\",\"path\":\"${path}\"}")
  done < <(sed -e 's/#.*//' -e 's/[[:space:]]//g' -e '/^$/d' "$versions_file")
fi

(IFS=,; echo "[${entries[*]}]") > "$site/versions.json"
echo "versions.json: $(cat "$site/versions.json")"

# The switcher is injected at build time (not referenced from docs/index.html) so it also works for tags released
# before it existed, and a local Docsify run of docs/ doesn't try to load it.
cp "$switcher" "$site/version-switcher.js"
find "$site" -name index.html -print0 | while IFS= read -r -d '' index; do
  sed -i.bak 's|</body>|<script src="/version-switcher.js"></script></body>|' "$index"
  rm -f "${index}.bak"
done

# docs/CNAME is meaningless for a workflow deployment (the custom domain is a repository setting)
find "$site" -name CNAME -delete
