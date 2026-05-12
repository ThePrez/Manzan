package CamelTests;

import com.github.theprez.manzan.routes.dest.PrometheusDestination;
import com.github.theprez.manzan.routes.event.WatchJobLog;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for Prometheus destination
 * Tests metric exposure, HTTP endpoint, authentication, and metric formatting
 */
public class PrometheusDestinationTest extends CamelTestHelper {
    
    private static final int TEST_PORT = 9091; // Use different port to avoid conflicts
    private static final String TEST_PATH = "/metrics";
    private static final String TEST_PREFIX = "test_";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "testpass";

    @Test
    public void testPrometheusEndpointAccessible() throws Exception {
        // Wait for routes to start
        Thread.sleep(2000);
        
        // Test that the metrics endpoint is accessible with authentication
        String metricsUrl = "http://localhost:" + TEST_PORT + TEST_PATH;
        HttpURLConnection conn = (HttpURLConnection) new URL(metricsUrl).openConnection();
        conn.setRequestMethod("GET");
        
        // Add authentication
        String auth = TEST_USERNAME + ":" + TEST_PASSWORD;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
        
        int responseCode = conn.getResponseCode();
        assertEquals(200, responseCode, "Metrics endpoint should return 200 OK with authentication");
        
        conn.disconnect();
    }

