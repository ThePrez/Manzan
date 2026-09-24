#!/bin/bash
# One-time setup: grant the authorities required for multi-instance operation.
#
# Run this script once after installing or upgrading Manzan, logged in as a
# user with *SECADM special authority (typically QSECOFR).
#
# What this script does:
#   - Grants *PUBLIC *USE on the HANDLER program so every instance user
#     profile can call it via the IBM i Watch facility.
#   - Grants INSERT/SELECT on the event tables so instance jobs can write
#     and query their own rows.
#   - Grants SELECT/INSERT/UPDATE on AUDJRNTS so audit watermarks can be
#     maintained independently per instance.
#   - Grants *USE on the MANZANDTAQ data queue so instance jobs can
#     enqueue and dequeue events.
#
# Usage:
#   /opt/manzan/bin/setup-authorities [LIBRARY]
#
#   LIBRARY  The Manzan DB2 library (default: MANZAN)

set -euo pipefail

LIBRARY="${1:-MANZAN}"

echo "Granting multi-instance authorities in library: ${LIBRARY}"

# ILE handler program — called by IBM i Watch facility under the instance
# user profile, so *PUBLIC must have *USE.
system "GRTOBJAUT OBJ(${LIBRARY}/HANDLER) OBJTYPE(*PGM) USER(*PUBLIC) AUT(*USE)"

# Event tables — instances INSERT new rows and SELECT their own rows.
system "GRTOBJAUT OBJ(${LIBRARY}/MANZANMSG)  OBJTYPE(*FILE) USER(*PUBLIC) AUT(*CHANGE)"
system "GRTOBJAUT OBJ(${LIBRARY}/MANZANOTH)  OBJTYPE(*FILE) USER(*PUBLIC) AUT(*CHANGE)"
system "GRTOBJAUT OBJ(${LIBRARY}/MANZANPAL)  OBJTYPE(*FILE) USER(*PUBLIC) AUT(*CHANGE)"
system "GRTOBJAUT OBJ(${LIBRARY}/MANZANVLOG) OBJTYPE(*FILE) USER(*PUBLIC) AUT(*CHANGE)"

# Audit watermark table — instances SELECT, INSERT, and UPDATE their own row.
system "GRTOBJAUT OBJ(${LIBRARY}/AUDJRNTS) OBJTYPE(*FILE) USER(*PUBLIC) AUT(*CHANGE)"

# Data queue — HANDLER enqueues events; the Java component dequeues them.
system "GRTOBJAUT OBJ(${LIBRARY}/MANZANDTAQ) OBJTYPE(*DTAQ) USER(*PUBLIC) AUT(*CHANGE)"

echo "Done. Authorities granted successfully."
