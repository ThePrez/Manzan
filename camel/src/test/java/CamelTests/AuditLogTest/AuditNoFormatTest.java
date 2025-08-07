package CamelTests.AuditLogTest;

import CamelTests.CamelTestHelper;
import com.github.theprez.manzan.routes.dest.StreamDestination;
import com.github.theprez.manzan.routes.event.AuditLog;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.ini4j.Ini;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

public class AuditNoFormatTest extends CamelTestHelper {
    @Override
    public String isMockEndpoints() {
        String[] mockEndpoints = new String[]{"direct:" + testOutDest};
        return String.join(",", mockEndpoints);
    }

    @Test
    public void testWithoutFormat() throws Exception {
        Ini ini = readIni();
        String hostname = ini.get("remote", "system");
        String fakeUser = "fakeUser";
        String fakePassword = "fakePassword";
        MockEndpoint mockOut = getMockEndpoint("mock:direct:" + testOutDest);
        boolean loginFailed = attemptInvalidLogin(hostname, fakeUser, fakePassword);
        if (loginFailed) {
            mockOut.setResultWaitTime(5000); // give Camel up to 5 seconds
            mockOut.setMinimumExpectedMessageCount(1);
            List<String> expectedKeys = Arrays.asList(
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
            );
            expectBodyToHaveKeys(mockOut, expectedKeys);
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

        dataMapInjections.put("FOO", "BAR");
        destinations.add(testOutDest);

        return new RoutesBuilder[]{
                new AuditLog(routeNameNoFormat, null, destinations, pollInterval, numToProcess, auditType, fallbackStartTime, dataMapInjections),
                new StreamDestination(context, testOutDest, null, componentOptions)
        };
    }
}

