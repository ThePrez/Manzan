package com.github.theprez.manzan.configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;

import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;

import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.manzan.InstanceContext;
import org.ini4j.Profile;

public abstract class Config {

    public static final String DIRECTORY_OVERRIDE_PROPERTY = "manzan.configdir";
    public static final String COMPONENT_OPTIONS_PREFIX = "componentOptions.";
    public static final String DATA_MAP_INJECTIONS_PREFIX = "injections.";


    public static boolean isIBMi() {
        final String osName = System.getProperty("os.name", "Misty");
        return "os400".equalsIgnoreCase(osName) || "os/400".equalsIgnoreCase(osName);
    }
    
    /**
     * Get configuration file for the default instance (backward compatibility)
     * @deprecated Use getConfigFile(InstanceContext, String) for multi-instance support
     */
    @Deprecated
    protected static File getConfigFile(final String _name) throws IOException {
        // Backward compatibility: Use default instance context
        try {
            InstanceContext defaultContext = InstanceContext.getDefault();
            return getConfigFile(defaultContext, _name);
        } catch (Exception e) {
            // Fallback to legacy behavior if InstanceContext fails
            return getConfigFileLegacy(_name);
        }
    }
    
    /**
     * Get configuration file for a specific instance (multi-instance support)
     * Security: Uses instance-specific config directory with proper permissions
     *
     * @param ctx Instance context
     * @param _name Configuration file name
     * @return Configuration file
     * @throws IOException if file cannot be created or secured
     */
    protected static File getConfigFile(final InstanceContext ctx, final String _name) throws IOException {
        if (ctx == null) {
            throw new IllegalArgumentException("InstanceContext cannot be null");
        }
        
        // Get instance-specific config directory
        final File configDir = new File(ctx.getConfigDirectory());
        
        // Ensure directory exists with proper security
        ctx.ensureConfigDirectorySecurity();
        
        // Create config file if it doesn't exist
        final File ret = new File(configDir, _name);
        if (!ret.isFile()) {
            ret.createNewFile();
            // Set permissions immediately and verify before delegating to validateConfigFileSecurity,
            // so a silent setPosixFilePermissions failure is caught here rather than surfacing as a
            // misleading "insecure permissions" error on a brand-new file.
            setFilePermissionsTo600(ret);
            verifyFilePermissions600(ret);
            return ret;
        }

        // Validate file security for pre-existing files
        ctx.validateConfigFileSecurity(ret);

        return ret;
    }
    
    /**
     * Legacy config file resolution (for backward compatibility fallback)
     * @deprecated Internal use only - use getConfigFile(InstanceContext, String)
     */
    @Deprecated
    private static File getConfigFileLegacy(final String _name) throws IOException {
        final File configDir;
        final String configDirOverride = System.getProperty(DIRECTORY_OVERRIDE_PROPERTY);
        if(StringUtils.isNonEmpty(configDirOverride)) {
            configDir = new File(configDirOverride).getAbsoluteFile();
        } else {
            configDir = isIBMi() ? new File("/QOpenSys/etc/manzan") : new File(".").getAbsoluteFile();
        }

        if (!configDir.isDirectory()) {
            if (!configDir.mkdirs()) {
                throw new IOException("Could not create configuration directory " + configDir.getAbsolutePath());
            }
        }
        final File ret = new File(configDir, _name);
        if (!ret.isFile()) {
            ret.createNewFile();
            setFilePermissionsTo600(ret);
        }
        ensureOnlyOwnerCanReadAndWriteFile(ret);
        return ret;
    }

    /**
     * Verify that a file's permissions are exactly 600 after an explicit set.
     * Distinct from ensureOnlyOwnerCanReadAndWriteFile so it can be called without
     * the UnsupportedOperationException swallowing that the legacy path uses.
     */
    private static void verifyFilePermissions600(File file) throws IOException {
        Path path = file.toPath();
        try {
            Set<PosixFilePermission> perms = Files.getPosixFilePermissions(path);
            Set<PosixFilePermission> expected = new HashSet<>(Arrays.asList(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE
            ));
            if (!perms.equals(expected)) {
                throw new IOException(
                    "Failed to set permissions to 600 on newly created config file: " + file.getAbsolutePath()
                    + ". Found: " + perms
                );
            }
        } catch (UnsupportedOperationException e) {
            // Non-POSIX system (e.g., developer workstation) — skip
        }
    }

