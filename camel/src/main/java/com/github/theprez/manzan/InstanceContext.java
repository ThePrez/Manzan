package com.github.theprez.manzan;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import com.github.theprez.jcmdutils.StringUtils;

/**
 * Manages instance-specific context for multi-instance Manzan deployments.
 * Each instance has:
 * - Unique instance name
 * - Isolated configuration directory
 * - User profile association (IBM i)
 * - Unique session ID prefix
 * 
 * Security considerations:
 * - Instance names are validated to prevent path traversal attacks
 * - Configuration directories are created with restrictive permissions (700)
 * - Configuration files are validated to have 600 permissions
 * - User profile is captured for audit trails
 */
public class InstanceContext {
    
    // Security: Restrict instance names to alphanumeric, dash, and underscore only
    private static final Pattern VALID_INSTANCE_NAME = Pattern.compile("^[a-z0-9_-]{1,32}$");
    private static final String DEFAULT_INSTANCE_NAME = "default";
    private static final String INSTANCE_ENV_VAR = "MANZAN_INSTANCE";
    private static final String INSTANCE_ARG_PREFIX = "--instance=";
    
    // Atomic counter for guaranteed session ID uniqueness within this instance.
    // Instance field (not static) so that two InstanceContext objects (e.g. in
    // tests or a future in-process manager) each maintain independent counters
    // and never interleave session IDs.
    private final AtomicLong sessionIdCounter = new AtomicLong(0);
    
    private final String instanceName;
    private final String configDirectory;
    private final String userProfile;
    private final String sessionIdPrefix;
    
    /**
     * Private constructor - use factory methods to create instances
     */
    private InstanceContext(String instanceName, String configDirectory, String userProfile) {
        this.instanceName = instanceName;
        this.configDirectory = configDirectory;
        this.userProfile = userProfile;
        this.sessionIdPrefix = generateSessionIdPrefix(instanceName);
    }
    
    /**
     * Create InstanceContext from command line arguments or environment.
     * Priority: 1) Command line args, 2) Environment variable, 3) Default
     * 
     * @param args Command line arguments
     * @return InstanceContext instance
     * @throws SecurityException if instance name is invalid
     * @throws IOException if config directory cannot be created or secured
     */
    public static InstanceContext fromEnvironment(String[] args) throws IOException {
        String instanceName = parseInstanceParameter(args);

        if (instanceName == null) {
            instanceName = System.getenv(INSTANCE_ENV_VAR);
        }

        if (instanceName == null) {
            instanceName = DEFAULT_INSTANCE_NAME;
        }

        // Security: Validate instance name to prevent path traversal and injection attacks
        validateInstanceName(instanceName);

        String configDir = resolveConfigDirectory(instanceName);
        String userProfile = getCurrentUserProfile();

        InstanceContext context = new InstanceContext(instanceName, configDir, userProfile);

        // Security: Ensure config directory exists with proper permissions
        context.ensureConfigDirectorySecurity();

        return context;
    }
    
    /**
     * Create a default instance context (for backward compatibility)
     */
    public static InstanceContext getDefault() throws IOException {
        return fromEnvironment(new String[0]);
    }
    
    /**
     * Parse --instance parameter from command line arguments
     * Security: Only accepts the first --instance parameter to prevent confusion attacks
     */
    private static String parseInstanceParameter(String[] args) {
        if (args == null) {
            return null;
        }
        
        for (String arg : args) {
            if (arg != null && arg.startsWith(INSTANCE_ARG_PREFIX)) {
                String instanceName = arg.substring(INSTANCE_ARG_PREFIX.length()).trim();
                // Empty string is invalid - throw immediately
                if (instanceName.isEmpty()) {
                    throw new SecurityException("Instance name cannot be empty");
                }
                return instanceName.toLowerCase(); // Normalize to lowercase
            }
        }
        return null;
    }
    
