#!/bin/bash

# run.sh writes the resolved webapp path (no leading slash, empty when deployed
# at the root) into the marker file just before starting Tomcat.
[[ -f /tmp/startup-succeeded ]] || { echo "startup not yet completed" >&2; exit 1; }

BASE_PATH="$(< /tmp/startup-succeeded)"
URL="http://127.0.0.1:8080/${BASE_PATH:+${BASE_PATH}/}"

# Tomcat accepts connections even when the webapp failed to deploy, and answers 404 for a context that never started.
# Only a 2xx/3xx response proves the application itself is actually serving.
STATUS="$(curl --silent --output /dev/null --max-time 10 --write-out '%{http_code}' "$URL")"

case "$STATUS" in
    2??|3??)
        exit 0
        ;;
    000)
        echo "Tomcat is not accepting connections on 127.0.0.1:8080" >&2
        exit 1
        ;;
    *)
        echo "Viewer webapp at ${URL} returned HTTP ${STATUS}" >&2
        exit 1
        ;;
esac
