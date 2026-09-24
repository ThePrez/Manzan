package com.github.theprez.manzan;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Programmatic lifecycle API for Manzan instances.
 *
 * Intended for use by operators, AI agents, and products that need to
 * create, inspect, or retire Manzan instances without shelling out to
 * management scripts.
 *
 * All mutating operations perform the same security validation that
 * {@link InstanceContext} and {@link LockFile} enforce at runtime, so
 * it is not possible to create an instance whose name would be rejected
 * when Manzan is later started with that name.
 */
public class ManzanInstanceManager {

    private static final String CONFIG_BASE_DIR = "/QOpenSys/etc";
    private static final String CONFIG_DIR_PREFIX = "manzan-";
    private static final String[] TEMPLATE_INI_FILES = {"app.ini", "data.ini", "dests.ini"};
    private static final java.util.regex.Pattern VALID_INSTANCE_NAME =
            java.util.regex.Pattern.compile("^[a-z0-9_-]{1,32}$");

    // -------------------------------------------------------------------------
    // Public value type returned by listInstances()
    // -------------------------------------------------------------------------

    /**
     * A point-in-time snapshot of a single instance's status.
     */
    public static class InstanceInfo {
        private final String instanceName;
        private final String configDirectory;
        private final boolean running;
        private final long pid;

        private InstanceInfo(String instanceName, String configDirectory, boolean running, long pid) {
            this.instanceName = instanceName;
            this.configDirectory = configDirectory;
            this.running = running;
            this.pid = pid;
        }

        /** The validated instance name (e.g. {@code "watson-monitor"}). */
        public String getInstanceName() { return instanceName; }

        /** Absolute path to the instance config directory. */
        public String getConfigDirectory() { return configDirectory; }

        /** {@code true} if a live lock file exists for this instance. */
        public boolean isRunning() { return running; }

        /**
         * PID of the owning process, or {@code -1} if the instance is not running
         * or the PID could not be read from the lock file.
         */
        public long getPid() { return pid; }

        @Override
        public String toString() {
            return "InstanceInfo{name='" + instanceName + "'"
                    + ", configDirectory='" + configDirectory + "'"
                    + ", running=" + running
                    + (running ? ", pid=" + pid : "")
                    + "}";
        }
    }

    // -------------------------------------------------------------------------
    // createInstance
    // -------------------------------------------------------------------------

    /**
     * Create a new Manzan instance.
     *
     * Creates the config directory at {@code /QOpenSys/etc/manzan-<name>} with
     * mode 700, then seeds it with empty {@code app.ini}, {@code data.ini}, and
     * {@code dests.ini} stubs (mode 600 each).  If a config directory for the
     * default instance already exists and contains any of these files, those
     * files are copied as the starting template instead of creating empty stubs,
     * giving operators a convenient starting point.
     *
     * @param instanceName Lowercase alphanumeric name (1–32 chars, hyphens and
     *                     underscores allowed). Validated against the same rules
     *                     enforced by {@link InstanceContext}.
     * @return The {@link InstanceContext} for the newly created instance.
     * @throws SecurityException if the instance name is invalid or the config
     *                           directory is already owned by another user.
     * @throws IOException       if the directory or seed files cannot be created.
     * @throws IllegalStateException if the instance already exists.
     */
    public static InstanceContext createInstance(String instanceName) throws IOException {
        // Validate the raw name before anything else — fromEnvironment() normalises
        // to lowercase via parseInstanceParameter, so it would silently accept
        // "UPPERCASE"; the programmatic API must enforce the contract explicitly.
        if (instanceName == null || !VALID_INSTANCE_NAME.matcher(instanceName).matches()) {
            throw new SecurityException(
                    "Invalid instance name: '" + instanceName + "'. "
                    + "Names must be 1-32 characters: lowercase letters, digits, hyphens, underscores.");
        }

        // Check existence before constructing InstanceContext — fromEnvironment()
        // calls ensureConfigDirectorySecurity() which creates the directory as a
        // side effect, so checking after construction would always see it present.
        File configDir = new File("/QOpenSys/etc/manzan-" + instanceName);
        if (configDir.exists()) {
            throw new IllegalStateException(
                    "Instance '" + instanceName + "' already exists at " + configDir);
        }

        InstanceContext ctx = InstanceContext.fromEnvironment(
                new String[]{"--instance=" + instanceName});
        // ensureConfigDirectorySecurity() was already called inside fromEnvironment().

        // Seed config files — copy from the default instance if present, otherwise
        // create empty stubs so Manzan starts without errors.
        File defaultConfigDir = new File(CONFIG_BASE_DIR, CONFIG_DIR_PREFIX + "default");
        for (String fileName : TEMPLATE_INI_FILES) {
            File dest = new File(configDir, fileName);
            File template = new File(defaultConfigDir, fileName);
            if (template.isFile()) {
                Files.copy(template.toPath(), dest.toPath());
            } else {
                dest.createNewFile();
            }
            setPermissions600(dest);
        }

        System.out.println("Created instance '" + instanceName + "' at " + configDir);
        return ctx;
    }

    // -------------------------------------------------------------------------
    // destroyInstance
    // -------------------------------------------------------------------------

