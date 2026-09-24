package com.github.theprez.manzan.configuration;

import java.io.File;
import java.io.IOException;

import org.ini4j.InvalidFileFormatException;

import com.github.theprez.jcmdutils.ConsoleQuestionAsker;
import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.manzan.InstanceContext;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400SecurityException;

public class ApplicationConfig extends Config {

    /**
     * Get ApplicationConfig for a specific instance.
     * Each call returns a new instance bound to the given InstanceContext.
     * The returned object caches its own AS400 connection — there is no shared
     * static state between instances.
     */
    public static ApplicationConfig get(final InstanceContext ctx) throws InvalidFileFormatException, IOException {
        return new ApplicationConfig(getConfigFile(ctx, "app.ini"));
    }

    /**
     * Get ApplicationConfig for the default instance.
     *
     * @deprecated Use {@link #get(InstanceContext)} for multi-instance support.
     */
    @Deprecated
    public static ApplicationConfig get() throws InvalidFileFormatException, IOException {
        return get(InstanceContext.getDefault());
    }

    // Per-instance connection cache — instance fields, not static, so each
    // ApplicationConfig holds its own connection independently.
    private AS400 m_connection = null;
    private String m_connectionPw = null;

    private ApplicationConfig(final File _f) throws InvalidFileFormatException, IOException {
        super(_f);
    }

    public AS400 getRemoteConnection() throws AS400SecurityException, IOException {

        if (isIBMi()) {
            return new AS400("localhost", "*CURRENT", "*CURRENT");
        }
        if (m_connectionPw != null) {
            final AS400 cacheHit = new AS400(m_connection);
            cacheHit.setPassword(m_connectionPw);
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
        m_connection = ret;
        m_connectionPw = pw;
        return ret;
    }

    public String getLibrary() {
        String configuredLib = getOptionalString("install", "library");
        if (StringUtils.isNonEmpty(configuredLib)) {
            return configuredLib;
        }
        return "manzan";
    }

    /**
     * Return the TCP port the ILE HANDLER uses to push watch events to this
     * instance.  Reads {@code socketPort} from the {@code [watch]} section of
     * {@code app.ini}.  Falls back to the {@code MANZAN_SOCKET_PORT} environment
     * variable, and finally to the historic default of 8080.
     *
     * <p>Each instance should be configured with a distinct port so that
     * multiple instances on the same host do not collide at bind time.</p>
     *
     * <p>Example {@code app.ini} entry:</p>
     * <pre>
     * [watch]
     * socketPort = 8081
     * </pre>
     */
    public int getSocketPort() {
        String configured = getOptionalString("watch", "socketPort");
        if (StringUtils.isNonEmpty(configured)) {
            try {
                return Integer.parseInt(configured.trim());
            } catch (NumberFormatException e) {
                throw new RuntimeException(
                        "Invalid socketPort in [watch] section of app.ini: '" + configured + "'");
            }
        }
        // Fall back to env var for backward compatibility
        String portEnv = System.getenv("MANZAN_SOCKET_PORT");
        if (portEnv != null && portEnv.matches("\\d+")) {
            return Integer.parseInt(portEnv);
        }
        return 8080;
    }

}
