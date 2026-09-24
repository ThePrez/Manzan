#!/bin/bash
# List all known Manzan instances and their running status.
#
# An instance is discovered from either:
#   1. A config directory at /QOpenSys/etc/manzan-<name>
#   2. An active lock file at /var/run/manzan/manzan-<name>.lock
#
# Output columns:
#   NAME     Instance name
#   STATUS   RUNNING (live lock file) or STOPPED
#   PID      Process ID if running, - otherwise
#   CONFIG   Path to config directory (- if directory is missing)
#
# Usage:
#   ./scripts/list-instances.sh

set -euo pipefail

CONFIG_BASE="/QOpenSys/etc"
LOCK_DIR="/var/run/manzan"

printf "%-24s %-10s %-8s %s\n" "NAME" "STATUS" "PID" "CONFIG"
printf "%-24s %-10s %-8s %s\n" "------------------------" "----------" "--------" "------"

declare -A seen

# Discover instances from config directories.
for dir in "${CONFIG_BASE}"/manzan-*/; do
    [ -d "${dir}" ] || continue
    name="${dir#"${CONFIG_BASE}/manzan-"}"
    name="${name%/}"
    [ -z "${name}" ] && continue

    seen["${name}"]=1
    lock_file="${LOCK_DIR}/manzan-${name}.lock"
    status="STOPPED"
    pid="-"

    if [ -f "${lock_file}" ]; then
        candidate_pid=$(cat "${lock_file}" 2>/dev/null || echo "")
        if [ -n "${candidate_pid}" ] && kill -0 "${candidate_pid}" 2>/dev/null; then
            status="RUNNING"
            pid="${candidate_pid}"
        fi
    fi

    printf "%-24s %-10s %-8s %s\n" "${name}" "${status}" "${pid}" "${dir%/}"
done

# Show any orphaned running instances whose config directories are missing.
if [ -d "${LOCK_DIR}" ]; then
    for lock_file in "${LOCK_DIR}"/manzan-*.lock; do
        [ -f "${lock_file}" ] || continue
        base=$(basename "${lock_file}" .lock)
        name="${base#manzan-}"
        [ -n "${seen[${name}]+x}" ] && continue   # already printed

        candidate_pid=$(cat "${lock_file}" 2>/dev/null || echo "")
        if [ -n "${candidate_pid}" ] && kill -0 "${candidate_pid}" 2>/dev/null; then
            printf "%-24s %-10s %-8s %s\n" "${name}" "RUNNING" "${candidate_pid}" "-"
        fi
    done
fi
