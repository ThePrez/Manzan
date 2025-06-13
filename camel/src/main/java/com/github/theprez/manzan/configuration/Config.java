package com.github.theprez.manzan.configuration;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;

import com.github.theprez.jcmdutils.StringUtils;
import org.ini4j.Profile;

public abstract class Config {

    public static final String DIRECTORY_OVERRIDE_PROPERTY = "manzan.configdir";
    public static final String COMPONENT_OPTIONS_PREFIX = "componentOptions.";

    public static boolean isIBMi() {
        final String osName = System.getProperty("os.name", "Misty");
        return "os400".equalsIgnoreCase(osName) || "os/400".equalsIgnoreCase(osName);
    }
    protected static File getConfigFile(final String _name) throws IOException {

        final File configDir;
        final String configDirOverride = System.getProperty(DIRECTORY_OVERRIDE_PROPERTY);
        if(StringUtils.isNonEmpty(configDirOverride)) {
            configDir = new File(configDirOverride).getAbsoluteFile();
        } else {
            configDir = isIBMi() ? new File("/QOpenSys/etc/manzan") : new File(".").getAbsoluteFile();
        }

        if (!configDir.isDirectory()) {
            if (!configDir.mkdirs()) {
                throw new IOException("Cound not create configuration directory " + configDir.getAbsolutePath());
            }
            // TODO: set appropriate permissions
        }
        final File ret = new File(configDir, _name);
        if (!ret.isFile()) {
            ret.createNewFile();
            // TODO: set ownership and permissions
        }
        return ret;
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

    public Map<String, String> getUriAndHeaderParameters(final String _name, Profile.Section sectionObj, String... _exclusions) {
        final Map<String, String> pathParameters = new LinkedHashMap<String, String>();
        List<String> exclusions = new LinkedList<>(Arrays.asList(_exclusions));
        exclusions.addAll(Arrays.asList("type", "filter", "format", "id", "destinations", "interval"));
        for (final String sectionKey : sectionObj.keySet()) {
            if (exclusions.contains(sectionKey) || sectionKey.startsWith(Config.COMPONENT_OPTIONS_PREFIX)) {
                continue;
            }
            pathParameters.put(sectionKey, getRequiredString(_name, sectionKey));
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

    protected int getOptionalInt(final String _name, final String _key) {
        final String ret = m_ini.get(_name, _key);
        if (StringUtils.isEmpty(ret)) {
            return -1;
        }
        return Integer.valueOf(ret);
    }
}