    /**
     * Remove a Manzan instance.
     *
     * Refuses to destroy an instance whose lock file indicates it is currently
     * running — the operator must stop the instance first.
     *
     * Deletes the config directory and, if a stale lock file is present, removes
     * it.  Optionally purges all database rows written by the instance by calling
     * the {@code CLEANUP_INSTANCE_DATA} stored procedure over a local JDBC
     * connection.
     *
     * @param instanceName  Name of the instance to destroy.
     * @param cleanDatabase When {@code true}, all rows in the Manzan event and
     *                      audit tables that belong to this instance are deleted
     *                      before the config directory is removed.
     * @throws IllegalStateException if the instance is currently running.
     * @throws IllegalArgumentException if the instance config directory does not exist.
     * @throws IOException if the config directory cannot be fully removed.
     * @throws SecurityException if the instance name is invalid.
     */
    public static void destroyInstance(String instanceName, boolean cleanDatabase)
            throws IOException {
        // Check existence before constructing InstanceContext — fromEnvironment()
        // creates the directory as a side effect via ensureConfigDirectorySecurity().
        // Name validation still happens implicitly: an invalid name produces a path
        // that will never exist, so the IllegalArgumentException fires first; a
        // reserved/invalid name causes fromEnvironment() to throw SecurityException
        // before we get to the lock check anyway.
        File configDir = new File("/QOpenSys/etc/manzan-" + instanceName);
        if (!configDir.exists()) {
            throw new IllegalArgumentException(
                    "Instance '" + instanceName + "' does not exist at " + configDir);
        }

        // Safety check — refuse to destroy a live instance.
        List<String> active = LockFile.listActiveLocks();
        if (active.contains(instanceName)) {
            throw new IllegalStateException(
                    "Instance '" + instanceName + "' is currently running. "
                    + "Stop it before destroying.");
        }

        // Optional DB cleanup — runs before the config dir is removed so that if
        // it fails the operator still has the config intact and can retry.
        if (cleanDatabase) {
            cleanDatabaseForInstance(instanceName);
        }

        // Remove config directory tree.
        deleteDirectoryTree(configDir);
        System.out.println("Removed config directory for instance '" + instanceName + "'.");

        // Remove this instance's stale lock file, if present.
        File lockFile = new File("/var/run/manzan/manzan-" + instanceName + ".lock");
        if (lockFile.exists() && !lockFile.delete()) {
            throw new IOException("Could not delete stale lock file: " + lockFile);
        }
    }

    // -------------------------------------------------------------------------
    // listInstances
    // -------------------------------------------------------------------------

    /**
     * List all known Manzan instances.
     *
     * An instance is known if its config directory ({@code /QOpenSys/etc/manzan-<name>})
     * exists.  The returned list also reflects any instances whose lock files exist
     * in {@code /var/run/manzan} but whose config directories are missing (orphaned
     * locks from abnormal exits), so callers get a complete picture.
     *
     * @return Unmodifiable list of {@link InstanceInfo} snapshots, sorted by instance name.
     */
    public static List<InstanceInfo> listInstances() {
        List<InstanceInfo> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // 1. Discover instances from config directories.
        File baseDir = new File(CONFIG_BASE_DIR);
        File[] entries = baseDir.listFiles(
                (dir, name) -> name.startsWith(CONFIG_DIR_PREFIX) && new File(dir, name).isDirectory());
        if (entries != null) {
            for (File entry : entries) {
                String name = entry.getName().substring(CONFIG_DIR_PREFIX.length());
                if (name.isEmpty()) {
                    continue;
                }
                seen.add(name);
                result.add(buildInstanceInfo(name, entry.getAbsolutePath()));
            }
        }

        // 2. Add any orphaned active lock entries not covered by a config directory.
        for (String name : LockFile.listActiveLocks()) {
            if (!seen.contains(name)) {
                String configDir = CONFIG_BASE_DIR + "/" + CONFIG_DIR_PREFIX + name;
                result.add(buildInstanceInfo(name, configDir));
            }
        }

        Collections.sort(result, (a, b) -> a.getInstanceName().compareTo(b.getInstanceName()));
        return Collections.unmodifiableList(result);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static InstanceInfo buildInstanceInfo(String instanceName, String configDirectory) {
        List<String> activeLocks = LockFile.listActiveLocks();
        boolean running = activeLocks.contains(instanceName);
        long pid = -1;
        if (running) {
            pid = readPidFromLockFile(instanceName);
        }
        return new InstanceInfo(instanceName, configDirectory, running, pid);
    }

    private static long readPidFromLockFile(String instanceName) {
        File lockFile = new File("/var/run/manzan/manzan-" + instanceName + ".lock");
        if (!lockFile.isFile()) {
            return -1;
        }
        try {
            String content = new String(Files.readAllBytes(lockFile.toPath())).trim();
            return Long.parseLong(content);
        } catch (Exception e) {
            return -1;
        }
    }

    private static void cleanDatabaseForInstance(String instanceName) throws IOException {
        // Use a local *CURRENT connection — the manager always runs on IBM i.
        String url = "jdbc:as400://localhost;naming=sql;errors=full;prompt=false";
        try (Connection conn = DriverManager.getConnection(url, "*CURRENT", "*CURRENT");
             Statement stmt = conn.createStatement()) {
            stmt.execute("CALL MANZAN.CLEANUP_INSTANCE_DATA('" + instanceName + "')");
            System.out.println("Database data cleaned for instance '" + instanceName + "'.");
        } catch (SQLException e) {
            throw new IOException(
                    "Failed to clean database data for instance '" + instanceName + "': "
                    + e.getMessage(), e);
        }
    }

    private static void setPermissions600(File file) throws IOException {
        Set<PosixFilePermission> perms = new HashSet<>(Arrays.asList(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE));
        try {
            Files.setPosixFilePermissions(file.toPath(), perms);
        } catch (UnsupportedOperationException e) {
            // Non-POSIX dev environment — skip.
        }
    }

    private static void deleteDirectoryTree(File root) throws IOException {
        if (root.isDirectory()) {
            File[] children = root.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteDirectoryTree(child);
                }
            }
        }
        if (!root.delete()) {
            throw new IOException("Could not delete: " + root);
        }
    }
}
