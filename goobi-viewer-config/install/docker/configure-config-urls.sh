#!/bin/bash
#
# Rewrites the placeholder URLs in the deployed config_viewer.xml and
# config_oai.xml for their two distinct audiences.
#
#   frontend  handed to a browser or an OAI harvester  -> public address
#   backend   fetched by the viewer itself, in-process -> container-internal
#
# Getting this wrong is silent: a backend key pointed at the public address
# leaves the viewer unable to reach itself, and the OAI verbs that depend on it
# answer with an empty <GetRecord/> rather than an error.
#
# Usage: configure-config-urls.sh <webapp-WEB-INF-classes-dir>
# Reads: VIEWER_DOMAIN, VIEWER_BASE_PATH (no leading slash), USE_SSL

set -eu

CLASSES_DIR=$1

VIEWER_CONFIG="${CLASSES_DIR}/config_viewer.xml"
OAI_CONFIG="${CLASSES_DIR}/config_oai.xml"

# The address the packaged configs ship with, and which everything below keys off.
PLACEHOLDER="http://localhost:8080/viewer"

# Parked here while the frontend sweep runs, so that sweep cannot see the backend
# keys. A sentinel is needed rather than rewriting them in place first: with the
# default VIEWER_BASE_PATH the backend address is *identical* to the placeholder,
# so an in-place rewrite would leave text the sweep then clobbers.
SENTINEL="@@GOOBI_VIEWER_BACKEND_BASE@@"

# config_oai.xml keys that the viewer resolves itself, over HTTP, in-process.
# Each is fetched via NetTools by the connector:
#   documentResolverUrl  METSFormat / LIDOFormat / SruServlet -- fetches the METS
#                        source file. MARCXMLFormat extends METSFormat, so MARCXML
#                        depends on it too.
#   harvestUrl           GoobiViewerUpdateFormat
#   restApiUrl           OAIDCFormat (the #TOC# master value) and TEIFormat
#
# Beware the near-miss in naming: config_oai.xml's <restApiUrl> is backend, while
# config_viewer.xml's <rest> is frontend. Same REST API, different consumer -- the
# latter is handed to the browser and embedded in IIIF manifests.
#
# Everything else carrying the placeholder is frontend: the <urls><metadata>
# marc/dc/ese/sourcefile entries are rendered as href attributes, <rest>/<iiif>/
# <base>/<download> reach the browser, and config_oai.xml's baseURL/urnResolverUrl/
# piResolverUrl are emitted into OAI responses for harvesters to follow.
BACKEND_KEYS="documentResolverUrl harvestUrl restApiUrl"

if [ "${USE_SSL}" = "true" ]; then
  frontend_scheme="https"
else
  frontend_scheme="http"
fi

path_suffix="${VIEWER_BASE_PATH:+/${VIEWER_BASE_PATH}}"
frontend_base="${frontend_scheme}://${VIEWER_DOMAIN}${path_suffix}"

# Always plain http on the Tomcat port: the image installs a single HTTP/1.1
# connector on 8080 and terminates no TLS -- the reverse proxy does. USE_SSL must
# not reach this value. Loopback rather than the compose service name because the
# service name resolves to a site-local address, which NetTools' outbound
# validation rejects outright.
backend_base="http://localhost:8080${path_suffix}"

# 1. Park the backend keys out of the sweep's reach.
if [ -f "$OAI_CONFIG" ]; then
  for key in $BACKEND_KEYS; do
    sed -i "s#<${key}>${PLACEHOLDER}#<${key}>${SENTINEL}#" "$OAI_CONFIG"
  done
fi

# 2. Whatever still carries the placeholder is handed out.
for config in "$VIEWER_CONFIG" "$OAI_CONFIG"; do
  [ -f "$config" ] && sed -i "s#${PLACEHOLDER}#${frontend_base}#g" "$config"
done

# 3. Resolve the sentinel to the container-internal address.
if [ -f "$OAI_CONFIG" ]; then
  sed -i "s#${SENTINEL}#${backend_base}#g" "$OAI_CONFIG"
fi

echo "Frontend URLs set to ${frontend_base}, backend URLs to ${backend_base}"
