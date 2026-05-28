#!/bin/bash

[[ -f /tmp/startup-succeeded ]] || { echo "startup not yet completed" >&2; exit 1; }

if exec 3<>/dev/tcp/127.0.0.1/8080; then
    exec 3<&-
    exec 3>&-
    exit 0
fi

echo "Tomcat is not accepting connections on 127.0.0.1:8080" >&2
exit 1