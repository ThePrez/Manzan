# Manzan Multi-Instance — Tester's Test Plan

**Document Purpose**: Step-by-step test plan for validating multi-instance support.  
**Audience**: Testers who are new to Manzan.  
**Last Updated**: 2026-07-08  
**Build Under Test**: Branch `multInstance` — Stages 1–6 complete.

---

## 1. Background for New Testers

### What is Manzan?

Manzan is an IBM i application that watches IBM i system events (message queues, audit journals, file activity, HTTP endpoints) and forwards them to external destinations (Slack, Kafka, Elasticsearch, email, etc.). It runs as a Java process on IBM i (PASE environment) using Apache Camel routing.

### What changed in this release?

Previously, only **one** Manzan process could run on a given IBM i system. This release adds **multi-instance support**: you can now run as many named instances as you need, each completely isolated from the others. Key changes:

| Area | Before | After |
|---|---|---|
| Startup | `java -jar manzan.jar` (one process only) | `java -jar manzan.jar --instance=<name>` |
| Config location | `/QOpenSys/etc/manzan/` (single shared dir) | `/QOpenSys/etc/manzan-<name>/` per instance |
| Duplicate prevention | None | Lock file at `/var/run/manzan/manzan-<name>.lock` |
| Session ID | Shared across all events | Unique per instance, e.g. `WATSON0001` |
| Database | Tables had no per-instance filter on `AUDJRNTS` | All tables now carry `SESSION_ID`; `AUDJRNTS` migrated |

### Concepts you need to know

| Term | Meaning |
|---|---|
| **Instance** | A named, independent Manzan process. Name must be 1–32 chars, lowercase letters/digits/hyphens/underscores. |
| **Instance context** | The resolved combination of instance name + config directory + user profile. |
| **Lock file** | `/var/run/manzan/manzan-<name>.lock` — holds the PID of a running instance. Prevents two processes using the same name. |
| **Session ID** | A 10-character human-readable identifier (e.g. `WATSON0001`) written into every DB row the instance produces. |
| **Config directory** | `/QOpenSys/etc/manzan-<name>/` — holds `app.ini`, `data.ini`, `dests.ini` for that instance only. Permissions: 700. |
| **`MANZAN_INSTANCE`** | Optional environment variable — alternative to the `--instance=` flag. |
| **Default instance** | The instance used when no `--instance` arg or env var is given. Name = `"default"`. Backward-compatible with pre-release installs. |

---

## 2. Pre-Test Setup

### 2.1 Prerequisites

- [ ] Access to an IBM i system (PASE shell)
- [ ] Manzan JAR installed at `/opt/manzan/manzan.jar` (or equivalent)
- [ ] Manzan ILE library (`MANZAN`) built and installed — run `make all` in the `ile/` directory
- [ ] Multi-instance authorities granted: run `/opt/manzan/scripts/setup-multi-instance-authorities.sh`  
  *(requires `*SECADM` authority; ask your system administrator if needed)*
- [ ] Your user profile has authority to write to `/QOpenSys/etc/` and `/var/run/manzan/`
- [ ] SQL client available (ACS Run SQL Scripts or `db2` CLI)

### 2.2 Verify baseline installation

```bash
# Confirm the JAR starts and prints version
java -jar /opt/manzan/manzan.jar --version

# Expected: prints version number and ILE handler version; exits cleanly.
```

### 2.3 Create a minimal working configuration (default instance)

If no default instance exists yet, create one so it can be used as a template in later tests:

```bash
/opt/manzan/scripts/create-instance.sh default $(id -un)
```

Edit `/QOpenSys/etc/manzan-default/app.ini` with valid IBM i credentials (ask your administrator for the values). Leave `data.ini` and `dests.ini` minimal or empty for now — the startup tests in section 4 do not require active watches.

---

## 3. Test Scope

### In scope

