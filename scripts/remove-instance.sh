#!/bin/bash
# Remove a Manzan instance.
#
# Stops the instance if it is still running (via ENDWCH / ENDJOB), removes
# the config directory, cleans up any stale lock file, and optionally purges
# all database rows written by the instance.
#
# Usage:
#   ./scripts/remove-instance.sh <instance-name> [--clean-db] [LIBRARY]
#
#   instance-name   Name of the instance to remove.
#   --clean-db      Also delete all database rows belonging to this instance
#                   by calling the CLEANUP_INSTANCE_DATA stored procedure.
#   LIBRARY         Manzan DB2 library used when --clean-db is specified (default: MANZAN).

set -euo pipefail

if [ $# -lt 1 ]; then
    echo "Usage: $0 <instance-name> [--clean-db] [LIBRARY]" >&2
    exit 1
fi

INSTANCE_NAME="$1"
CLEAN_DB=false
LIBRARY="MANZAN"

shift
while [ $# -gt 0 ]; do
    case "$1" in
        --clean-db) CLEAN_DB=true ;;
        *)          LIBRARY="$1" ;;
    esac
    shift
done

CONFIG_DIR="/QOpenSys/etc/manzan-${INSTANCE_NAME}"
LOCK_FILE="/var/run/manzan/manzan-${INSTANCE_NAME}.lock"

# Validate name format.
if ! echo "${INSTANCE_NAME}" | grep -qE '^[a-z0-9_-]{1,32}$'; then
    echo "Error: invalid instance name '${INSTANCE_NAME}'." >&2
    exit 1
fi

if [ ! -d "${CONFIG_DIR}" ]; then
    echo "Error: instance '${INSTANCE_NAME}' does not exist at ${CONFIG_DIR}." >&2
    exit 1
fi

# Refuse to remove a live instance — the operator must stop it first.
if [ -f "${LOCK_FILE}" ]; then
    PID=$(cat "${LOCK_FILE}" 2>/dev/null || echo "")
    if [ -n "${PID}" ] && kill -0 "${PID}" 2>/dev/null; then
        echo "Error: instance '${INSTANCE_NAME}' is currently running (PID ${PID})." >&2
        echo "Stop it first, then re-run this script." >&2
        exit 1
    fi
    # Lock file is stale — remove it.
    rm -f "${LOCK_FILE}"
    echo "Removed stale lock file for instance '${INSTANCE_NAME}'."
fi

# Optional DB cleanup.
if [ "${CLEAN_DB}" = true ]; then
    echo "Cleaning database rows for instance '${INSTANCE_NAME}'..."
    system "RUNSQL SQL('CALL ${LIBRARY}.CLEANUP_INSTANCE_DATA(''${INSTANCE_NAME}'')') COMMIT(*NONE) NAMING(*SQL)"
    echo "Database rows removed."
fi

# Remove the config directory.
rm -rf "${CONFIG_DIR}"
echo "Removed config directory ${CONFIG_DIR}."
echo "Instance '${INSTANCE_NAME}' has been removed."
