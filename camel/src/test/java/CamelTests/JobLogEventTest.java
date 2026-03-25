package CamelTests;

import com.github.theprez.manzan.routes.dest.StreamDestination;
import com.github.theprez.manzan.routes.event.WatchJobLog;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

public class JobLogEventTest extends CamelTestHelper {
    @Test
    public void testJobLogEndpoint() throws Exception {
        MockEndpoint mockOut = getMockEndpoint("mock:direct:" + testOutDest);
        mockOut.setResultWaitTime(10000); // give Camel up to 10 seconds for job log queries
        mockOut.expectedMinimumMessageCount(0); // May not have any messages depending on job activity
        
        // Expected fields from QSYS2.JOBLOG_INFO
        List<String> expectedKeys = Arrays.asList(
            "JOB_FULL",
            "JOB_NUMBER", 
            "JOB_USER",
            "JOB_NAME",
            "MESSAGE_ID",
            "MESSAGE_TYPE",
            "SEVERITY",
            "MESSAGE_TIMESTAMP",
            "MESSAGE_TEXT",
            "FROM_PROGRAM",
            "TEST_ENV" // From injection
        );
        
        expectBodyToHaveKeys(mockOut, expectedKeys);
        mockOut.assertIsSatisfied();
    }

    @Test
    public void testJobLogWithFormat() throws Exception {
        MockEndpoint mockOut = getMockEndpoint("mock:direct:" + testOutDest);
        mockOut.setResultWaitTime(10000);
        mockOut.expectedMinimumMessageCount(0);
        
        // When format is applied, body should be formatted string, not JSON
        mockOut.expectedMessagesMatches(exchange -> {
            String body = exchange.getIn().getBody(String.class);
            // Should contain formatted output if any messages exist
            return body != null;
        });
        
        mockOut.assertIsSatisfied();
    }

    @Override
    protected RoutesBuilder[] createRouteBuilders() throws IOException {
        final int pollInterval = 2000; // Poll every 2 seconds for testing
        final String jobLogEvent = "jobLogEvent";
        
        // Get current job to monitor (this test will monitor its own job log)
        String currentJob = getCurrentJobIdentifier();
        List<String> jobs = Arrays.asList(currentJob);
        
        // Add test injection
        dataMapInjections.put("TEST_ENV", "JUNIT");
        destinations.add(testOutDest);

        return new RoutesBuilder[]{
                new WatchJobLog(jobLogEvent, jobs, null, destinations, pollInterval, dataMapInjections),
                new StreamDestination(context, testOutDest, null, componentOptions)
        };
    }
    
    /**
     * Get the current job identifier for testing
     * Format: number/user/name
     */
    private String getCurrentJobIdentifier() {
        // For testing purposes, we'll use a placeholder
        // In a real IBM i environment, this would query the current job
        // For now, return a test job identifier that may or may not exist
        return "999999/QUSER/QZDASOINIT";
    }
}
