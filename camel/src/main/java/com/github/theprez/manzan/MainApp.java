package com.github.theprez.manzan;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;

import com.github.theprez.manzan.configuration.DestinationConfig;
import com.github.theprez.manzan.routes.ManzanRoute;
import com.github.theprez.manzan.routes.event.WatchMsgEvent;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400JDBCDataSource;

/**
 * A Camel Application that routes messages from an IBM i message queue to
 * email.
 */
public class MainApp {
    private static AS400 getSystemConnection() {
        return new AS400("oss73dev.rch.stglabs.ibm.com", "linux", "linux1");
    }

    public static void main(final String... args) throws Exception {
        // Standard for a Camel deployment. Start by getting a CamelContext object.
        final CamelContext context = new DefaultCamelContext();
        System.out.println("Apache Camel version " + context.getVersion());

        final AS400 as400 = getSystemConnection();
        as400.setGuiAvailable(false);
        as400.validateSignon();
        final AS400JDBCDataSource dataSource = new AS400JDBCDataSource(as400);
        dataSource.setTransactionIsolation("none");
        context.getRegistry().bind("jt400", dataSource);

        final DestinationConfig destinations = new DestinationConfig(new File("dests.ini"));
        for (final Entry<String, ManzanRoute> dest : destinations.getRoutes().entrySet()) {
            context.addRoutes(dest.getValue());
        }
        // testing only
        final List<String> testdests = new LinkedList<String>();
        testdests.add("test_out");
        testdests.add("mysentry");
        context.addRoutes(new WatchMsgEvent("JESSE", testdests, "JESSEG", 5, 1000));

        // OLD SECTION
        // final ManzanConfig config = new ManzanConfig();
        // final ManzanRouteMaster master = new ManzanRouteMaster(config, context);

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
}