    /**
     * Validate instance name against security constraints
     * Security: Prevents path traversal, command injection, and other attacks
     * 
     * @throws SecurityException if instance name is invalid
     */
    private static void validateInstanceName(String instanceName) {
        if (StringUtils.isEmpty(instanceName)) {
            throw new SecurityException("Instance name cannot be empty");
        }

        // Security: Check against whitelist pattern on the raw name — callers that
        // want case-folding must normalise before calling this method.  Checking the
        // raw value ensures uppercase letters are explicitly rejected rather than
        // silently accepted after lowercasing.
        if (!VALID_INSTANCE_NAME.matcher(instanceName).matches()) {
            throw new SecurityException(
                "Invalid instance name: '" + instanceName + "'. " +
                "Instance names must be 1-32 characters, containing only lowercase letters, " +
                "numbers, hyphens, and underscores. No spaces or special characters allowed."
            );
        }

        // Use lowercase for the remaining checks (path traversal, reserved names).
        String normalized = instanceName.toLowerCase();
        
        // Security: Prevent path traversal attempts
        if (normalized.contains("..") || normalized.contains("/") || normalized.contains("\\")) {
            throw new SecurityException(
                "Instance name contains invalid path characters: '" + instanceName + "'"
            );
        }
        
        // Security: Prevent reserved names that could cause conflicts
        Set<String> reservedNames = new HashSet<>(Arrays.asList(
            ".", "..", "con", "prn", "aux", "nul", "com1", "com2", "com3", "com4",
            "lpt1", "lpt2", "lpt3", "root", "admin", "system"
        ));
        if (reservedNames.contains(normalized)) {
            throw new SecurityException(
                "Instance name '" + instanceName + "' is reserved and cannot be used"
            );
        }
    }
    
    /**
     * Resolve configuration directory path based on instance name.
     * Respects the {@code manzan.configdir} system property (set via {@code --configdir})
     * so that test harnesses and legacy deployments that pass an explicit directory
     * continue to work after the multi-instance refactor.
     * When no override is set, falls back to the standard IBM i path
     * {@code /QOpenSys/etc/manzan-<instance>}.
     */
    private static String resolveConfigDirectory(String instanceName) {
        // "manzan.configdir" is the same constant as Config.DIRECTORY_OVERRIDE_PROPERTY.
        // We read it directly here to avoid a circular dependency between this class and
        // the configuration package (Config already imports InstanceContext).
        String override = System.getProperty("manzan.configdir");
        if (StringUtils.isNonEmpty(override)) {
            return new File(override).getAbsolutePath();
        }
        // IBM i: /QOpenSys/etc/manzan-<instance>
        return "/QOpenSys/etc/manzan-" + instanceName;
    }

    /**
     * Get current user profile (IBM i) or username
     * Security: Used for audit trails and ownership validation
     */
    private static String getCurrentUserProfile() {
        String userName = System.getProperty("user.name");
        if (StringUtils.isEmpty(userName)) {
            return "UNKNOWN";
        }
        return userName.toUpperCase(); // IBM i user profiles are uppercase
    }
    
    
    /**
     * Generate human-readable session ID prefix from instance name
     *
     * Strategy: Use actual instance name characters when possible for readability
     * - Takes first 6 alphanumeric characters from instance name
     * - Converts to uppercase for IBM i compatibility
     * - Pads with '0' if name is shorter than 6 characters
     * - Replaces non-alphanumeric with '0'
     *
     * Examples:
     *   "watson-monitor" -> "WATSON"
     *   "nav-01" -> "NAV010"
     *   "default" -> "DEFAUL"
     *   "admin" -> "ADMIN0"
     *
     * Security: Still deterministic and unique per instance name
     */
    private String generateSessionIdPrefix(String instanceName) {
        // Remove hyphens and underscores, keep alphanumeric only
        String cleaned = instanceName.replaceAll("[^a-zA-Z0-9]", "");
        
        // Take first 6 characters, pad with '0' if shorter
        String prefix = (cleaned + "000000").substring(0, 6).toUpperCase();
        
        return prefix;
    }
    
    /**
     * Generate unique session ID for this instance
     *
     * Format: <6-char-readable-prefix><2-char-instance-hash><2-char-counter>
     *
     * Examples:
     *   Instance "watson-monitor" -> "WATSON<hash>01", "WATSON<hash>02", etc.
     *   Instance "navigator-01" -> "NAVIGA<hash>01", "NAVIGA<hash>02", etc.
     *   Instance "default" -> "DEFAUL<hash>01", "DEFAUL<hash>02", etc.
     *
     * The instance hash distinguishes names that share the same readable prefix,
     * while the counter distinguishes sessions for the same instance.
     *
     * The fixed-width format fits the IBM i SESSION_ID field.
     *
     * @param baseName Optional base name for the session (not used in current implementation)
     * @return 10-character session ID
     */
    public String generateSessionId(String baseName) {
        // Keep the readable prefix while using instance-specific hash characters to
        // distinguish instance names whose first six characters are identical.
        long counter = sessionIdCounter.incrementAndGet() % 100;
        String instanceHash = String.format("%02X", instanceName.hashCode() & 0xFF);
        String counterStr = String.format("%02d", counter);
        return sessionIdPrefix + instanceHash + counterStr;
    }
    
