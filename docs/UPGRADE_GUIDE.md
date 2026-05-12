# Manzan Upgrade Guide

This guide explains how to upgrade an existing Manzan installation to a new release, and how to create installer packages for distribution.

---

## Table of Contents

1. [Creating an Installer](#creating-an-installer)
2. [Upgrading Manzan](#upgrading-manzan)
3. [Rollback Procedures](#rollback-procedure)
4. [Troubleshooting](#troubleshooting-upgrades)

---

## Creating an Installer

Before you can upgrade, you need to create an installer package for the new version.

### Prerequisites for Building

**On IBM i System:**

```bash
# Install required packages
yum install git gmake maven zip wget gcc-cplusplus

# Verify installations
gmake --version
mvn --version
java -version
```

### Quick Build Process

```bash
# 1. Clone or update repository
cd ~
git clone https://github.com/ThePrez/Manzan.git
cd Manzan

# Or update existing clone
git pull origin main

# 2. Checkout the version you want to build
git checkout v2.0.0

# 3. Build the installer
gmake manzan-installer-v2.0.0.jar BUILDLIB=MANZAN
```

This creates: `manzan-installer-v2.0.0.jar`

### Detailed Build Steps

#### Step 1: Prepare Build Environment

```bash
# Set environment variables
export BUILDLIB=MANZAN
export VERSION=2.0.0

# Ensure clean environment
system "dltlib ${BUILDLIB}" || echo "Library doesn't exist"
rm -fr /QOpenSys/etc/manzan
rm -fr /opt/manzan
```

#### Step 2: Build the Installer

```bash
# Build installer with version number
gmake manzan-installer-v${VERSION}.jar BUILDLIB=${BUILDLIB}
```

The build process:
1. Downloads AppInstall tool
2. Creates build library
3. Compiles ILE components (C++ programs)
4. Builds Java components (Maven)
5. Packages everything into installer JAR

#### Step 3: Verify the Build

```bash
# Check installer was created
ls -lh manzan-installer-v${VERSION}.jar

# Verify it's a valid JAR
file manzan-installer-v${VERSION}.jar

# Check size (should be ~50-100MB)
du -h manzan-installer-v${VERSION}.jar
```

#### Step 4: Test the Installer

```bash
# Test on clean system or test library
export TEST_BUILDLIB=MANZANTEST

# Clean test environment
system "dltlib ${TEST_BUILDLIB}" || echo "OK"
rm -rf /tmp/manzan-test

# Install to test location
java -jar manzan-installer-v${VERSION}.jar

# Verify installation
/opt/manzan/bin/manzan --version
```

#### Step 5: Create Distribution Package

```bash
# Create checksum
sha256sum manzan-installer-v${VERSION}.jar > manzan-installer-v${VERSION}.jar.sha256

# Create release notes file
cat > RELEASE_NOTES_v${VERSION}.md << EOF
# Manzan v${VERSION} Release Notes

## New Features
- Prometheus metrics exporter
- Enhanced security options

## Bug Fixes
- Fixed memory leak in event processing

## Upgrade Instructions
See UPGRADE_GUIDE.md

## Installation
\`\`\`bash
java -jar manzan-installer-v${VERSION}.jar
\`\`\`
EOF

# Package for distribution
tar czf manzan-v${VERSION}-release.tar.gz \
  manzan-installer-v${VERSION}.jar \
  manzan-installer-v${VERSION}.jar.sha256 \
  RELEASE_NOTES_v${VERSION}.md \
  README.md \
  LICENSE
```

### Build from Development Machine

If building from a non-IBM i system:

```bash
# Set environment variables
export USER=myuser
export HOST=ibmi.example.com

# Create .env file
cat > .env << EOF
USER=${USER}
HOST=${HOST}
EOF

# Deploy and build remotely
./scripts/buildAll.sh

# Or manually
rsync -avz --exclude camel/target -e ssh . ${USER}@${HOST}:~/manzan-build
ssh ${USER}@${HOST} "cd ~/manzan-build && gmake manzan-installer-v2.0.0.jar"

# Download installer
scp ${USER}@${HOST}:~/manzan-build/manzan-installer-v2.0.0.jar .
```

### Troubleshooting Build Issues

**Issue: Maven build fails**
```bash
# Solution: Clean Maven cache
rm -rf ~/.m2/repository
gmake -C camel clean
gmake manzan-installer-v${VERSION}.jar
```

**Issue: ILE compilation fails**
```bash
# Solution: Check compiler is available
system "DSPSFWRSC"

# Verify C++ compiler installed
ls -la /QOpenSys/usr/bin/g++
```

**Issue: AppInstall download fails**
```bash
# Solution: Download manually
wget -O appinstall.jar \
  https://github.com/ThePrez/AppInstall-IBMi/releases/download/v0.0.6/appinstall-v0.0.6.jar
```

---

## Upgrading Manzan

Once you have the installer, follow these steps to upgrade.

### Overview

Upgrading Manzan involves:
1. Backing up current configuration
2. Stopping the running service
3. Installing the new version
4. Restoring configuration
5. Restarting the service
6. Verifying the upgrade

---

## Pre-Upgrade Checklist

Before upgrading, ensure you have:

- [ ] Current Manzan version documented
- [ ] Backup of configuration files
- [ ] Backup of custom modifications
- [ ] Maintenance window scheduled
- [ ] Rollback plan prepared
- [ ] New installer JAR downloaded
- [ ] Release notes reviewed

---

## Quick Upgrade Process

```bash
# 1. Backup configuration
cp -r /QOpenSys/etc/manzan /QOpenSys/etc/manzan.backup

# 2. Stop Manzan
sc stop manzan

# 3. Install new version
java -jar manzan-installer-v2.0.0.jar

# 4. Restore configuration (if needed)
# Configuration is preserved by default

# 5. Start Manzan
sc start manzan

# 6. Verify
sc status manzan
```

---

## Detailed Upgrade Steps

### Step 1: Check Current Version

```bash
# Check installed version
/opt/manzan/bin/manzan --version

# Or check via Service Commander
sc status manzan
```

Expected output:
```
Distributor version information:
-------------------------------------------
    Version: 1.0.0
    Build date (UTC): Mon Jan 15 10:30:00 2024

ILE Handler version information:
-------------------------------------------
    Version: 1.0.0
    Build date: Mon Jan 15 10:25:00 2024 UTC
```

### Step 2: Review Release Notes

Before upgrading, review the release notes for:
- New features
- Breaking changes
- Configuration changes
- Deprecated features
- Known issues

Download from: https://github.com/ThePrez/Manzan/releases

### Step 3: Backup Current Installation

#### Backup Configuration Files

```bash
# Create backup directory
mkdir -p /tmp/manzan-backup-$(date +%Y%m%d)
cd /tmp/manzan-backup-$(date +%Y%m%d)

# Backup configuration
cp -r /QOpenSys/etc/manzan ./config-backup

# Backup custom scripts (if any)
cp -r /opt/manzan/bin ./bin-backup

# Create backup archive
tar czf manzan-backup-$(date +%Y%m%d-%H%M%S).tar.gz \
  config-backup bin-backup

# Verify backup
tar tzf manzan-backup-*.tar.gz
```

#### Backup ILE Library

```bash
# Save library to save file
system "SAVLIB LIB(MANZAN) DEV(*SAVF) SAVF(QGPL/MANZANBKP) TGTRLS(*CURRENT)"

# Or export to IFS
system "SAVLIB LIB(MANZAN) DEV(*SAVF) SAVF(QGPL/MANZANBKP)"
system "CPYTOSTMF FROMMBR('/QSYS.LIB/QGPL.LIB/MANZANBKP.FILE') TOSTMF('/tmp/manzan-lib-backup.savf') STMFOPT(*REPLACE)"
```

#### Document Current Configuration

```bash
# Save current configuration details
cat > /tmp/manzan-pre-upgrade-info.txt << EOF
Upgrade Date: $(date)
Current Version: $(/opt/manzan/bin/manzan --version | grep Version)
Configuration Files:
$(ls -la /QOpenSys/etc/manzan/)

Active Data Sources:
$(grep -E '^\[' /QOpenSys/etc/manzan/data.ini)

Active Destinations:
$(grep -E '^\[' /QOpenSys/etc/manzan/dests.ini)

Service Status:
$(sc status manzan)
EOF

cat /tmp/manzan-pre-upgrade-info.txt
```

### Step 4: Stop Manzan Service

```bash
# Stop via Service Commander
sc stop manzan

# Verify it's stopped
sc status manzan

# Alternative: Stop manually if needed
pkill -f manzan.jar

# Verify no processes running
ps aux | grep manzan | grep -v grep
```

### Step 5: Install New Version

#### Option A: Standard Upgrade (Preserves Configuration)

```bash
# Download new installer
cd /tmp
wget https://github.com/ThePrez/Manzan/releases/download/v2.0.0/manzan-installer-v2.0.0.jar

# Or transfer from local machine
scp manzan-installer-v2.0.0.jar user@ibmi:/tmp/

# Install (configuration is preserved by default)
java -jar manzan-installer-v2.0.0.jar
```

The installer will:
- Update ILE programs in MANZAN library
- Replace Java JAR file
- Update startup scripts
- **Preserve existing configuration files**
- Update Service Commander definition

#### Option B: Clean Install (Fresh Configuration)

```bash
# Remove existing configuration
rm -rf /QOpenSys/etc/manzan

# Remove application directory
rm -rf /opt/manzan

# Delete ILE library
system "DLTLIB MANZAN"

# Install new version
java -jar manzan-installer-v2.0.0.jar

# Restore configuration from backup
cp -r /tmp/manzan-backup-*/config-backup/* /QOpenSys/etc/manzan/
```

### Step 6: Review Configuration Changes

Check if new version requires configuration updates:

```bash
# Compare old and new configuration templates
diff /tmp/manzan-backup-*/config-backup/app.ini.tpl \
     /QOpenSys/etc/manzan/app.ini.tpl

diff /tmp/manzan-backup-*/config-backup/dests.ini \
     /QOpenSys/etc/manzan/dests.ini
```

#### Update Configuration for New Features

If upgrading to version with Prometheus support:

```bash
# Edit destinations configuration
vi /QOpenSys/etc/manzan/dests.ini

# Add Prometheus destination
cat >> /QOpenSys/etc/manzan/dests.ini << EOF

[prometheus_metrics]
type=prometheus
port=9090
path=/metrics
metricPrefix=ibmi_
username=prometheus
password=your_secure_password
EOF
```

### Step 7: Verify Installation

```bash
# Check new version
/opt/manzan/bin/manzan --version

# Verify files are updated
ls -la /opt/manzan/lib/manzan.jar
stat /opt/manzan/lib/manzan.jar

# Check ILE library
system "DSPOBJD OBJ(MANZAN/HANDLER) OBJTYPE(*PGM)"

# Verify configuration preserved
ls -la /QOpenSys/etc/manzan/
cat /QOpenSys/etc/manzan/app.ini
```

### Step 8: Start Manzan Service

```bash
# Start service
sc start manzan

# Check status
sc status manzan

# Monitor logs
tail -f /opt/manzan/logs/manzan.log
```

Expected output:
```
Apache Camel version 3.14.10
[main] INFO  - Route: joblog_monitor started
[main] INFO  - Route: prometheus_metrics started
[main] INFO  - Total 2 routes started
```

### Step 9: Validate Upgrade

#### Test Basic Functionality

```bash
# Test data sources are working
# Check if events are being processed
tail -f /opt/manzan/logs/manzan.log | grep "Route:"

# Test destinations
# For Prometheus:
curl http://localhost:9090/metrics

# For file destination:
ls -la /path/to/output/files/
```

#### Verify New Features

If upgrading to version with new features:

```bash
# Test Prometheus metrics (if new in this version)
curl -u username:password http://localhost:9090/metrics | head -20

# Verify metrics are being generated
curl -s http://localhost:9090/metrics | grep "ibmi_events_total"
```

#### Check for Errors

```bash
# Check for errors in logs
grep -i error /opt/manzan/logs/manzan.log

# Check system logs
system "DSPLOG"

# Check job logs
system "DSPJOBLOG JOB(MANZAN)"
```

### Step 10: Monitor Post-Upgrade

```bash
# Monitor for 15 minutes
watch -n 30 'sc status manzan && tail -5 /opt/manzan/logs/manzan.log'

# Check resource usage
top -p $(pgrep -f manzan.jar)

# Verify data flow
# Check that events are being processed and sent to destinations
```

---

## Upgrade Scenarios

### Scenario 1: Minor Version Upgrade (1.0.0 → 1.1.0)

Minor upgrades typically include:
- Bug fixes
- New features (backward compatible)
- Performance improvements

**Process:**
```bash
# Standard upgrade process
sc stop manzan
java -jar manzan-installer-v1.1.0.jar
sc start manzan
```

Configuration changes: Usually none required

### Scenario 2: Major Version Upgrade (1.x → 2.0.0)

Major upgrades may include:
- Breaking changes
- Configuration format changes
- Deprecated feature removal

**Process:**
```bash
# Backup everything
cp -r /QOpenSys/etc/manzan /tmp/manzan-config-backup

# Stop service
sc stop manzan

# Review migration guide
cat /tmp/MIGRATION_v1_to_v2.md

# Install new version
java -jar manzan-installer-v2.0.0.jar

# Update configuration for breaking changes
vi /QOpenSys/etc/manzan/dests.ini

# Start and test
sc start manzan
tail -f /opt/manzan/logs/manzan.log
```

### Scenario 3: Upgrade with Custom Modifications

If you have custom modifications:

```bash
# Document custom changes
diff -r /opt/manzan.original /opt/manzan > /tmp/custom-changes.diff

# Backup custom files
cp /opt/manzan/bin/custom-script.sh /tmp/

# Perform upgrade
sc stop manzan
java -jar manzan-installer-v2.0.0.jar

# Reapply custom changes
cp /tmp/custom-script.sh /opt/manzan/bin/
chmod +x /opt/manzan/bin/custom-script.sh

# Start service
sc start manzan
```

---

## Rollback Procedure

If upgrade fails or causes issues:

### Quick Rollback

```bash
# Stop new version
sc stop manzan

# Restore ILE library
system "DLTLIB MANZAN"
system "RSTLIB SAVLIB(MANZAN) DEV(*SAVF) SAVF(QGPL/MANZANBKP)"

# Restore configuration
rm -rf /QOpenSys/etc/manzan
cp -r /tmp/manzan-backup-*/config-backup /QOpenSys/etc/manzan

# Restore application files
rm -rf /opt/manzan
cp -r /tmp/manzan-backup-*/bin-backup /opt/manzan/bin

# Start old version
sc start manzan
```

### Detailed Rollback

```bash
# 1. Stop service
sc stop manzan
pkill -f manzan.jar

# 2. Remove new version
system "DLTLIB MANZAN"
rm -rf /opt/manzan
rm -rf /QOpenSys/etc/manzan

# 3. Restore from backup archive
cd /tmp
tar xzf manzan-backup-*.tar.gz

# 4. Restore ILE library
system "RSTLIB SAVLIB(MANZAN) DEV(*SAVF) SAVF(QGPL/MANZANBKP)"

# 5. Restore configuration
cp -r config-backup /QOpenSys/etc/manzan

# 6. Restore application
mkdir -p /opt/manzan
cp -r bin-backup/* /opt/manzan/bin/

# 7. Restore JAR (from previous backup)
cp /tmp/manzan-lib-backup/manzan.jar /opt/manzan/lib/

# 8. Start service
sc start manzan

# 9. Verify rollback
/opt/manzan/bin/manzan --version
sc status manzan
```

---

## Troubleshooting Upgrades

### Issue: Configuration Lost After Upgrade

**Symptoms:**
```
Error: Cannot find configuration file
```

**Solution:**
```bash
# Restore from backup
cp -r /tmp/manzan-backup-*/config-backup/* /QOpenSys/etc/manzan/

# Verify permissions
chmod 600 /QOpenSys/etc/manzan/*.ini
chown qsys:0 /QOpenSys/etc/manzan/*.ini
```

### Issue: Service Won't Start After Upgrade

**Symptoms:**
```
sc start manzan
Error: Service failed to start
```

**Diagnosis:**
```bash
# Check logs
tail -50 /opt/manzan/logs/manzan.log

# Check for Java errors
/opt/manzan/bin/manzan --version

# Verify JAR file
ls -la /opt/manzan/lib/manzan.jar
file /opt/manzan/lib/manzan.jar
```

**Solutions:**
1. Check Java version compatibility
2. Verify file permissions
3. Check for configuration errors
4. Review system logs

### Issue: New Features Not Working

**Symptoms:**
```
Prometheus endpoint returns 404
```

**Solution:**
```bash
# Verify new version installed
/opt/manzan/bin/manzan --version

# Check if new destination configured
cat /QOpenSys/etc/manzan/dests.ini | grep prometheus

# Add missing configuration
vi /QOpenSys/etc/manzan/dests.ini

# Restart service
sc restart manzan
```

### Issue: Performance Degradation After Upgrade

**Diagnosis:**
```bash
# Check resource usage
top -p $(pgrep -f manzan.jar)

# Check for memory leaks
jstat -gc $(pgrep -f manzan.jar) 1000

# Review logs for errors
grep -i "error\|exception" /opt/manzan/logs/manzan.log
```

**Solutions:**
1. Increase JVM heap size
2. Review new configuration options
3. Check for known issues in release notes
4. Consider rollback if critical

---

## Best Practices

### Before Upgrade

1. **Test in Non-Production**
   - Install new version on test system first
   - Validate all functionality
   - Document any issues

2. **Schedule Maintenance Window**
   - Plan for downtime
   - Notify stakeholders
   - Have rollback plan ready

3. **Document Everything**
   - Current configuration
   - Custom modifications
   - Integration points

### During Upgrade

1. **Follow Process**
   - Don't skip backup steps
   - Verify each step completes
   - Monitor for errors

2. **Keep Backups**
   - Don't delete backups until upgrade validated
   - Keep multiple backup copies
   - Store backups off-system

### After Upgrade

1. **Validate Thoroughly**
   - Test all data sources
   - Verify all destinations
   - Check new features

2. **Monitor Closely**
   - Watch logs for errors
   - Monitor resource usage
   - Track performance metrics

3. **Document Changes**
   - Update documentation
   - Note any issues encountered
   - Share lessons learned

---

## Upgrade Checklist

### Pre-Upgrade
- [ ] Review release notes
- [ ] Backup configuration files
- [ ] Backup ILE library
- [ ] Document current version
- [ ] Test in non-production
- [ ] Schedule maintenance window
- [ ] Prepare rollback plan

### During Upgrade
- [ ] Stop Manzan service
- [ ] Verify service stopped
- [ ] Install new version
- [ ] Verify installation
- [ ] Review configuration changes
- [ ] Update configuration if needed

### Post-Upgrade
- [ ] Start Manzan service
- [ ] Verify service started
- [ ] Check version number
- [ ] Test data sources
- [ ] Test destinations
- [ ] Verify new features
- [ ] Monitor for errors
- [ ] Update documentation

### Rollback (If Needed)
- [ ] Stop service
- [ ] Restore ILE library
- [ ] Restore configuration
- [ ] Restore application files
- [ ] Start service
- [ ] Verify rollback successful
- [ ] Document issues

---

## Version-Specific Upgrade Notes

### Upgrading to v2.0.0 (Prometheus Support)

**New Features:**
- Prometheus metrics exporter
- Enhanced security options
- Performance improvements

**Configuration Changes:**
```ini
# Add to dests.ini
[prometheus_metrics]
type=prometheus
port=9090
path=/metrics
metricPrefix=ibmi_
username=prometheus
password=secure_password_here
```

**Breaking Changes:**
- None

**Deprecated Features:**
- None

---

## Support

If you encounter issues during upgrade:

1. **Check Documentation**
   - Review this upgrade guide
   - Check release notes
   - Read troubleshooting section

2. **Search Issues**
   - GitHub Issues: https://github.com/ThePrez/Manzan/issues
   - Look for similar problems

3. **Get Help**
   - Open GitHub issue with details
   - Include version numbers
   - Provide error logs
   - Describe steps taken

---

## Summary

Upgrading Manzan is straightforward:

1. **Backup** - Save configuration and library
2. **Stop** - Stop the service
3. **Install** - Run new installer
4. **Verify** - Check installation
5. **Start** - Start the service
6. **Validate** - Test functionality

The installer preserves your configuration by default, making upgrades safe and simple. Always backup before upgrading and have a rollback plan ready.