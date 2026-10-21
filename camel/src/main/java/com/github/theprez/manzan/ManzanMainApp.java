package com.github.theprez.manzan;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;

import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.manzan.configuration.ApplicationConfig;
import com.github.theprez.manzan.configuration.Config;
import com.github.theprez.manzan.configuration.DataConfig;
import com.github.theprez.manzan.configuration.DestinationConfig;
import com.github.theprez.manzan.routes.ManzanRoute;
import com.github.theprez.manzan.routes.dest.PrometheusDestination;
import com.github.theprez.manzan.routes.event.WatchMsgEventSockets;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400JDBCDataSource;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.CommandCall;

/**
 * A Camel Application that routes messages from an IBM i message queue to
 * configured destinations.
 *
 * Supports multi-instance operation via the {@code --instance=<name>} argument.
 * When no argument is supplied the "default" instance is used, preserving
 * backward compatibility with existing deployments.
 */
public class ManzanMainApp {

    public static void main(final String... _args) throws Exception {

        if (Arrays.asList(_args).contains("--version")) {
            printVersionInfo(_args);
            return;
        }

        // Check to make sure we use right java version on IBM i
        if (Config.isIBMi()) {
            String javaVendor = System.getProperty("java.vendor");
            if (!javaVendor.toLowerCase().contains("ibm")) {
                System.out.println("Java vendor: " + javaVendor);
                System.err.println("Error: This application requires Java provided by IBM");
                System.exit(1);
            }
        }

        // Legacy --configdir support (still honoured for backward compatibility)
        for (final String arg : _args) {
            if (arg.startsWith("--configdir=")) {
                System.setProperty(Config.DIRECTORY_OVERRIDE_PROPERTY, arg.replaceFirst("^[^=]+=", ""));
            }
        }

        // Resolve instance from --instance= arg or MANZAN_INSTANCE env var, defaulting
        // to "default".  validateInstanceName() is called inside fromEnvironment().
        final InstanceContext ctx = InstanceContext.fromEnvironment(_args);
        System.out.println("Starting instance: " + ctx.getInstanceName()
                + " (user: " + ctx.getUserProfile() + ")");

        // Prevent duplicate instances — fails fast if this instance is already running.
        final LockFile lock = LockFile.acquire(ctx.getInstanceName());
        if (lock == null) {
            System.err.println("Instance '" + ctx.getInstanceName()
                    + "' is already running. Exiting.");
            System.exit(1);
        }

        // Ensure config directory exists with 700 permissions before any config reads.
        ctx.ensureConfigDirectorySecurity();

        // --- Standard Camel startup ---

        final CamelContext context = new DefaultCamelContext();
        System.out.println("Apache Camel version " + context.getVersion());

        final AS400 as400 = ApplicationConfig.get(ctx).getRemoteConnection();
        as400.setGuiAvailable(false);
        as400.validateSignon();
        final AS400JDBCDataSource dataSource = new AS400JDBCDataSource(as400);
        dataSource.setTransactionIsolation("none");
        context.getRegistry().bind("jt400", dataSource);

        final DestinationConfig destinations = DestinationConfig.get(ctx);
        final Map<String, ManzanRoute> destinationRoutes = destinations.getRoutes(context);
        for (final Entry<String, ManzanRoute> dest : destinationRoutes.entrySet()) {
            context.addRoutes(dest.getValue());
        }

        final DataConfig dataSources = DataConfig.get(ctx, destinationRoutes.keySet());
        final Map<String, ManzanRoute> sourceRoutes = dataSources.getRoutes();
        for (final Entry<String, ManzanRoute> src : sourceRoutes.entrySet()) {
            context.addRoutes(src.getValue());
        }

        // Pre-start port conflict check — fail with a clear message rather than
        // letting Camel's netty component throw an opaque bind error.
        for (final ManzanRoute route : destinationRoutes.values()) {
            if (route instanceof PrometheusDestination) {
                assertPortAvailable(((PrometheusDestination) route).getPort(),
                        "Prometheus scrape endpoint (dests.ini). "
                        + "Set a unique 'port' for this instance.");
            }
        }
        for (final ManzanRoute route : sourceRoutes.values()) {
            if (route instanceof WatchMsgEventSockets) {
                assertPortAvailable(((WatchMsgEventSockets) route).getSocketPort(),
                        "watch event socket (app.ini [watch] socketPort). "
                        + "Set a unique 'socketPort' for this instance.");
            }
        }

        context.start();

        // Sleep the main thread until cancelled — Camel runs in secondary threads.
        Thread.sleep(Long.MAX_VALUE);
        context.stop();
        context.close();
    }

    /**
     * Attempt to bind the given port to confirm it is not already in use.
     * Throws {@link IllegalStateException} with an actionable message if the
     * port is occupied, so the operator knows which config key to change rather
     * than seeing a raw netty bind exception.
     */
    private static void assertPortAvailable(int port, String context) {
        try (ServerSocket s = new ServerSocket(port)) {
            // Port is free — close immediately and let Camel bind it at start.
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Port " + port + " is already in use. "
                    + "Another instance may be running on the same host with the same "
                    + context, e);
        }
    }

    private static void printVersionInfo(final String... _args) {
        System.out.println("");
        System.out.println("Distributor version information:");
        System.out.println("-------------------------------------------");
        System.out.println("    Version: " + Version.version);
        System.out.println("    Build date (UTC): " + Version.compileDateTime);
        System.out.println("");
        String library = null;
        try {
            // Use a throw-away default context for --version output — no lock needed.
            final InstanceContext ctx = InstanceContext.getDefault();
            library = ApplicationConfig.get(ctx).getLibrary();
            if (StringUtils.isEmpty(library)) {
                System.err.println("ERROR: Cannot locate handler component!!");
                System.exit(-1);
            }
            AS400 as400 = ApplicationConfig.get(ctx).getRemoteConnection();
            CommandCall cmd = new CommandCall(as400,
                    "QSYS/CALL PGM(" + library.trim() + "/handler) PARM('*VERSION' '*VERSION')");
            cmd.setMessageOption(AS400Message.MESSAGE_OPTION_ALL);
            boolean isSuccess = cmd.run();
            if (isSuccess) {
                AS400Message[] msgs = cmd.getMessageList();
                System.out.println("ILE Handler version information:");
                System.out.println("-------------------------------------------");
                for (AS400Message msg : msgs) {
                    if (StringUtils.isEmpty(msg.getID())) {
                        System.out.println("    " + msg.getText());
                    }
                }
                System.out.println("");
            } else {
                System.err.println("Unable to get handler component version info");
            }
            as400.disconnectAllServices();
        } catch (IOException e) {
            System.err.println("ERROR: Cannot locate handler component!!");
            e.printStackTrace();
            System.exit(-1);
        } catch (Exception e) {
            System.err.println("ERROR: Cannot get handler version information!!");
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