    private static void ensureOnlyOwnerCanReadAndWriteFile(File file) throws SecurityException, IOException {
        Path path = file.toPath();

        // Only works on POSIX-compliant systems (e.g., Linux, macOS)
        Set<PosixFilePermission> perms = Files.getPosixFilePermissions(path);

        Set<PosixFilePermission> expected = new HashSet<>(Arrays.asList(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE
        ));


        if (!perms.equals(expected)) {
            throw new SecurityException("File " + file.getAbsolutePath() +
                    " does not have permission 600. Found: " + perms
                    + ". Set your file permissions to 600 and retry.");
        }
    }

    private static void setFilePermissionsTo600(File file) throws IOException {
        Path path = file.toPath();

        Set<PosixFilePermission> perms = new HashSet<>(Arrays.asList(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE
        ));


        Files.setPosixFilePermissions(path, perms);
        System.out.println("Permissions set to 600.");
    }

    private final Ini m_ini;

    protected Config(final File _f) throws InvalidFileFormatException, IOException {
        m_ini = new Ini(_f);
    }

    protected Ini getIni() {
        return m_ini;
    }

    protected String getOptionalString(final String _name, final String _key) {
        final String ret = m_ini.get(_name, _key);
        if (StringUtils.isEmpty(ret)) {
            return null;
        }
        return ret;
    }

    protected int getRequiredInt(final String _name, final String _key) {
        return Integer.valueOf(getRequiredString(_name, _key));
    }

    protected String getRequiredString(final String _name, final String _key) {
        final String ret = m_ini.get(_name, _key);
        if (StringUtils.isEmpty(ret)) {
            throw new RuntimeException("Required value for '" + _key + "' in [" + _name + "] not found");
        }
        return ret;
    }

    private boolean isValidSectionKey(String key, List<String> exclusions){
        return !exclusions.contains(key) 
            && !key.startsWith(Config.COMPONENT_OPTIONS_PREFIX)
            && !key.startsWith(Config.DATA_MAP_INJECTIONS_PREFIX);
    }

    public Map<String, String> getUriAndHeaderParameters(final String _name, Profile.Section sectionObj, String... _exclusions) {
        final Map<String, String> pathParameters = new LinkedHashMap<String, String>();
        List<String> exclusions = new LinkedList<>(Arrays.asList(_exclusions));
        exclusions.addAll(Arrays.asList("type", "filter", "format", "destinations", "interval"));
        for (final String sectionKey : sectionObj.keySet()) {
            if (isValidSectionKey(sectionKey, exclusions)){
                pathParameters.put(sectionKey, getRequiredString(_name, sectionKey));
            }
        }
        return pathParameters;
    }

    protected Map<String, String> getComponentOptions(final String _name) {
            Map<String, String> section = m_ini.get(_name);

            // Map to store key-value pairs
            Map<String, String> componentOptionsMap = new HashMap<>();
            if (section != null) {
                // Iterate through the section's keys and values
                for (Map.Entry<String, String> entry : section.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();

                    // Check if the key starts with "componentOptions."
                    if (key.startsWith(COMPONENT_OPTIONS_PREFIX)) {
                        // Store it in the map
                        componentOptionsMap.put(key.substring(COMPONENT_OPTIONS_PREFIX.length()), value);
                    }
                }
            }
            return componentOptionsMap;
    }

    protected Map<String, String> getDataMapInjections(final String _name) {
            Map<String, String> section = m_ini.get(_name);

            // Map to store key-value pairs
            Map<String, String> dataMapInjections = new HashMap<>();
            if (section != null) {
                // Iterate through the section's keys and values
                for (Map.Entry<String, String> entry : section.entrySet()) {
                    String key = entry.getKey();
                    String value = entry.getValue();

                    // Check if the key starts with "injections."
                    if (key.startsWith(DATA_MAP_INJECTIONS_PREFIX)) {
                        // Store it in the map
                        dataMapInjections.put(key.substring(DATA_MAP_INJECTIONS_PREFIX.length()), value);
                    }
                }
            }
            return dataMapInjections;
    }

    protected int getOptionalInt(final String _name, final String _key) {
        final String ret = m_ini.get(_name, _key);
        if (StringUtils.isEmpty(ret)) {
            return -1;
        }
        return Integer.valueOf(ret);
    }
}
