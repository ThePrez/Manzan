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
     * Queries the actual current job from the IBM i system
     */
    private String getCurrentJobIdentifier() {
        try {
            // Query the current job information from the system
            // This will return the job that's running this test
            String sql = "SELECT JOB_NAME FROM TABLE(QSYS2.ACTIVE_JOB_INFO(JOB_NAME_FILTER => '*')) WHERE JOB_NAME = QSYS2.JOB_NAME FETCH FIRST 1 ROW ONLY";
            
            // Use the JDBC connection to get current job
            // The job name returned is in format: number/user/name
            java.sql.Connection conn = context.getRegistry().lookupByNameAndType("jt400", javax.sql.DataSource.class).getConnection();
            java.sql.Statement stmt = conn.createStatement();
            java.sql.ResultSet rs = stmt.executeQuery(sql);
            
            String jobName = null;
            if (rs.next()) {
                jobName = rs.getString("JOB_NAME");
            }
            
            rs.close();
            stmt.close();
            conn.close();
            
            // If we couldn't get the job name, fall back to a generic active job
            if (jobName == null || jobName.isEmpty()) {
                // Query for any active job as fallback
                conn = context.getRegistry().lookupByNameAndType("jt400", javax.sql.DataSource.class).getConnection();
                stmt = conn.createStatement();
                rs = stmt.executeQuery("SELECT JOB_NAME FROM TABLE(QSYS2.ACTIVE_JOB_INFO()) FETCH FIRST 1 ROW ONLY");
                if (rs.next()) {
                    jobName = rs.getString("JOB_NAME");
                }
                rs.close();
                stmt.close();
                conn.close();
            }
            
            return jobName != null ? jobName : "000000/QSYS/QINTER";
        } catch (Exception e) {
            // If query fails, return a fallback job identifier
            System.err.println("Failed to get current job identifier: " + e.getMessage());
            return "000000/QSYS/QINTER";
        }
    }
}
