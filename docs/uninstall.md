# Uninstall

This document explains how to uninstall Manzan from IBM i.

## Important Note

The `uninstall` command in the Makefile is **only for developers** who are building Manzan from source. It is **NOT** intended for users who installed Manzan using the `.jar` installer file.

## Uninstalling Manzan (Installed via .jar)

If you installed Manzan using the `manzan-installer-v*.jar` file, follow these steps to uninstall:

### Step 1: Stop Manzan

First, stop the Manzan service if it's running:

```sh
# If using Service Commander
sc stop manzan

# Or kill the process manually if started without Service Commander
ps -ef | grep manzan.jar | grep -v grep | awk '{print $2}' | xargs kill
```

### Step 2: Remove Manzan Files

Remove the Manzan installation directories:

```sh
# Remove the main installation directory
rm -rf /opt/manzan

# Remove the configuration directory
rm -rf /QOpenSys/etc/manzan

# Remove the Service Commander link (if it exists)
rm -f /QOpenSys/etc/sc/services/manzan.yaml
```

### Step 3: Remove the Library

Delete the Manzan library from IBM i:

```sh
system "DLTLIB MANZAN"
```

Or from a 5250 session:

```
DLTLIB MANZAN
```

### Step 4: Verify Uninstallation

Verify that Manzan has been completely removed:

```sh
# Check if directories are gone
ls -la /opt/manzan 2>&1
ls -la /QOpenSys/etc/manzan 2>&1

# Check if library is gone
system "DSPLIB MANZAN"
```

All of these commands should indicate that the files/library no longer exist.

## Troubleshooting

### "Library MANZAN cannot be allocated" Error

If you see the error `CPF2113: Library MANZAN can not be allocated` during installation, this typically means:

1. **The library is locked** - Another process or job is using the MANZAN library
2. **Previous installation is still running** - Manzan is still running from a previous installation

**Solution:**

1. Stop all Manzan processes:
   ```sh
   sc stop manzan
   # Or
   ps -ef | grep manzan.jar | grep -v grep | awk '{print $2}' | xargs kill
   ```

2. Check for jobs using the library:
   ```sh
   system "WRKACTJOB JOB(MANZAN)"
   ```

3. End any jobs that are using the library, then try the installation again.

### Reinstalling After Failed Installation

If an installation failed partway through:

1. Follow the complete uninstall steps above
2. Verify all files and the library are removed
3. Run the installer again:
   ```sh
   java -jar manzan-installer-v0.0.15.jar
   ```

## For Developers Only

If you are building Manzan from source using the Makefile, you can use:

```sh
gmake uninstall
```

This command:
- Deletes the MANZAN library (or custom library specified by `BUILDLIB`)
- Removes files from `/opt/manzan` and `/QOpenSys/etc/manzan`

**Note:** This is only applicable when building from source, not for installations done via the `.jar` installer.