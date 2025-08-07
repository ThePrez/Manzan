package CamelTests;

import com.github.theprez.manzan.routes.dest.StreamDestination;
import com.github.theprez.manzan.routes.event.WatchSql;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

public class SqlEventTest extends CamelTestHelper {
    @Test
    public void testHttpEndpoint() throws Exception {
        MockEndpoint mockOut = getMockEndpoint("mock:direct:" + testOutDest);
        mockOut.setResultWaitTime(5000); // give Camel up to 5 seconds
        mockOut.expectedMessageCount(1);
        List<String> expectedKeys =
                Arrays.asList("JOB_NAME", "JOB_USER", "SUBSYSTEM",
                        "JOB_STATUS", "CPU_TIME", "ELAPSED_CPU_PERCENTAGE",
                        "TOTAL_DISK_IO_COUNT", "ELAPSED_TIME", "TEMPORARY_STORAGE",
                        "MEMORY_POOL", "FUNCTION", "THREAD_COUNT", "FOO");
        expectBodyToHaveKeys(mockOut, expectedKeys);
        mockOut.assertIsSatisfied();
    }

    @Override
    protected RoutesBuilder[] createRouteBuilders() throws IOException {
        final int pollInterval = 1000;
        final String sqlEvent = "sqlEvent";
        final String sql = "SELECT JOB_NAME, JOB_USER, SUBSYSTEM, JOB_STATUS, CPU_TIME, ELAPSED_CPU_PERCENTAGE, TOTAL_DISK_IO_COUNT, ELAPSED_TIME, TEMPORARY_STORAGE, MEMORY_POOL, FUNCTION, THREAD_COUNT FROM TABLE(QSYS2.ACTIVE_JOB_INFO()) AS X WHERE ELAPSED_CPU_PERCENTAGE > 20 OR TOTAL_DISK_IO_COUNT > 100000 ORDER BY ELAPSED_CPU_PERCENTAGE DESC FETCH FIRST 20 ROWS ONLY";

        final String format = null;
        final Map<String, String> headerParams = new LinkedHashMap();
        headerParams.put("authorization", "bearer eyt");

        dataMapInjections.put("FOO", "BAR");
        destinations.add(testOutDest);

        return new RoutesBuilder[]{
                new WatchSql(sqlEvent, sql, format, destinations, pollInterval, dataMapInjections),
                new StreamDestination(context, testOutDest, null, componentOptions)
        };
    }
}

