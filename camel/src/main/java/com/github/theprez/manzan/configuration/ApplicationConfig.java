package com.github.theprez.manzan.configuration;

import java.io.File;
import java.io.IOException;

import org.ini4j.InvalidFileFormatException;

import com.github.theprez.jcmdutils.ConsoleQuestionAsker;
import com.github.theprez.jcmdutils.StringUtils;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400SecurityException;

public class ApplicationConfig extends Config {
    private static AS400 m_cached = null;
    private static String m_cachedPw = null;
    private static ApplicationConfig s_cached = null;

    public static ApplicationConfig get() throws InvalidFileFormatException, IOException {
        if (null != s_cached) {
            return s_cached;
        }
        return s_cached = new ApplicationConfig(getConfigFile("app.ini"));
    }

    private ApplicationConfig(final File _f) throws InvalidFileFormatException, IOException {
        super(_f);
    }

    public AS400 getRemoteConnection() throws AS400SecurityException, IOException {

        if(isIBMi()) {
            return new AS400("localhost", "*CURRENT", "*CURRENT");
        }
        if (null != m_cachedPw) {
            final AS400 cacheHit = new AS400(m_cached);
            cacheHit.setPassword(m_cachedPw);
            return cacheHit;
        }
        final ConsoleQuestionAsker asker = new ConsoleQuestionAsker();
        String system = super.getOptionalString("remote", "system");
        if (StringUtils.isEmpty(system)) {
            system = asker.askUserOrThrow("System name: ");
        }
        String user = super.getOptionalString("remote", "user");
        if (StringUtils.isEmpty(user)) {
            user = asker.askUserOrThrow("Username: ");
        }
        String pw = super.getOptionalString("remote", "password");
        if (StringUtils.isEmpty(pw)) {
            pw = asker.askUserForPwd("Password: ");
        }

        final AS400 ret = new AS400(system, user, pw);
        ret.validateSignon();
        m_cached = ret;
        m_cachedPw = pw;
        return ret;
    }

    public String getLibrary() {
        String configuredLib = getOptionalString("install", "library");
        if(StringUtils.isNonEmpty(configuredLib)) {
            return configuredLib;
        }
        return "manzan";
    }

}
