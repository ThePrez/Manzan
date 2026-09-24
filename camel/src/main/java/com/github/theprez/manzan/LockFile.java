package com.github.theprez.manzan;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Manages PID-based lock files to prevent duplicate instance execution.
 *
 * Security considerations:
 * - Lock files are created in /var/run/manzan (IBM i only)
 * - Uses Java NIO file locking for atomic operations
 * - Validates PID to detect stale locks from crashed processes
 * - Automatically cleans up stale locks
 * - Prevents race conditions through atomic file operations
 *
 * Lock file format:
 * - Filename: manzan-<instance-name>.lock
 * - Content: PID of the owning process
 * - Location: /var/run/manzan/
 */
public class LockFile {
    
    private static final String LOCK_DIR_IBMI = "/var/run/manzan";
    private static final String LOCK_FILE_SUFFIX = ".lock";
    private static final String LOCK_FILE_PREFIX = "manzan-";
    
    private final String instanceName;
    private final File lockFile;
    private RandomAccessFile lockFileHandle;
    private FileLock fileLock;
    private final long pid;
    
    /**
     * Private constructor - use acquire() factory method
     */
    private LockFile(String instanceName, File lockFile, long pid) {
        this.instanceName = instanceName;
        this.lockFile = lockFile;
        this.pid = pid;
    }
    