- Instance name validation (accept/reject rules)
- `create-instance.sh` — creation, permissions, templating
- `list-instances.sh` — correct RUNNING/STOPPED status
- `remove-instance.sh` — refused on live instance; succeeds on stopped instance; `--clean-db`
- `ManzanMainApp` startup with `--instance=` and `MANZAN_INSTANCE` env var
- Default-instance backward compatibility (no flag)
- Duplicate instance prevention (lock file)
- Session ID format and uniqueness
- Config directory and file permissions (700/600)
- Database migration: `AUDJRNTS.SESSION_ID` column and index
- `CLEANUP_INSTANCE_DATA` stored procedure
- Multi-instance data isolation (two instances do not see each other's DB rows)

### Out of scope for this test plan

- Specific destination integrations (Kafka, Slack, etc.) — covered by existing destination tests
- IBM i job submission via `SBMJOB` / Service Commander
- Performance testing beyond basic smoke test of concurrent startup

---

## 4. Test Cases

Each test case follows this format:

> **TC-nn — Title**  
> **Preconditions** | **Steps** | **Expected results** | **Pass/Fail**

---

### Section A — Instance Name Validation

---

**TC-01 — Valid instance names are accepted**

**Preconditions**: No instance named `test01` exists.

**Steps**:
```bash
/opt/manzan/scripts/create-instance.sh test01 $(id -un)
```

**Expected result**: Script prints `Instance 'test01' created at /QOpenSys/etc/manzan-test01.`  No error message.

**Pass / Fail**: ___

---

**TC-02 — Valid name with hyphens and underscores**

**Steps**:
```bash
/opt/manzan/scripts/create-instance.sh my-test_02 $(id -un)
```

**Expected result**: Instance created successfully.

**Pass / Fail**: ___

---

**TC-03 — Name with uppercase letters is rejected**

**Steps**:
```bash
/opt/manzan/scripts/create-instance.sh Watson $(id -un)
```

**Expected result**: Script prints an error containing `invalid instance name` and exits with a non-zero code. Directory `/QOpenSys/etc/manzan-Watson` is **not** created.

**Pass / Fail**: ___

---

**TC-04 — Name longer than 32 characters is rejected**

**Steps**:
```bash
/opt/manzan/scripts/create-instance.sh abcdefghijklmnopqrstuvwxyz1234567 $(id -un)
# That name is 33 characters — one over the limit.
```

**Expected result**: Error message, no directory created.

**Pass / Fail**: ___

---

**TC-05 — Empty name is rejected**

**Steps**:
```bash
/opt/manzan/scripts/create-instance.sh "" $(id -un)
```

**Expected result**: Error message, no directory created.

**Pass / Fail**: ___

---

**TC-06 — Name with path traversal characters is rejected**

**Steps**:
```bash
/opt/manzan/scripts/create-instance.sh "../etc/passwd" $(id -un)
```

**Expected result**: Error message (invalid characters), no directory created, no file system traversal.

**Pass / Fail**: ___

---

### Section B — `create-instance.sh`

---

**TC-07 — Config directory created with correct permissions**

**Preconditions**: TC-01 ran and `test01` was created.

**Steps**:
```bash
ls -ld /QOpenSys/etc/manzan-test01
```

**Expected result**: Permissions are `drwx------` (700). Owner matches the user profile supplied to `create-instance.sh`.

**Pass / Fail**: ___

---

**TC-08 — .ini files created with correct permissions**

**Steps**:
```bash
ls -l /QOpenSys/etc/manzan-test01/
```

**Expected result**: `app.ini`, `data.ini`, and `dests.ini` all exist with permissions `-rw-------` (600), owned by the specified user profile.

**Pass / Fail**: ___

---

**TC-09 — Duplicate instance creation is rejected**

**Preconditions**: Instance `test01` already exists (from TC-01).

**Steps**:
```bash
/opt/manzan/scripts/create-instance.sh test01 $(id -un)
```

**Expected result**: Script prints error `already exists` and exits non-zero. Existing files are untouched.

**Pass / Fail**: ___

---

**TC-10 — New instance is seeded from default template when default exists**

**Preconditions**: `/QOpenSys/etc/manzan-default/app.ini` exists and is non-empty.

**Steps**:
```bash
/opt/manzan/scripts/create-instance.sh seeded-test $(id -un)
cat /QOpenSys/etc/manzan-seeded-test/app.ini
```

**Expected result**: `app.ini` content matches the default template. Script output says `Seeded app.ini from default instance.`

**Pass / Fail**: ___

---

**TC-11 — New instance gets empty stubs when no default exists**

**Preconditions**: `/QOpenSys/etc/manzan-default/` does **not** exist.

**Steps**:
```bash
/opt/manzan/scripts/create-instance.sh no-default-test $(id -un)
cat /QOpenSys/etc/manzan-no-default-test/app.ini
```

**Expected result**: Files exist but are empty (zero bytes). Script output says `Created empty app.ini.`

**Pass / Fail**: ___

---

### Section C — `list-instances.sh`

---

**TC-12 — Stopped instance shows STATUS = STOPPED**

**Preconditions**: Instance `test01` exists; no lock file present for it.

**Steps**:
```bash
/opt/manzan/scripts/list-instances.sh
```

**Expected result**: A table row for `test01` shows `STOPPED`, PID column shows `-`.

**Pass / Fail**: ___

---

**TC-13 — Running instance shows STATUS = RUNNING with PID**

**Preconditions**: An instance is running (see TC-16 for how to start one). You may use the `default` instance if it is configured and running.

**Steps**:
```bash
/opt/manzan/scripts/list-instances.sh
```

**Expected result**: Running instance shows `RUNNING` and a numeric PID. Stopped instances still show `STOPPED`.

**Pass / Fail**: ___

---

**TC-14 — Orphaned running instance (config dir removed) is shown**

**Preconditions**: An instance is running. While it is running, manually delete its config directory (do **not** stop the process first — this simulates an operator accident).

**Steps**:
```bash
rm -rf /QOpenSys/etc/manzan-<running-instance>
/opt/manzan/scripts/list-instances.sh
```

**Expected result**: The instance still appears in the table with status `RUNNING` but CONFIG column shows `-` (no config dir). It is not silently omitted.

**Cleanup**: Stop the orphaned process and remove its lock file before continuing.

**Pass / Fail**: ___

---

### Section D — Startup & Instance Resolution

---

**TC-15 — Default instance starts when no flag is given**

**Preconditions**: Default instance is configured (valid `app.ini` in `/QOpenSys/etc/manzan-default/`).

**Steps**:
```bash
java -jar /opt/manzan/manzan.jar &
sleep 3
cat /var/run/manzan/manzan-default.lock
```

**Expected result**:
- Startup message includes `Starting instance: default`.
- Lock file `/var/run/manzan/manzan-default.lock` exists and contains a PID.

**Cleanup**: `kill $(cat /var/run/manzan/manzan-default.lock)`

**Pass / Fail**: ___

---

**TC-16 — Named instance starts with `--instance=` flag**

**Preconditions**: Instance `test01` is configured (valid `app.ini`). Default instance is not running.

**Steps**:
```bash
java -jar /opt/manzan/manzan.jar --instance=test01 &
sleep 3
cat /var/run/manzan/manzan-test01.lock
```

**Expected result**:
- Startup message includes `Starting instance: test01`.
- Lock file `/var/run/manzan/manzan-test01.lock` contains a PID.

**Cleanup**: `kill $(cat /var/run/manzan/manzan-test01.lock)`

**Pass / Fail**: ___

---

**TC-17 — `MANZAN_INSTANCE` environment variable is honoured**

**Preconditions**: Instance `test01` is configured and not running.

**Steps**:
```bash
MANZAN_INSTANCE=test01 java -jar /opt/manzan/manzan.jar &
sleep 3
cat /var/run/manzan/manzan-test01.lock
```

**Expected result**: Startup message includes `Starting instance: test01`. Lock file exists.

**Cleanup**: Kill the process.

**Pass / Fail**: ___

---

**TC-18 — `--instance=` flag takes precedence over `MANZAN_INSTANCE`**

**Preconditions**: Instance `test01` and `seeded-test` both have valid `app.ini` and neither is running.

**Steps**:
```bash
MANZAN_INSTANCE=seeded-test java -jar /opt/manzan/manzan.jar --instance=test01 &
sleep 3
```

**Expected result**: Startup message says `Starting instance: test01` (the flag wins over the env var). Lock file for `test01` exists; no lock file for `seeded-test`.

**Cleanup**: Kill the process.

**Pass / Fail**: ___

---

**TC-19 — Starting an already-running instance is rejected**

**Preconditions**: Instance `test01` is currently running (TC-16 left it up).

**Steps**:
```bash
java -jar /opt/manzan/manzan.jar --instance=test01
# This should exit immediately, do not background it.
```

**Expected result**: Prints `Instance 'test01' is already running. Exiting.` and exits with a non-zero code. The original process continues running undisturbed.

**Verify**: `ls -l /var/run/manzan/manzan-test01.lock` — PID unchanged from TC-16.

**Pass / Fail**: ___

---

**TC-20 — Stale lock file from a dead process is cleared on next start**

**Preconditions**: Instance `test01` is stopped. Manually plant a stale lock file with a non-existent PID:
```bash
echo "99999999" > /var/run/manzan/manzan-test01.lock
# Verify that PID 99999999 does not exist:
ls /proc/99999999   # Should say "No such file or directory"
```

**Steps**:
```bash
java -jar /opt/manzan/manzan.jar --instance=test01 &
sleep 3
```

**Expected result**: Manzan detects the stale lock (PID not in `/proc`), starts normally, and overwrites the lock file with the real PID.

**Cleanup**: Kill the process.

**Pass / Fail**: ___

---

### Section E — Two Instances Running Concurrently

---

**TC-21 — Two instances start simultaneously without conflict**

**Preconditions**: `test01` and `seeded-test` both have valid `app.ini`. Neither is running.

**Steps**:
```bash
java -jar /opt/manzan/manzan.jar --instance=test01 &
java -jar /opt/manzan/manzan.jar --instance=seeded-test &
sleep 5
/opt/manzan/scripts/list-instances.sh
```

**Expected result**:
- Both `test01` and `seeded-test` show `RUNNING` with distinct PIDs.
- No error in either process's stdout/stderr.

**Cleanup**: Kill both processes.

**Pass / Fail**: ___

---

**TC-22 — Session IDs for two instances are distinct**

**Preconditions**: TC-21 — both instances are running.

**Steps**: Check the startup output (or the session IDs in the lock files) of both processes. Alternatively, after they have run long enough to write at least one event each, run:

```sql
SELECT DISTINCT SESSION_ID FROM MANZAN.MANZANMSG ORDER BY SESSION_ID;
```

**Expected result**: The two instances have different 10-character session IDs (e.g. `TEST010001` and `SEEDED0001`). No two rows from different instances share a session ID.

**Pass / Fail**: ___

---

**TC-23 — Stopping one instance does not affect the other**

**Preconditions**: TC-21 — both instances running.

**Steps**:
```bash
kill $(cat /var/run/manzan/manzan-test01.lock)
sleep 2
/opt/manzan/scripts/list-instances.sh
```

**Expected result**: `test01` shows `STOPPED`; `seeded-test` still shows `RUNNING` with the same PID as before.

**Cleanup**: Kill `seeded-test` as well.

**Pass / Fail**: ___

---

### Section F — `remove-instance.sh`

---

**TC-24 — Removing a stopped instance succeeds**

**Preconditions**: Instance `no-default-test` is stopped (created in TC-11; should not be running).

**Steps**:
```bash
/opt/manzan/scripts/remove-instance.sh no-default-test
ls /QOpenSys/etc/manzan-no-default-test   # Should fail
```

**Expected result**: Script prints `Instance 'no-default-test' has been removed.` Config directory no longer exists.

**Pass / Fail**: ___

---

**TC-25 — Removing a running instance is refused**

**Preconditions**: `test01` is running.

**Steps**:
```bash
/opt/manzan/scripts/remove-instance.sh test01
```

**Expected result**: Script prints `Error: instance 'test01' is currently running` and exits non-zero. Config directory still exists and the running process is unaffected.

**Pass / Fail**: ___

---

**TC-26 — `--clean-db` flag removes database rows for the instance**

**Preconditions**: `seeded-test` has been running long enough to have written at least one DB row. It is now stopped.

**Steps**:
```bash
# Confirm rows exist before removal
db2 "SELECT COUNT(*) FROM MANZAN.MANZANMSG WHERE SESSION_ID = 'SEEDED0001'"

# Remove with --clean-db
/opt/manzan/scripts/remove-instance.sh seeded-test --clean-db

# Confirm rows are gone
db2 "SELECT COUNT(*) FROM MANZAN.MANZANMSG WHERE SESSION_ID = 'SEEDED0001'"
```

*(Replace `SEEDED0001` with the actual session ID observed in TC-22.)*

**Expected result**: Row count drops to 0 after removal. Script reports `Database rows removed.` and `Instance 'seeded-test' has been removed.`

**Pass / Fail**: ___

---

**TC-27 — Stale lock file is cleaned up by `remove-instance.sh`**

**Preconditions**: Instance `test01` is stopped. Manually plant a stale lock file:
```bash
echo "99999999" > /var/run/manzan/manzan-test01.lock
```

**Steps**:
```bash
/opt/manzan/scripts/remove-instance.sh test01
```

**Expected result**: Script detects stale lock (dead PID), prints `Removed stale lock file for instance 'test01'.`, then removes the config directory normally.

**Pass / Fail**: ___

---

### Section G — Database Migration

---

**TC-28 — `AUDJRNTS` table has `SESSION_ID` column after migration**

**Preconditions**: `make all` (or `make migrate`) has been run in the `ile/` directory.

**Steps**:
```sql
SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, COLUMN_DEFAULT, IS_NULLABLE
FROM QSYS2.SYSCOLUMNS
WHERE TABLE_SCHEMA = 'MANZAN'
  AND TABLE_NAME   = 'AUDJRNTS'
  AND COLUMN_NAME  = 'SESSION_ID';
```

**Expected result**: One row returned. `DATA_TYPE = 'VARCHAR'`, `CHARACTER_MAXIMUM_LENGTH = 10`, `COLUMN_DEFAULT = 'LEGACY'`, `IS_NULLABLE = 'NO'`.

**Pass / Fail**: ___

---

**TC-29 — Existing `AUDJRNTS` rows are preserved with `SESSION_ID = 'LEGACY'`**

**Preconditions**: At least one row existed in `AUDJRNTS` before the migration ran.

**Steps**:
```sql
SELECT COUNT(*) FROM MANZAN.AUDJRNTS WHERE SESSION_ID = 'LEGACY';
```

**Expected result**: Row count equals the number of rows that existed before the upgrade (no data loss). No rows have a `NULL` session ID.

**Pass / Fail**: ___

---

**TC-30 — Compound index exists on `AUDJRNTS`**

**Steps**:
```sql
SELECT INDEX_NAME, COLUMN_NAMES
FROM QSYS2.SYSINDEXES
WHERE TABLE_SCHEMA = 'MANZAN'
  AND TABLE_NAME   = 'AUDJRNTS'
  AND INDEX_NAME   = 'AUDJRNTS_SESSION_IDX';
```

**Expected result**: One row returned with `SESSION_ID` among the indexed columns.

**Pass / Fail**: ___

---

**TC-31 — Compound indexes exist on all four event tables**

**Steps**:
```sql
SELECT TABLE_NAME, INDEX_NAME
FROM QSYS2.SYSINDEXES
WHERE TABLE_SCHEMA = 'MANZAN'
  AND INDEX_NAME LIKE '%SESSION_IDX%'
ORDER BY TABLE_NAME;
```

**Expected result**: Four rows — one each for `MANZANMSG`, `MANZANOTH`, `MANZANPAL`, `MANZANVLOG`.

**Pass / Fail**: ___

---

**TC-32 — Migration SQL is idempotent (safe to re-run)**

**Steps**:
```bash
# Re-run the migration from the ile/ directory
make migrate
```

**Expected result**: `make` completes with exit code 0. No `SQL0601` (object already exists) or similar errors. Existing data is unchanged.

**Pass / Fail**: ___

---

### Section H — `CLEANUP_INSTANCE_DATA` Stored Procedure

---

**TC-33 — `CLEANUP_INSTANCE_DATA` removes only the target instance's rows**

**Preconditions**: Both `test01` (session ID `TEST010001`) and `seeded-test` (`SEEDED0001`) have written rows. Both are now stopped.

**Steps**:
```sql
-- Record row counts before cleanup
SELECT 'MANZANMSG' AS TBL, SESSION_ID, COUNT(*) AS N
FROM MANZAN.MANZANMSG GROUP BY SESSION_ID
UNION ALL
SELECT 'MANZANOTH', SESSION_ID, COUNT(*) FROM MANZAN.MANZANOTH GROUP BY SESSION_ID;

-- Run cleanup for test01 only
CALL MANZAN.CLEANUP_INSTANCE_DATA('TEST010001');

-- Verify after cleanup
SELECT 'MANZANMSG' AS TBL, SESSION_ID, COUNT(*) AS N
FROM MANZAN.MANZANMSG GROUP BY SESSION_ID
UNION ALL
SELECT 'MANZANOTH', SESSION_ID, COUNT(*) FROM MANZAN.MANZANOTH GROUP BY SESSION_ID;
```

**Expected result**: All rows for `TEST010001` are gone across all five tables. Rows for `SEEDED0001` are untouched.

**Pass / Fail**: ___

---

**TC-34 — `CLEANUP_INSTANCE_DATA` is transactional (all or nothing)**

This test is informational / best-effort. It verifies that the procedure runs in a single transaction. If you have a test tool that can inject a mid-procedure failure (e.g. revoke table authority mid-call), verify that no partial deletions occur. Otherwise, document this as verified by code review of [`ile/install_tasks/cleanup_procedures.sql`](../../ile/install_tasks/cleanup_procedures.sql) (it uses a single `BEGIN ... END` block with no explicit commits).

**Pass / Fail (code review)**: ___

---

### Section I — Security & Permissions

---

**TC-35 — A second user cannot read another user's config directory**

**Preconditions**: `test01` config directory is owned by `USERA`. You have a second shell session logged in as `USERB`.

**Steps** (as `USERB`):
```bash
ls /QOpenSys/etc/manzan-test01/
cat /QOpenSys/etc/manzan-test01/app.ini
```

**Expected result**: Both commands fail with `Permission denied`. The 700 directory permission enforces owner-only access.

**Pass / Fail**: ___

---

**TC-36 — A second user cannot read another user's config file**

**Preconditions**: Same as TC-35; `app.ini` is mode 600.

**Steps** (as `USERB`):
```bash
# Try direct path if directory traversal somehow succeeded
cat /QOpenSys/etc/manzan-test01/app.ini
```

**Expected result**: `Permission denied`. Mode 600 enforces owner-only read/write.

**Pass / Fail**: ___

---

**TC-37 — `--version` works without a lock file or running instance**

**Steps**:
```bash
java -jar /opt/manzan/manzan.jar --version
```

**Expected result**: Version output printed, process exits cleanly (exit code 0). No lock file is created.

**Verify**: `ls /var/run/manzan/` should not have gained any new file.

**Pass / Fail**: ___

---

## 5. Regression Tests

These tests verify that the existing single-instance behaviour still works unchanged.

---

**TC-R01 — `--version` still works (no regression)**

*(See TC-37 — already covers this.)*

---

**TC-R02 — `--configdir=` legacy argument is still honoured**

**Steps**:
```bash
mkdir -p /tmp/manzan-legacy-test
cp /QOpenSys/etc/manzan-default/app.ini /tmp/manzan-legacy-test/
java -jar /opt/manzan/manzan.jar --configdir=/tmp/manzan-legacy-test &
sleep 3
```

**Expected result**: Process starts, reads config from `/tmp/manzan-legacy-test/`, and acquires a lock for the `default` instance (since no `--instance` was given).

**Cleanup**: Kill the process; remove `/tmp/manzan-legacy-test`.

**Pass / Fail**: ___

---

**TC-R03 — Existing database rows with `SESSION_ID = 'LEGACY'` are not touched by new instances**

**Steps**:
```sql
-- Any new rows inserted by the 'default' instance after migration
-- should NOT have SESSION_ID = 'LEGACY'
SELECT SESSION_ID, COUNT(*)
FROM MANZAN.AUDJRNTS
GROUP BY SESSION_ID;
```

**Expected result**: Rows written by the new `default` instance have a generated session ID (e.g. `DEFAUL0001`), not `'LEGACY'`. Pre-migration rows with `'LEGACY'` remain untouched.

**Pass / Fail**: ___

---

## 6. Test Execution Checklist

| TC | Title | Result | Tester | Date |
|---|---|---|---|---|
| TC-01 | Valid instance name | | | |
| TC-02 | Hyphens and underscores | | | |
| TC-03 | Uppercase rejected | | | |
| TC-04 | Name too long rejected | | | |
| TC-05 | Empty name rejected | | | |
| TC-06 | Path traversal rejected | | | |
| TC-07 | Directory permissions 700 | | | |
| TC-08 | File permissions 600 | | | |
| TC-09 | Duplicate instance rejected | | | |
| TC-10 | Template seeded from default | | | |
| TC-11 | Empty stubs without default | | | |
| TC-12 | STOPPED status in list | | | |
| TC-13 | RUNNING status with PID | | | |
| TC-14 | Orphaned running instance shown | | | |
| TC-15 | Default instance on bare start | | | |
| TC-16 | Named instance with `--instance=` | | | |
| TC-17 | `MANZAN_INSTANCE` env var | | | |
| TC-18 | `--instance=` overrides env var | | | |
| TC-19 | Duplicate start rejected | | | |
| TC-20 | Stale lock cleared on start | | | |
| TC-21 | Two instances run concurrently | | | |
| TC-22 | Session IDs are distinct | | | |
| TC-23 | Stopping one does not affect other | | | |
| TC-24 | Remove stopped instance | | | |
| TC-25 | Remove running instance refused | | | |
| TC-26 | `--clean-db` removes DB rows | | | |
| TC-27 | Stale lock cleaned by remove | | | |
| TC-28 | `AUDJRNTS.SESSION_ID` column exists | | | |
| TC-29 | Existing rows preserved as LEGACY | | | |
| TC-30 | `AUDJRNTS` index exists | | | |
| TC-31 | All four event table indexes exist | | | |
| TC-32 | Migration is idempotent | | | |
| TC-33 | Cleanup isolates to target session | | | |
| TC-34 | Cleanup is transactional (review) | | | |
| TC-35 | Second user denied directory | | | |
| TC-36 | Second user denied config file | | | |
| TC-37 | `--version` no lock created | | | |
| TC-R01 | `--version` regression | | | |
| TC-R02 | `--configdir=` regression | | | |
| TC-R03 | LEGACY rows not overwritten | | | |

---

## 7. Known Limitations & Notes

- **IBM i only**: All tests must be executed on IBM i (PASE). The lock-file mechanism uses `/proc/<pid>` for liveness detection, which does not exist on Windows or Linux.
- **Java 8 only**: The build targets Java 8. Do not run with a non-IBM JDK on IBM i (the startup code checks the vendor and refuses).
- **Root / QSECOFR not required**: Tests can be run as a regular user profile, provided that profile owns the relevant config directories.
- **Port conflicts**: If any destination (e.g. HTTP endpoint) uses a fixed port, two instances cannot both bind to the same port simultaneously. This is a destination-level concern, not tested here.
- **Test isolation**: After each test section, stop any running instances and confirm the lock directory is clean before starting the next section:
  ```bash
  ls /var/run/manzan/       # should be empty or contain only expected locks
  /opt/manzan/scripts/list-instances.sh
  ```

---

## 8. Defect Reporting

When logging a defect, include:

1. TC number and title
2. Exact command(s) executed (copy/paste from shell)
3. Full stdout/stderr output
4. Output of `/opt/manzan/scripts/list-instances.sh` at the time of failure
5. Contents of `/var/run/manzan/` at the time of failure
6. IBM i OS version (`DSPSYSVAL SYSVAL(QOSLEVEL)`)
7. Java version (`java -version`)

---

*Document Version: 1.0 — covers Stages 1–6 of the multi-instance migration.*
