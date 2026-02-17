package com.github.theprez.manzan;

import java.io.IOException;
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
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400JDBCDataSource;
import com.ibm.as400.access.AS400Message;
import com.ibm.as400.access.CommandCall;

/**
 * A Camel Application that routes messages from an IBM i message queue to
 * email.
 */
public class ManzanMainApp {

    public static void main(final String... _args) throws Exception {

        if (Arrays.asList(_args).contains("--version")) {
            printVersionInfo();
            return;
        }

        // Check to make sure we use right java version on IBM i
        if (Config.isIBMi()){
            // Get the Java vendor property
            String javaVendor = System.getProperty("java.vendor");
            // Check if the vendor is IBM
            if (!javaVendor.toLowerCase().contains("ibm")) {
                System.out.println("Java vendor: " + javaVendor);
                System.err.println("Error: This application requires Java provided by IBM");
                System.exit(1);
            }
        }

        for(final String arg:_args) {
            if(arg.startsWith("--configdir=")) {
                System.setProperty(Config.DIRECTORY_OVERRIDE_PROPERTY, arg.replaceFirst("^[^=]+=", ""));
            }
        }

        // Standard for a Camel deployment. Start by getting a CamelContext object.
        final CamelContext context = new DefaultCamelContext();
        System.out.println("Apache Camel version " + context.getVersion());

        final AS400 as400 = ApplicationConfig.get().getRemoteConnection();
        as400.setGuiAvailable(false);
        as400.validateSignon();
        final AS400JDBCDataSource dataSource = new AS400JDBCDataSource(as400);
        dataSource.setTransactionIsolation("none");
        context.getRegistry().bind("jt400", dataSource);

        final DestinationConfig destinations = DestinationConfig.get();
        final Map<String, ManzanRoute> destinationRoutes = destinations.getRoutes(context);
        for (final Entry<String, ManzanRoute> dest : destinationRoutes.entrySet()) {
            context.addRoutes(dest.getValue());
        }

        final DataConfig dataSources = DataConfig.get(destinationRoutes.keySet());
        for (final Entry<String, ManzanRoute> src : dataSources.getRoutes().entrySet()) {
            context.addRoutes(src.getValue());
        }

        // This actually "starts" the route, so Camel will start monitoring and routing
        // activity here.
        context.start();

        // Since this program is designed to just run forever (until user cancel), we
        // can just sleep the
        // main thread. Camel's work will happen in secondary threads.
        Thread.sleep(Long.MAX_VALUE);
        context.stop();
        context.close();
    }

    private static void printVersionInfo() {
        System.out.println("");
        System.out.println("Distributor version information:");
        System.out.println("-------------------------------------------");
        System.out.println("    Version: " + Version.version);
        System.out.println("    Build date (UTC): " + Version.compileDateTime);
        System.out.println("");
        String library = null;
        try {
            library = ApplicationConfig.get().getLibrary();
        } catch (IOException e) {
            System.err.println("ERROR: Cannot locate handler component!!");
            e.printStackTrace();
            System.exit(-1);
        }
        if (StringUtils.isEmpty(library)) {
            System.err.println("ERROR: Cannot locate handler component!!");
            System.exit(-1);
        }
        try {
            AS400 as400 = ApplicationConfig.get().getRemoteConnection();
            CommandCall cmd = new CommandCall(as400,
                    "CALL PGM(" + library.trim() + "/handler) PARM('*VERSION' '*VERSION')");

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
        } catch (Exception e) {
            System.err.println("ERROR: Cannot get handler version information!!");
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
