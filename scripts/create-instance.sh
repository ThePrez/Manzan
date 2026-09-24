#!/bin/bash
# Create a new Manzan instance.
#
# Creates the isolated config directory (/QOpenSys/etc/manzan-<name>) with
# mode 700 and seeds it with blank app.ini, data.ini, and dests.ini stubs
# (mode 600 each).  If the default instance config directory already exists,
# its .ini files are used as the starting template.
#
# Also creates a Service Commander service definition at
# /QOpenSys/etc/sc/services/manzan-<name>.yaml so the instance can be
# managed with the `sc` command.
#
# After running this script, edit the three .ini files in the new config
# directory and then start the instance with:
#
#   sc start manzan-<name>
#   # or directly:
#   java -jar /opt/manzan/manzan.jar --instance=<name>
#
# Usage:
#   ./scripts/create-instance.sh <instance-name> [USER_PROFILE]
#
#   instance-name   Lowercase alphanumeric name (1–32 chars, hyphens/underscores allowed)
#   USER_PROFILE    IBM i user profile that will own the config directory (default: current user)

set -euo pipefail

if [ $# -lt 1 ]; then
    echo "Usage: $0 <instance-name> [USER_PROFILE]" >&2
    exit 1
fi

INSTANCE_NAME="$1"
USER_PROFILE="${2:-$(id -un)}"
CONFIG_DIR="/QOpenSys/etc/manzan-${INSTANCE_NAME}"
DEFAULT_DIR="/QOpenSys/etc/manzan-default"
INI_FILES=("app.ini" "data.ini" "dests.ini")

# Validate name format (mirrors InstanceContext validation).
if ! echo "${INSTANCE_NAME}" | grep -qE '^[a-z0-9_-]{1,32}$'; then
    echo "Error: invalid instance name '${INSTANCE_NAME}'." >&2
    echo "Names must be 1-32 characters: lowercase letters, digits, hyphens, underscores." >&2
    exit 1
fi

if [ -d "${CONFIG_DIR}" ]; then
    echo "Error: instance '${INSTANCE_NAME}' already exists at ${CONFIG_DIR}." >&2
    exit 1
fi

echo "Creating instance '${INSTANCE_NAME}'..."

install -d -m 700 -o "${USER_PROFILE}" "${CONFIG_DIR}"

for INI in "${INI_FILES[@]}"; do
    DEST="${CONFIG_DIR}/${INI}"
    SRC="${DEFAULT_DIR}/${INI}"
    if [ -f "${SRC}" ]; then
        cp "${SRC}" "${DEST}"
        echo "  Seeded ${INI} from default instance."
    else
        touch "${DEST}"
        echo "  Created empty ${INI}."
    fi
    chmod 600 "${DEST}"
    chown "${USER_PROFILE}" "${DEST}"
done

# Create Service Commander YAML for this instance.
SC_SERVICES_DIR="/QOpenSys/etc/sc/services"
SC_YAML="${SC_SERVICES_DIR}/manzan-${INSTANCE_NAME}.yaml"

# IBM i job names cannot contain hyphens or symbols; strip non-alphanumerics
JOB_NAME_SUFFIX=$(echo "${INSTANCE_NAME}" | tr -cd '[:alnum:]')

JOB_NAME="mz${JOB_NAME_SUFFIX:0:8}"

if [ -d "${SC_SERVICES_DIR}" ]; then
    cat > "${SC_YAML}" <<EOF
name: manzan-${INSTANCE_NAME}
dir: .
start_cmd: /opt/manzan/bin/manzan --instance=${INSTANCE_NAME}
check_alive: ${JOB_NAME}
batch_mode: 'true'
sbmjob_jobname: ${JOB_NAME}
sbmjob_opts: JOBQ(QUSRNOMAX)
environment_vars:
- PATH=/QOpenSys/pkgs/bin:/QOpenSys/usr/bin:/usr/ccs/bin:/QOpenSys/usr/bin/X11:/usr/sbin:.:/usr/bin
EOF
    chmod 600 "${SC_YAML}"
    echo "  Created Service Commander definition at ${SC_YAML}."
else
    echo "  Warning: ${SC_SERVICES_DIR} does not exist — skipping Service Commander registration."
    echo "  You can register the instance manually later."
fi

echo ""
echo "Instance '${INSTANCE_NAME}' created at ${CONFIG_DIR}."
echo "Edit the .ini files there, then start with:"
echo "  sc start manzan-${INSTANCE_NAME}"
echo "  # or directly:"
echo "  java -jar /opt/manzan/manzan.jar --instance=${INSTANCE_NAME}"
