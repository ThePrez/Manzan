package CamelTests.AuditLogTest;

import CamelTests.CamelTestHelper;
import com.github.theprez.manzan.routes.dest.StreamDestination;
import com.github.theprez.manzan.routes.event.AuditLog;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.ini4j.Ini;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class AuditWithFormatTest extends CamelTestHelper {
    @Test
    public void testWithFormat() throws Exception {
        Ini ini = readIni();
        String hostname = ini.get("remote", "system");
        String fakeUser = "fakeUser";
        String fakePassword = "fakePassword";

        MockEndpoint mockOut = getMockEndpoint("mock:direct:" + testOutDest);
        boolean loginFailed = attemptInvalidLogin(hostname, fakeUser, fakePassword);
        if (loginFailed) {
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

        destinations.add(testOutDest);

        return new RoutesBuilder[]{
                new AuditLog(routeName, format, destinations, pollInterval, numToProcess, auditType, fallbackStartTime, dataMapInjections),
                new StreamDestination(context, testOutDest, null, componentOptions)
        };
    }
}

