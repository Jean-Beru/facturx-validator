#!/bin/sh
set -u

DEBUG=0
FORMAT=""
SOURCE=""

usage()
{
    echo "usage: validator [--debug] [--format=yaml|md|xml] <pdf-path>" >&2
}

for arg in "$@"; do
    case "$arg" in
        --debug)
            DEBUG=1
            ;;
        --format=*)
            FORMAT="${arg#--format=}"
            case "$FORMAT" in
                yaml|md|xml) ;;
                *)
                    echo "unknown format: $FORMAT" >&2
                    usage
                    exit 2
                    ;;
            esac
            ;;
        -*)
            echo "unknown option: $arg" >&2
            usage
            exit 2
            ;;
        *)
            if [ -n "$SOURCE" ]; then
                usage
                exit 2
            fi
            SOURCE="$arg"
            ;;
    esac
done

if [ -z "$SOURCE" ]; then
    usage
    exit 2
fi

if [ ! -f "$SOURCE" ]; then
    echo "file not found: $SOURCE" >&2
    exit 2
fi

LOG_FLAG="-Dorg.slf4j.simpleLogger.defaultLogLevel=error"
if [ "$DEBUG" -eq 1 ]; then
    LOG_FLAG=""
fi

if [ -z "$FORMAT" ]; then
    java -Xmx1G -Dfile.encoding=UTF-8 $LOG_FLAG -jar /opt/mustang-cli.jar --action validate --source "$SOURCE"
    MUSTANG_RC=$?

    java -Xmx512M -Dfile.encoding=UTF-8 -jar /opt/postcheck.jar "$SOURCE"
    POST_RC=$?
else
    MUSTANG_OUT="$(mktemp -t mustang.XXXXXX)"
    trap 'rm -f "$MUSTANG_OUT"' EXIT

    java -Xmx1G -Dfile.encoding=UTF-8 -Dorg.slf4j.simpleLogger.defaultLogLevel=error \
        -jar /opt/mustang-cli.jar --action validate --source "$SOURCE" >"$MUSTANG_OUT" 2>/dev/null
    MUSTANG_RC=$?

    java -Xmx512M -Dfile.encoding=UTF-8 -jar /opt/postcheck.jar \
        "--format=$FORMAT" "--mustang-output=$MUSTANG_OUT" "--mustang-rc=$MUSTANG_RC" "$SOURCE"
    POST_RC=$?
fi

if [ "$MUSTANG_RC" -ne 0 ] || [ "$POST_RC" -ne 0 ]; then
    exit 1
fi
exit 0
