package com.github.theprez.manzan.configuration;

import java.io.File;
import java.io.IOException;

import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;

import com.github.theprez.jcmdutils.StringUtils;

public abstract class Config {
    private final Ini m_ini;

    protected Config (final File _f) throws InvalidFileFormatException, IOException {
        m_ini = new Ini(_f);
    }
    protected Ini getIni() {
        return m_ini;
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
    protected String getOptionalString(final String _name, final String _key) {
        final String ret = m_ini.get(_name, _key);
        if (StringUtils.isEmpty(ret)) {
           return null;
        }
        return ret;
    }
}
