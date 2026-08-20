#! /bin/sh

export JAVA_OPTS="${JAVA_OPTS} -XX:+UseG1GC"
export JAVA_OPTS="${JAVA_OPTS} -XX:+ParallelRefProcEnabled"
export JAVA_OPTS="${JAVA_OPTS} -XX:+DisableExplicitGC"
export JAVA_OPTS="${JAVA_OPTS} -Dfile.encoding='utf-8'"
export JAVA_OPTS="${JAVA_OPTS} --add-exports=java.desktop/sun.awt.image=ALL-UNNAMED"
