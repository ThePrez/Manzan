package com.github.theprez.manzan;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;

import com.github.theprez.manzan.configuration.DataConfig;
import com.github.theprez.manzan.configuration.DestinationConfig;
import com.github.theprez.manzan.routes.ManzanRoute;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400JDBCDataSource;

/**
 * A Camel Application that routes messages from an IBM i message queue to
 * email.
 */
public class ManzanMainApp {

    public static void main(final String... args) throws Exception {
        // Standard for a Camel deployment. Start by getting a CamelContext object.
        final CamelContext context = new DefaultCamelContext();
        System.out.println("Apache Camel version " + context.getVersion());

        final AS400 as400 = IBMiConnectionSupplier.getSystemConnection();
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
}