    /**
     * Ensure configuration directory exists with secure permissions
     * Security: Creates directory with 700 permissions (owner only)
     * 
     * @throws IOException if directory cannot be created or secured
     * @throws SecurityException if directory has insecure permissions
     */
    public void ensureConfigDirectorySecurity() throws IOException {
        File configDir = new File(configDirectory);
        
        // Create directory if it doesn't exist
        if (!configDir.exists()) {
            if (!configDir.mkdirs()) {
                throw new IOException(
                    "Failed to create configuration directory: " + configDirectory
                );
            }
            System.out.println("Created configuration directory: " + configDirectory);
        }
        
        if (!configDir.isDirectory()) {
            throw new IOException(
                "Configuration path exists but is not a directory: " + configDirectory
            );
        }
        
        // Security: Set directory permissions to 700 (owner only)
        setDirectoryPermissions(configDir);
        
        // Security: Validate directory ownership
        validateDirectoryOwnership(configDir);
    }
    
    /**
     * Set directory permissions to 700 (drwx------)
     * Security: Only owner can read, write, and execute (list directory)
     */
    private void setDirectoryPermissions(File directory) throws IOException {
        Path path = directory.toPath();
        
        Set<PosixFilePermission> perms = new HashSet<>(Arrays.asList(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE
        ));
        
        try {
            Files.setPosixFilePermissions(path, perms);
            System.out.println("Set directory permissions to 700: " + directory);
        } catch (UnsupportedOperationException e) {
            // Non-POSIX system (e.g., Windows) - skip permission setting
            System.out.println("Warning: Cannot set POSIX permissions on this system");
        }
    }
    
    /**
     * Validate directory ownership matches current user
     * Security: Prevents using directories owned by other users
     */
    private void validateDirectoryOwnership(File directory) throws IOException {
        try {
            Path path = directory.toPath();
            String owner = Files.getOwner(path).getName();
            String currentUser = System.getProperty("user.name");

            // IBM i / POSIX systems generally return just the profile name, but some local
            // development environments (notably Windows) include a domain prefix such as
            // "AzureAD\\username".  Accept either exact match or a trailing "\\username"
            // match so local validation does not fail spuriously.
            boolean matchesCurrentUser = owner.equals(currentUser)
                    || owner.endsWith("\\" + currentUser)
                    || owner.endsWith("/" + currentUser);

            if (!matchesCurrentUser) {
                throw new SecurityException(
                    "Configuration directory is not owned by current user. " +
                    "Directory: " + directory + ", Owner: " + owner + ", Current user: " + currentUser
                );
            }
        } catch (UnsupportedOperationException e) {
            // Non-POSIX system - skip ownership check
            System.out.println("Warning: Cannot verify directory ownership on this system");
        }
    }
    
    /**
     * Validate configuration file has secure permissions (600)
     * Security: Only owner can read and write
     */
    public void validateConfigFileSecurity(File configFile) throws IOException {
        if (!configFile.exists()) {
            return; // File will be created with proper permissions
        }
        
        Path path = configFile.toPath();
        
        try {
            Set<PosixFilePermission> perms = Files.getPosixFilePermissions(path);
            
            Set<PosixFilePermission> expected = new HashSet<>(Arrays.asList(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE
            ));
            
            if (!perms.equals(expected)) {
                throw new SecurityException(
                    "Configuration file has insecure permissions: " + configFile + ". " +
                    "Expected: 600 (rw-------), Found: " + perms + ". " +
                    "Run: chmod 600 " + configFile.getAbsolutePath()
                );
            }
        } catch (UnsupportedOperationException e) {
            // Non-POSIX system - skip permission check
            System.out.println("Warning: Cannot verify file permissions on this system");
        }
    }
    
    // Getters
    
    public String getInstanceName() {
        return instanceName;
    }
    
    public String getConfigDirectory() {
        return configDirectory;
    }
    
    public String getUserProfile() {
        return userProfile;
    }
    
    public String getSessionIdPrefix() {
        return sessionIdPrefix;
    }
    
    public boolean isDefaultInstance() {
        return DEFAULT_INSTANCE_NAME.equals(instanceName);
    }
    
    @Override
    public String toString() {
        return "InstanceContext{" +
                "instanceName='" + instanceName + '\'' +
                ", configDirectory='" + configDirectory + '\'' +
                ", userProfile='" + userProfile + '\'' +
                ", sessionIdPrefix='" + sessionIdPrefix + '\'' +
                '}';
    }
}