    /**
     * Acquire a lock file for the specified instance.
     * 
     * Security: Uses atomic file locking to prevent race conditions
     * 
     * @param instanceName The instance name to lock
     * @return LockFile instance if lock acquired, null if instance already running
     * @throws IOException if lock file cannot be created or accessed
     * @throws SecurityException if instance name is invalid
     */
    public static LockFile acquire(String instanceName) throws IOException {
        validateInstanceName(instanceName);
        
        File lockDir = getLockDirectory();
        ensureLockDirectoryExists(lockDir);
        
        File lockFile = new File(lockDir, LOCK_FILE_PREFIX + instanceName + LOCK_FILE_SUFFIX);
        long currentPid = getCurrentProcessId();
        
        // Check for stale lock and clean up if necessary
        if (lockFile.exists()) {
            if (isLockStale(lockFile, currentPid)) {
                System.out.println("Removing stale lock file: " + lockFile);
                if (!lockFile.delete()) {
                    System.err.println("Warning: Could not delete stale lock file: " + lockFile);
                }
            } else {
                // Lock file exists and is not stale - another instance is running
                System.out.println("Lock file exists for instance '" + instanceName + "' and is held by an active process");
                return null;
            }
        }
        
        RandomAccessFile raf = null;
        FileLock lock = null;
        
        try {
            // Try to acquire the lock
            raf = new RandomAccessFile(lockFile, "rw");
            FileChannel channel = raf.getChannel();
            
            // Security: Use exclusive lock to prevent concurrent access
            lock = channel.tryLock();
            
            if (lock == null) {
                // Lock is held by another process
                raf.close();
                return null;
            }
            
            // Write PID to lock file
            raf.setLength(0); // Clear any existing content
            raf.writeBytes(String.valueOf(currentPid));
            raf.getChannel().force(true); // Ensure written to disk
            
            LockFile lockFileObj = new LockFile(instanceName, lockFile, currentPid);
            lockFileObj.lockFileHandle = raf;
            lockFileObj.fileLock = lock;
            
            // Register shutdown hook to clean up lock file
            registerShutdownHook(lockFileObj);
            
            System.out.println("Acquired lock for instance '" + instanceName + "' (PID: " + currentPid + ")");
            return lockFileObj;
            
        } catch (OverlappingFileLockException e) {
            // Another thread in this JVM already holds the lock
            // Clean up resources
            if (lock != null && lock.isValid()) {
                try {
                    lock.release();
                } catch (IOException ex) {
                    // Ignore
                }
            }
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException ex) {
                    // Ignore
                }
            }
            System.err.println("Instance '" + instanceName + "' is already running in this JVM");
            return null;
        } catch (IOException e) {
            // Clean up resources on error
            if (lock != null && lock.isValid()) {
                try {
                    lock.release();
                } catch (IOException ex) {
                    // Ignore
                }
            }
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException ex) {
                    // Ignore
                }
            }
            throw new IOException("Failed to acquire lock for instance '" + instanceName + "': " + e.getMessage(), e);
        }
    }
    
    /**
     * Release the lock file
     * Security: Ensures proper cleanup of file handles and locks
     */
    public void release() {
        try {
            if (fileLock != null && fileLock.isValid()) {
                fileLock.release();
                fileLock = null;
            }
            
            if (lockFileHandle != null) {
                lockFileHandle.close();
                lockFileHandle = null;
            }
            
            // Delete lock file
            if (lockFile != null && lockFile.exists()) {
                if (lockFile.delete()) {
                    System.out.println("Released lock for instance '" + instanceName + "'");
                } else {
                    System.err.println("Warning: Could not delete lock file: " + lockFile);
                }
            }
        } catch (IOException e) {
            System.err.println("Error releasing lock file: " + e.getMessage());
        }
    }
    
    /**
     * Check if a lock file is stale (process no longer running)
     * Security: Validates PID to prevent false positives
     *
     * @param lockFile The lock file to check
     * @param currentPid The PID of the current process (to avoid false positives)
     * @return true if the lock is stale and can be removed
     */
    private static boolean isLockStale(File lockFile, long currentPid) {
        try {
            // Read PID from lock file
            String content = new String(Files.readAllBytes(lockFile.toPath())).trim();
            
            if (content.isEmpty()) {
                // Empty lock file is stale
                return true;
            }
            
            long pid;
            try {
                pid = Long.parseLong(content);
            } catch (NumberFormatException e) {
                // Invalid PID format - consider stale
                return true;
            }
            
            // PID 0 and negative PIDs are never valid lock owners.
            if (pid <= 0) {
                return true;
            }

            // If the lock belongs to the current process, it's NOT stale
            // This prevents the same process from acquiring the lock twice
            if (pid == currentPid) {
                return false;
            }

            // Check if process is still running
            return !isProcessRunning(pid);
            
        } catch (IOException e) {
            // Cannot read lock file - assume not stale to be safe
            System.err.println("Warning: Cannot read lock file to check staleness: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if a process with the given PID is running.
     *
     * Uses POSIX kill -0 (signal 0) via the Runtime, which works on any
     * POSIX-compliant system including IBM i PASE regardless of whether
     * the /proc filesystem is mounted.
     *
     * Signal 0 does not kill the process — it only checks whether the
     * calling process has permission to send a signal to the target.
     * Exit code 0 means the process exists; non-zero means it does not.
     *
     * Java 8 compatible implementation.
     */
    private static boolean isProcessRunning(long pid) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"kill", "-0", String.valueOf(pid)});
            return p.waitFor() == 0;
        } catch (Exception e) {
            // If we can't determine, assume process is running to be safe
            System.err.println("Warning: Cannot determine if process " + pid + " is running: " + e.getMessage());
            return true;
        }
    }
    
    /**
     * Get current process ID
     * Security: Java 8 compatible implementation using ManagementFactory
     */
    private static long getCurrentProcessId() {
        try {
            // Java 8: Parse from JVM name (format is typically "pid@hostname")
            String jvmName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
            return Long.parseLong(jvmName.split("@")[0]);
        } catch (Exception e) {
            // Fallback: Use thread ID (not ideal but better than nothing)
            System.err.println("Warning: Cannot determine process ID, using thread ID as fallback");
            return Thread.currentThread().getId();
        }
    }
    
    /**
     * Get lock directory for IBM i
     * IBM i only: Always uses /var/run/manzan
     * Security: Standard system directory with appropriate permissions
     */
    private static File getLockDirectory() {
        // IBM i: Use /var/run/manzan
        return new File(LOCK_DIR_IBMI);
    }
    
    /**
     * Ensure lock directory exists with appropriate permissions
     * Security: Creates directory if it doesn't exist
     */
    private static void ensureLockDirectoryExists(File lockDir) throws IOException {
        if (!lockDir.exists()) {
            if (!lockDir.mkdirs()) {
                throw new IOException("Failed to create lock directory: " + lockDir);
            }
            System.out.println("Created lock directory: " + lockDir);
        }
        
        if (!lockDir.isDirectory()) {
            throw new IOException("Lock path exists but is not a directory: " + lockDir);
        }
        
        // Verify directory is writable
        if (!lockDir.canWrite()) {
            throw new IOException("Lock directory is not writable: " + lockDir);
        }
    }
    
    /**
     * Validate instance name against the same rules as InstanceContext.
     * Security: Prevents path traversal and injection attacks.
     * Pattern mirrors InstanceContext.VALID_INSTANCE_NAME: ^[a-z0-9_-]{1,32}$
     */
    private static final Pattern VALID_INSTANCE_NAME = Pattern.compile("^[a-z0-9_-]{1,32}$");

    private static void validateInstanceName(String instanceName) {
        if (instanceName == null || instanceName.trim().isEmpty()) {
            throw new SecurityException("Instance name cannot be null or empty");
        }

        // Security: Prevent path traversal
        if (instanceName.contains("..") || instanceName.contains("/") || instanceName.contains("\\")) {
            throw new SecurityException("Instance name contains invalid path characters: " + instanceName);
        }

        // Security: Enforce same whitelist as InstanceContext (lowercase, max 32 chars)
        if (!VALID_INSTANCE_NAME.matcher(instanceName).matches()) {
            throw new SecurityException(
                "Invalid instance name: '" + instanceName + "'. " +
                "Instance names must be 1-32 characters, containing only lowercase letters, " +
                "numbers, hyphens, and underscores."
            );
        }
    }

    /**
     * Clean up all stale lock files in the lock directory.
     * A lock file is stale if its PID no longer refers to a running process.
     *
     * @return number of stale lock files removed
     */
    public static int cleanupAllStaleLocks() {
        File lockDir = getLockDirectory();
        if (!lockDir.exists() || !lockDir.isDirectory()) {
            return 0;
        }

        File[] lockFiles = lockDir.listFiles(
            (dir, name) -> name.startsWith(LOCK_FILE_PREFIX) && name.endsWith(LOCK_FILE_SUFFIX)
        );
        if (lockFiles == null) {
            return 0;
        }

        long currentPid = getCurrentProcessId();
        int removed = 0;
        for (File lockFile : lockFiles) {
            if (isLockStale(lockFile, currentPid)) {
                if (lockFile.delete()) {
                    System.out.println("Removed stale lock file: " + lockFile);
                    removed++;
                } else {
                    System.err.println("Warning: Could not remove stale lock file: " + lockFile);
                }
            }
        }
        return removed;
    }

    /**
     * List all currently active (non-stale) lock files.
     *
     * @return list of instance names that have active locks
     */
    public static List<String> listActiveLocks() {
        File lockDir = getLockDirectory();
        List<String> active = new ArrayList<>();
        if (!lockDir.exists() || !lockDir.isDirectory()) {
            return active;
        }

        File[] lockFiles = lockDir.listFiles(
            (dir, name) -> name.startsWith(LOCK_FILE_PREFIX) && name.endsWith(LOCK_FILE_SUFFIX)
        );
        if (lockFiles == null) {
            return active;
        }

        long currentPid = getCurrentProcessId();
        for (File lockFile : lockFiles) {
            if (!isLockStale(lockFile, currentPid)) {
                // Strip "manzan-" prefix and ".lock" suffix to recover instance name
                String name = lockFile.getName();
                String instanceName = name.substring(LOCK_FILE_PREFIX.length(),
                    name.length() - LOCK_FILE_SUFFIX.length());
                active.add(instanceName);
            }
        }
        return active;
    }

    /**
     * Return the number of currently active (non-stale) lock files.
     *
     * @return count of active instances
     */
    public static int getActiveInstanceCount() {
        return listActiveLocks().size();
    }
    
    /**
     * Register shutdown hook to clean up lock file on JVM exit
     * Security: Ensures lock is released even on abnormal termination
     */
    private static void registerShutdownHook(final LockFile lockFile) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                lockFile.release();
            } catch (Exception e) {
                System.err.println("Error in shutdown hook: " + e.getMessage());
            }
        }, "LockFile-Cleanup-" + lockFile.instanceName));
    }
    
    /**
     * Check if this lock is still valid
     */
    public boolean isValid() {
        return fileLock != null && fileLock.isValid() && lockFile.exists();
    }
    
    /**
     * Get the instance name associated with this lock
     */
    public String getInstanceName() {
        return instanceName;
    }
    
    /**
     * Get the lock file path
     */
    public File getLockFile() {
        return lockFile;
    }
    
    /**
     * Get the PID of the process holding this lock
     */
    public long getPid() {
        return pid;
    }
    
    @Override
    public String toString() {
        return "LockFile{" +
                "instanceName='" + instanceName + '\'' +
                ", lockFile=" + lockFile +
                ", pid=" + pid +
                ", valid=" + isValid() +
                '}';
    }
}
