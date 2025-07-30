package CamelTests.AuditLogTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.theprez.manzan.configuration.ApplicationConfig;
import com.github.theprez.manzan.routes.dest.StreamDestination;
import com.ibm.as400.access.AS400JDBCDataSource;
import org.apache.camel.CamelContext;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;
import com.github.theprez.manzan.routes.event.AuditLog;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.util.*;

import com.ibm.as400.access.AS400;
import com.ibm.as400.access.AS400SecurityException;

import java.util.concurrent.TimeUnit;

public class AuditNoFormatTest extends CamelTestSupport {
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
    public void testWithoutFormat() throws Exception {
        String hostname = "p8adt05.rch.stglabs.ibm.com";
        String fakeUser = "fakeUser";
        String fakePassword = "fakePassword";
        MockEndpoint mockOut = getMockEndpoint("mock:direct:" + testOutDest);
        boolean loginFailed = attemptInvalidLogin(hostname, fakeUser, fakePassword);
        if (loginFailed){
            mockOut.setResultWaitTime(5000); // give Camel up to 5 seconds
            mockOut.setMinimumExpectedMessageCount(1);
            mockOut.expectedMessagesMatches(exchange -> {
                try {
                    String body = exchange.getIn().getBody(String.class);
                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, Object> json = mapper.readValue(body, Map.class);

                    Set<String> expectedKeys = new HashSet<>(Arrays.asList(
                            "ENTRY_TIMESTAMP", "SEQUENCE_NUMBER", "USER_NAME", "QUALIFIED_JOB_NAME",
                            "JOB_NAME", "JOB_USER", "JOB_NUMBER", "THREAD", "PROGRAM_LIBRARY",
                            "PROGRAM_NAME", "PROGRAM_LIBRARY_ASP_DEVICE", "PROGRAM_LIBRARY_ASP_NUMBER",
                            "REMOTE_PORT", "REMOTE_ADDRESS", "SYSTEM_NAME", "SYSTEM_SEQUENCE_NUMBER",
                            "RECEIVER_LIBRARY", "RECEIVER_NAME", "RECEIVER_ASP_DEVICE", "RECEIVER_ASP_NUMBER",
                            "ARM_NUMBER", "VIOLATION_TYPE", "VIOLATION_TYPE_DETAIL", "AUDIT_USER_NAME",
                            "DEVICE_NAME", "INTERFACE_NAME", "REMOTE_LOCATION", "LOCAL_LOCATION",
                            "NETWORK_ID", "DECRYPT_HOST_VARIABLE", "DECRYPT_OBJECT_LIBRARY",
                            "DECRYPT_OBJECT_NAME", "DECRYPT_OBJECT_TYPE", "DECRYPT_OBJECT_ASP_NAME",
                            "DECRYPT_OBJECT_ASP_NUMBER", "FOO"
                    ));

                    return json.keySet().containsAll(expectedKeys);
                } catch (Exception e) {
                    return false;
                }
            });
            mockOut.assertIsSatisfied();
        }
    }


    @Override
    protected RoutesBuilder[] createRouteBuilders() throws IOException {
        final int pollInterval = 1000;
        final int numToProcess = 1000;
        final int fallbackStartTime = 1;
        final String routeNameNoFormat = "auditNoFormat";

        final String auditType = "PASSWORD";

        LinkedList<String> destinations = new LinkedList<>();
        Map<String, String> dataMapInjections = new HashMap<>();
        dataMapInjections.put("FOO", "BAR");
        Map<String, String> componentOptions = new HashMap<>();

        destinations.add(testOutDest);

        return new RoutesBuilder[]{
                new AuditLog(routeNameNoFormat, null, destinations, pollInterval, numToProcess, auditType, fallbackStartTime, dataMapInjections),
                new StreamDestination(context, testOutDest, null, componentOptions)
        };
    }
}

