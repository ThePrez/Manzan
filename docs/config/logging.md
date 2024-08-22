## Logging
Manzan has three options for available logging levels which can be configured via the `MANZAN_DEBUG_LEVEL` system environment variable.

MANZAN_DEBUG_LEVEL=0 => Don't log any messages
MANZAN_DEBUG_LEVEL=1 => Log only errors
MANZAN_DEBUG_LEVEL=2 => Log errors and warnings
MANZAN_DEBUG_LEVEL=3 => Log errors, warnings, and info

Logs will be written to `/tmp/manzan_debug.txt`.

The `MANZAN_DEBUG_LEVEL` environment variable can be set with ADDENVVAR command. Ex. `ADDENVVAR ENVVAR(MANZAN_DEBUG_LEVEL) VALUE(3) LEVEL(*SYS) REPLACE(*YES)`. After which you will need to run `ENDTCPSVR *SSHD` and `STRTCPSVR *SSHD`. Then restart your ssh session for the new environment variable to take effect.