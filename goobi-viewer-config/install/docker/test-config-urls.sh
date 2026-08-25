#!/usr/bin/env bash
#
# Tests for configure-config-urls.sh -- the frontend/backend URL split applied to
# config_viewer.xml and config_oai.xml at container start.
#
# Not shipped in the image: the Dockerfile COPYs the docker/ helpers file by file.
#
# Scenarios mirror real container lifecycles. An env var change can only reach a
# container by recreating it, which resets the writable layer -- so every case
# below starts from pristine fixtures, except the one that deliberately renders
# twice in the same directory to model a plain restart.
#
# Run: goobi-viewer-config/docker/test-config-urls.sh

set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SUT="${HERE}/configure-config-urls.sh"
FIXTURES="${HERE}/../../src/main/resources"

PASS=0
FAIL=0

fail() {
  FAIL=$((FAIL + 1))
  printf '  \033[31mFAIL\033[0m %s\n' "$1"
  [ $# -gt 1 ] && printf '       expected: %s\n       actual:   %s\n' "$2" "${3-}"
  return 0
}

pass() {
  PASS=$((PASS + 1))
  printf '  \033[32mok\033[0m   %s\n' "$1"
}

# Text content of the first <el>...</el>, tolerating attributes on the tag.
val_el() {
  sed -n "s#.*<$2[^>]*>\([^<]*\)</$2>.*#\1#p" "$1" | head -1
}

# value="..." of the first line matching the given grep pattern.
val_attr() {
  grep -m1 "$2" "$1" | sed -n 's#.*value="\([^"]*\)".*#\1#p'
}

expect_eq() {
  local desc=$1 want=$2 got=$3
  if [ "$want" = "$got" ]; then pass "$desc"; else fail "$desc" "$want" "$got"; fi
}

expect_count() {
  local desc=$1 want=$2 file=$3 needle=$4 got
  got=$(grep -c -F "$needle" "$file")
  if [ "$want" = "$got" ]; then pass "$desc"; else fail "$desc" "$want occurrence(s)" "$got"; fi
}

expect_absent() {
  local desc=$1 file=$2 needle=$3
  if grep -q -F "$needle" "$file"; then
    fail "$desc" "no occurrence of '$needle'" "$(grep -c -F "$needle" "$file") found"
  else
    pass "$desc"
  fi
}

# --- harness -----------------------------------------------------------------

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT
CLASSES="$WORK/classes"
VIEWER="$CLASSES/config_viewer.xml"
OAI="$CLASSES/config_oai.xml"

# A freshly created container: pristine config from the image, then one render.
fresh_render() {
  rm -rf "$CLASSES"
  mkdir -p "$CLASSES"
  cp "$FIXTURES/config_viewer.xml" "$FIXTURES/config_oai.xml" "$CLASSES/"
  render "$@"
}

# Invoked through `bash` rather than via its shebang: the script targets the
# container, where /bin/bash exists, but the test also has to run on developer
# hosts that place bash elsewhere (NixOS, for one).
render() {
  VIEWER_DOMAIN="$1" VIEWER_BASE_PATH="$2" USE_SSL="$3" bash "$SUT" "$CLASSES"
}

if [ ! -f "$SUT" ]; then
  printf '\033[31mconfigure-config-urls.sh not found at %s\033[0m\n' "$SUT"
  printf 'All tests below will fail until it exists.\n'
fi

# --- 1. the frontend/backend split ------------------------------------------

echo
echo "1. production domain, base path /viewer, SSL on"
fresh_render demo.example.org viewer true

echo "   backend keys stay reachable from inside the container:"
expect_eq "config_oai documentResolverUrl" \
  "http://localhost:8080/viewer/sourcefile?id=" "$(val_el "$OAI" documentResolverUrl)"
expect_eq "config_oai harvestUrl" \
  "http://localhost:8080/viewer/harvest" "$(val_el "$OAI" harvestUrl)"
expect_eq "config_oai restApiUrl" \
  "http://localhost:8080/viewer/api/v1/" "$(val_el "$OAI" restApiUrl)"

echo "   frontend keys get the public address:"
expect_eq "config_oai baseURL" \
  "https://demo.example.org/viewer/oai" "$(val_el "$OAI" baseURL)"
expect_eq "config_oai urnResolverUrl" \
  "https://demo.example.org/viewer/resolver?urn=" "$(val_el "$OAI" urnResolverUrl)"
expect_eq "config_oai piResolverUrl" \
  "https://demo.example.org/viewer/piresolver?id=" "$(val_el "$OAI" piResolverUrl)"
expect_eq "config_oai oai_dc image url template" \
  "https://demo.example.org/viewer/image/{0}/{1}/" "$(val_attr "$OAI" 'label="url"')"
expect_eq "config_viewer urls.metadata.sourcefile" \
  "https://demo.example.org/viewer/sourcefile?id=" "$(val_el "$VIEWER" sourcefile)"
expect_eq "config_viewer urls.metadata.marc" \
  "https://demo.example.org/viewer/oai?verb=GetRecord&amp;metadataPrefix=marcxml&amp;identifier=" \
  "$(val_el "$VIEWER" marc)"
expect_eq "config_viewer urls.metadata.dc" \
  "https://demo.example.org/viewer/oai?verb=GetRecord&amp;metadataPrefix=oai_dc&amp;identifier=" \
  "$(val_el "$VIEWER" dc)"
expect_eq "config_viewer urls.metadata.ese" \
  "https://demo.example.org/viewer/oai?verb=GetRecord&amp;metadataPrefix=europeana&amp;identifier=" \
  "$(val_el "$VIEWER" ese)"
expect_eq "config_viewer urls.base" \
  "https://demo.example.org/viewer/" "$(val_el "$VIEWER" base)"
expect_eq "config_viewer urls.rest" \
  "https://demo.example.org/viewer/api/v1/" "$(val_el "$VIEWER" rest)"
expect_eq "config_viewer urls.iiif" \
  "https://demo.example.org/viewer/api/v1/" "$(val_el "$VIEWER" iiif)"
expect_eq "config_viewer urls.download" \
  "https://demo.example.org/viewer/download/" "$(val_el "$VIEWER" download)"

echo "   nothing stray left behind:"
# config_viewer.xml holds no backend keys, so no container-internal URL may remain
expect_absent "config_viewer has no leftover localhost:8080" "$VIEWER" "http://localhost:8080/viewer"
# config_oai.xml keeps exactly the three backend keys
expect_count "config_oai keeps exactly 3 container-internal URLs" 3 "$OAI" "http://localhost:8080/viewer"

echo "   Solr URLs are handled separately in run.sh and must not be touched:"
expect_eq "config_viewer urls.solr untouched" \
  "http://localhost:8983/solr/current" "$(val_el "$VIEWER" solr)"
expect_eq "config_oai solrUrl untouched" \
  "http://localhost:8983/solr/current" "$(val_el "$OAI" solrUrl)"

# --- 2. a plain restart re-runs run.sh over the already-rendered files -------

echo
echo "2. restart -- same container, same env, run.sh runs again"
cp "$VIEWER" "$WORK/viewer.before"
cp "$OAI" "$WORK/oai.before"
render demo.example.org viewer true
if diff -q "$WORK/viewer.before" "$VIEWER" >/dev/null; then
  pass "config_viewer unchanged by the second run"
else
  fail "config_viewer unchanged by the second run" "no diff" "$(diff "$WORK/viewer.before" "$VIEWER" | head -5)"
fi
if diff -q "$WORK/oai.before" "$OAI" >/dev/null; then
  pass "config_oai unchanged by the second run"
else
  fail "config_oai unchanged by the second run" "no diff" "$(diff "$WORK/oai.before" "$OAI" | head -5)"
fi

# --- 3. plain HTTP ----------------------------------------------------------

echo
echo "3. recreated with SSL off and a different domain"
fresh_render other.example.net viewer false
expect_eq "config_oai baseURL" \
  "http://other.example.net/viewer/oai" "$(val_el "$OAI" baseURL)"
expect_eq "config_viewer urls.metadata.marc" \
  "http://other.example.net/viewer/oai?verb=GetRecord&amp;metadataPrefix=marcxml&amp;identifier=" \
  "$(val_el "$VIEWER" marc)"
expect_eq "backend documentResolverUrl still container-internal" \
  "http://localhost:8080/viewer/sourcefile?id=" "$(val_el "$OAI" documentResolverUrl)"

# --- 4. a non-default VIEWER_BASE_PATH applies to both audiences ------------

echo
echo "4. recreated with VIEWER_BASE_PATH=digital"
fresh_render other.example.net digital false
expect_eq "backend documentResolverUrl uses the new base path" \
  "http://localhost:8080/digital/sourcefile?id=" "$(val_el "$OAI" documentResolverUrl)"
expect_eq "backend harvestUrl uses the new base path" \
  "http://localhost:8080/digital/harvest" "$(val_el "$OAI" harvestUrl)"
expect_eq "backend restApiUrl uses the new base path" \
  "http://localhost:8080/digital/api/v1/" "$(val_el "$OAI" restApiUrl)"
expect_eq "frontend urls.rest uses the new base path" \
  "http://other.example.net/digital/api/v1/" "$(val_el "$VIEWER" rest)"
expect_eq "frontend baseURL uses the new base path" \
  "http://other.example.net/digital/oai" "$(val_el "$OAI" baseURL)"
expect_absent "no /viewer path left in config_oai" "$OAI" "8080/viewer"

# --- 5. root deployment (VIEWER_BASE_PATH empty) ---------------------------

echo
echo "5. recreated at the context root -- VIEWER_BASE_PATH empty"
fresh_render other.example.net "" false
expect_eq "backend documentResolverUrl at root" \
  "http://localhost:8080/sourcefile?id=" "$(val_el "$OAI" documentResolverUrl)"
expect_eq "backend harvestUrl at root" \
  "http://localhost:8080/harvest" "$(val_el "$OAI" harvestUrl)"
expect_eq "frontend urls.rest at root" \
  "http://other.example.net/api/v1/" "$(val_el "$VIEWER" rest)"

# --- summary ---------------------------------------------------------------

echo
printf '%s passed, %s failed\n' "$PASS" "$FAIL"
[ "$FAIL" -eq 0 ]
