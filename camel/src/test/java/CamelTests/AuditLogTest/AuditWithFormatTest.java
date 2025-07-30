package CamelTests.AuditLogTest;

import com.github.theprez.manzan.configuration.ApplicationConfig;
import com.github.theprez.manzan.routes.dest.StreamDestination;
import com.ibm.as400.access.AS400JDBCDataSource;
import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;
import com.github.theprez.manzan.routes.event.AuditLog;
import com.github.theprez.manzan.routes.event.AuditType;

import java.beans.PropertyVetoException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ConnectionDroppedException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class AuditWithFormatTest extends CamelTestSupport {
    final String testOutDest = "test_out";

    @Override
    protected CamelContext createCamelContext() throws Exception {
        CamelContext context = super.createCamelContext();

        final AS400 as400 = ApplicationConfig.get().getRemoteConnection();
        as400.setGuiAvailable(false);
        as400.validateSignon();

        final AS400JDBCDataSource dataSource = new AS400JDBCDataSource(as400);
        dataSource.setTransactionIsolation("none");

        // Register the dataSource with the correct Camel registry
        context.getRegistry().bind("jt400", dataSource);

        return context;
    }

    @Override
    public String isMockEndpoints() {
        String[] mockEndpoints = new String[]{"direct:" + testOutDest};
        return String.join(",", mockEndpoints);
    }

    private boolean attemptInvalidLogin(String hostname, String username, String password) throws PropertyVetoException {
        AS400 system = new AS400(hostname, username, password);
        system.setGuiAvailable(false); // ✅ Disable GUI prompt

        try {
            system.connectService(AS400.COMMAND); // Try connecting to the COMMAND service
            System.out.println("Login unexpectedly succeeded.");
            return false;
        }
        catch (AS400SecurityException e) {
            System.out.println("Login failed: Invalid credentials.");
        }
        catch (Exception e) {
            System.out.println("Login failed: IO or connection error - " + e.getMessage());
        } finally {
            if (system.isConnected()) {
                system.disconnectAllServices();
            }
        }
        return true;
    }

    @Test
    public void testWithFormat() throws Exception {
        String hostname = "p8adt05.rch.stglabs.ibm.com";
        String fakeUser = "fakeUser";
        String fakePassword = "fakePassword";
        MockEndpoint mockOut = getMockEndpoint("mock:direct:" + testOutDest);
        boolean loginFailed = attemptInvalidLogin(hostname, fakeUser, fakePassword);
        if (loginFailed){
            mockOut.setResultWaitTime(5000); // give Camel up to 5 seconds
            mockOut.setMinimumExpectedMessageCount(1);
            mockOut.expectedMessagesMatches(exchange ->
                    exchange.getIn().getBody(String.class).contains("Violation type: User name not valid username: FAKEUSER remote ip:")
            );
            mockOut.assertIsSatisfied();
        }
        context.getRouteController().stopRoute("audit", 1, TimeUnit.SECONDS, false);
    }

    @Override
    protected RoutesBuilder[] createRouteBuilders() throws IOException {
        final int pollInterval = 1000;
        final int numToProcess = 1000;
        final int fallbackStartTime = 1;
        final String routeName = "audit";

        final String auditType = "PASSWORD";
        final String format = "Violation type: $VIOLATION_TYPE_DETAIL$ username: $AUDIT_USER_NAME$ remote ip: $REMOTE_ADDRESS$:$REMOTE_PORT$";

        LinkedList<String> destinations = new LinkedList<>();
        Map<String, String> dataMapInjections = new HashMap<>();
        Map<String, String> componentOptions = new HashMap<>();

        destinations.add(testOutDest);

        return new RoutesBuilder[]{
                new AuditLog(routeName, format, destinations, pollInterval, numToProcess, auditType, fallbackStartTime, dataMapInjections),
                new StreamDestination(context, testOutDest, null, componentOptions)
        };
    }
}

