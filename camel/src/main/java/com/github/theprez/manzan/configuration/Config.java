package com.github.theprez.manzan.configuration;

import java.io.File;
import java.io.IOException;

import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;

import com.github.theprez.jcmdutils.StringUtils;

public abstract class Config {

    public static final String DIRECTORY_OVERRIDE_PROPERTY = "manzan.configdir";

    protected static boolean isIBMi() {
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

    protected int getOptionalInt(final String _name, final String _key) {
        final String ret = m_ini.get(_name, _key);
        if (StringUtils.isEmpty(ret)) {
            return -1;
        }
        return Integer.valueOf(ret);
    }
}
