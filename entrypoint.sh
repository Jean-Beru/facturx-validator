#!/bin/sh
set -u

if [ $# -ne 1 ]; then
    echo "usage: validator <pdf-path>" >&2
    exit 2
fi

SOURCE="$1"
if [ ! -f "$SOURCE" ]; then
    echo "validator: file not found: $SOURCE" >&2
    exit 2
fi

java -Xmx1G -Dfile.encoding=UTF-8 -jar /opt/mustang-cli.jar --action validate --source "$SOURCE"
MUSTANG_RC=$?

java -Xmx512M -Dfile.encoding=UTF-8 -jar /opt/postcheck.jar "$SOURCE"
POST_RC=$?

if [ "$MUSTANG_RC" -ne 0 ] || [ "$POST_RC" -ne 0 ]; then
    exit 1
fi
exit 0