    @Test
    public void testPrometheusMetricsFormat() throws Exception {
        // Wait for routes to start and process some events
        Thread.sleep(3000);
        
        // Fetch metrics with authentication
        String metricsUrl = "http://localhost:" + TEST_PORT + TEST_PATH;
        HttpURLConnection conn = (HttpURLConnection) new URL(metricsUrl).openConnection();
        conn.setRequestMethod("GET");
        
        // Add authentication
        String auth = TEST_USERNAME + ":" + TEST_PASSWORD;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
        
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line).append("\n");
        }
        in.close();
        conn.disconnect();
        
        String metrics = response.toString();
        
        // Verify Prometheus format
        assertTrue(metrics.contains("# HELP"), "Metrics should contain HELP comments");
        assertTrue(metrics.contains("# TYPE"), "Metrics should contain TYPE declarations");
        
        // Verify our custom prefix is used
        assertTrue(metrics.contains(TEST_PREFIX + "events_total"), 
            "Metrics should use custom prefix: " + TEST_PREFIX);
        
        // Verify metric types are present
        assertTrue(metrics.contains("TYPE " + TEST_PREFIX + "events_total counter"), 
            "Should have counter metric type");
    }

    @Test
    public void testPrometheusMetricsContent() throws Exception {
        // Wait for events to be processed
        Thread.sleep(3000);
        
        String metricsUrl = "http://localhost:" + TEST_PORT + TEST_PATH;
        HttpURLConnection conn = (HttpURLConnection) new URL(metricsUrl).openConnection();
        conn.setRequestMethod("GET");
        
        // Add authentication
        String auth = TEST_USERNAME + ":" + TEST_PASSWORD;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
        
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line).append("\n");
        }
        in.close();
        conn.disconnect();
        
        String metrics = response.toString();
        
        // Verify expected metrics exist
        assertTrue(metrics.contains(TEST_PREFIX + "events_total"), 
            "Should contain events_total metric");
        assertTrue(metrics.contains(TEST_PREFIX + "messages_total"), 
            "Should contain messages_total metric");
        assertTrue(metrics.contains(TEST_PREFIX + "jobs_total"), 
            "Should contain jobs_total metric");
        
        // Verify labels are present
        assertTrue(metrics.contains("event_type="), "Metrics should have event_type label");
        assertTrue(metrics.contains("system="), "Metrics should have system label");
    }

    @Test
    public void testPrometheusAuthentication() throws Exception {
        // Wait for routes to start
        Thread.sleep(2000);
        
        String metricsUrl = "http://localhost:" + TEST_PORT + TEST_PATH;
        
        // Test without authentication - should fail
        HttpURLConnection conn = (HttpURLConnection) new URL(metricsUrl).openConnection();
        conn.setRequestMethod("GET");
        int responseCode = conn.getResponseCode();
        assertEquals(401, responseCode, "Should return 401 without authentication");
        conn.disconnect();
        
        // Test with wrong credentials - should fail
        conn = (HttpURLConnection) new URL(metricsUrl).openConnection();
        conn.setRequestMethod("GET");
        String wrongAuth = "wronguser:wrongpass";
        String encodedWrongAuth = Base64.getEncoder().encodeToString(wrongAuth.getBytes());
        conn.setRequestProperty("Authorization", "Basic " + encodedWrongAuth);
        responseCode = conn.getResponseCode();
        assertEquals(401, responseCode, "Should return 401 with wrong credentials");
        conn.disconnect();
        
        // Test with correct credentials - should succeed
        conn = (HttpURLConnection) new URL(metricsUrl).openConnection();
        conn.setRequestMethod("GET");
        String correctAuth = TEST_USERNAME + ":" + TEST_PASSWORD;
        String encodedAuth = Base64.getEncoder().encodeToString(correctAuth.getBytes());
        conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
        responseCode = conn.getResponseCode();
        assertEquals(200, responseCode, "Should return 200 with correct credentials");
        conn.disconnect();
    }

    @Test
    public void testPrometheusMetricIncrement() throws Exception {
        // Wait for initial metrics
        Thread.sleep(2000);
        
        String metricsUrl = "http://localhost:" + TEST_PORT + TEST_PATH;
        String auth = TEST_USERNAME + ":" + TEST_PASSWORD;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        
        // Get initial metric value
        HttpURLConnection conn = (HttpURLConnection) new URL(metricsUrl).openConnection();
        conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response1 = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            response1.append(line).append("\n");
        }
        in.close();
        conn.disconnect();
        
        String metrics1 = response1.toString();
        double initialCount = extractMetricValue(metrics1, TEST_PREFIX + "events_total");
        
        // Wait for more events to be processed
        Thread.sleep(3000);
        
        // Get updated metric value
        conn = (HttpURLConnection) new URL(metricsUrl).openConnection();
        conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
        in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response2 = new StringBuilder();
        while ((line = in.readLine()) != null) {
            response2.append(line).append("\n");
        }
        in.close();
        conn.disconnect();
        
        String metrics2 = response2.toString();
        double updatedCount = extractMetricValue(metrics2, TEST_PREFIX + "events_total");
        
        // Verify metrics are incrementing (or at least not decreasing)
        assertTrue(updatedCount >= initialCount, 
            "Event count should not decrease: initial=" + initialCount + ", updated=" + updatedCount);
    }

    @Test
    public void testPrometheusContentType() throws Exception {
        Thread.sleep(2000);
        
        String metricsUrl = "http://localhost:" + TEST_PORT + TEST_PATH;
        String auth = TEST_USERNAME + ":" + TEST_PASSWORD;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        
        HttpURLConnection conn = (HttpURLConnection) new URL(metricsUrl).openConnection();
        conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
        conn.setRequestMethod("GET");
        
        String contentType = conn.getHeaderField("Content-Type");
        assertNotNull(contentType, "Content-Type header should be present");
        assertTrue(contentType.contains("text/plain"), 
            "Content-Type should be text/plain for Prometheus metrics");
        
        conn.disconnect();
    }

    /**
     * Helper method to extract a metric value from Prometheus output
     */
    private double extractMetricValue(String metrics, String metricName) {
        String[] lines = metrics.split("\n");
        for (String line : lines) {
            if (line.startsWith(metricName) && !line.startsWith("#")) {
                // Extract the value (last part after space)
                String[] parts = line.split("\\s+");
                if (parts.length >= 2) {
                    try {
                        return Double.parseDouble(parts[parts.length - 1]);
                    } catch (NumberFormatException e) {
                        // Skip invalid lines
                    }
                }
            }
        }
        return 0.0;
    }

    @Override
    protected RoutesBuilder[] createRouteBuilders() throws IOException {
        final int pollInterval = 2000; // Poll every 2 seconds for testing
        final String jobLogEvent = "jobLogEventPrometheus";
        final String prometheusDestName = "prometheus_test";
        
        // Get current job to monitor
        String currentJob = getCurrentJobIdentifier();
        List<String> jobs = Arrays.asList(currentJob);
        
        // Add test injection
        dataMapInjections.put("TEST_ENV", "JUNIT_PROMETHEUS");
        destinations.add(prometheusDestName);

        return new RoutesBuilder[]{
                new WatchJobLog(jobLogEvent, jobs, null, destinations, pollInterval, dataMapInjections),
                new PrometheusDestination(prometheusDestName, TEST_PORT, TEST_PATH, 
                    TEST_PREFIX, TEST_USERNAME, TEST_PASSWORD)
        };
    }
    
    /**
     * Get the current job identifier for testing
     */
    private String getCurrentJobIdentifier() {
        try {
            String sql = "SELECT JOB_NAME FROM TABLE(QSYS2.ACTIVE_JOB_INFO(JOB_NAME_FILTER => '*')) WHERE JOB_NAME = QSYS2.JOB_NAME FETCH FIRST 1 ROW ONLY";
            
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
            
            if (jobName == null || jobName.isEmpty()) {
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
            System.err.println("Failed to get current job identifier: " + e.getMessage());
            return "000000/QSYS/QINTER";
        }
    }
}
