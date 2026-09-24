package CamelTests;

import com.github.theprez.manzan.ManzanEventType;
import com.github.theprez.manzan.routes.dest.OpenTelemetryDestination;
import com.github.theprez.manzan.routes.dest.StreamDestination;
import com.github.theprez.manzan.routes.event.FileEvent;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.camel.Exchange;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link OpenTelemetryDestination}.
 *
 * <p>A {@link MockWebServer} stands in for the OTLP collector, following the same
 * pattern as {@link PrometheusDestinationTest} where a live HTTP server is the
 * assertion target. Each test verifies:
 * <ul>
 *   <li>The correct OTLP endpoint path is called</li>
 *   <li>The {@code Content-Type} header is the OTLP protobuf type</li>
 *   <li>Auth headers injected via config arrive at the server</li>
 *   <li>The SDK batch processor flushes within the configured timeout</li>
 * </ul>
 *
 * <p>Tests that require no live server exercise construction-time behaviour:
 * credential validation, errorRegex compilation, and mTLS guard.
 */
public class OpenTelemetryDestinationTest extends CamelTestHelper {

    private MockWebServer server;

    // Endpoint path that OtlpHttpLogRecordExporter posts to by default
    private static final String OTLP_LOGS_PATH = "/v1/logs";
    private static final String OTLP_CONTENT_TYPE = "application/x-protobuf";

    // Short batch settings so tests don't wait unnecessarily
    private static final int BATCH_SIZE      = 1;
    private static final int BATCH_TIMEOUT   = 500;
    private static final int REQUEST_TIMEOUT = 5000;

    private static final String TEST_DEST  = "otlp_test";
    private static final String FILE_EVENT = "otlpFileEvent";

    Path testFile;

    @Override
    protected void doPreSetup() throws Exception {
        // Start the mock OTLP collector
        server = new MockWebServer();
        server.start();

        // Prepare a temp file that the FileEvent source will watch
        testFile = Files.createTempFile("manzan-otlp-test-", ".txt");
        Files.write(testFile, new byte[0], StandardOpenOption.TRUNCATE_EXISTING);
    }

    @AfterEach
    public void tearDownServer() throws IOException {
        if (server != null) {
            server.shutdown();
        }
        if (testFile != null) {
            Files.deleteIfExists(testFile);
        }
    }

    // -------------------------------------------------------------------------
    // Route integration tests (MockWebServer as OTLP backend)
    // -------------------------------------------------------------------------

    @Test
    public void testOtlpRequestReachesCollector() throws Exception {
        // Enqueue a 200 OK for the SDK exporter to receive
        server.enqueue(new MockResponse().setResponseCode(200).setBody(""));

        // Trigger an event by appending content to the watched file
        try (BufferedWriter w = new BufferedWriter(new FileWriter(testFile.toFile()))) {
            w.write("hello from manzan");
        }

        // Wait for the SDK batch processor to flush (batch size = 1, timeout = 500 ms)
        RecordedRequest request = server.takeRequest(10, TimeUnit.SECONDS);

        assertNotNull(request,
                "OTLP exporter should have posted a request to the mock collector within 10 s");
        assertEquals(OTLP_LOGS_PATH, request.getPath(),
                "Request path should be the OTLP logs endpoint");
    }

    @Test
    public void testOtlpRequestContentType() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(""));

        try (BufferedWriter w = new BufferedWriter(new FileWriter(testFile.toFile()))) {
            w.write("content type check");
        }

        RecordedRequest request = server.takeRequest(10, TimeUnit.SECONDS);
        assertNotNull(request, "Expected a request to the mock collector");

        String contentType = request.getHeader("Content-Type");
        assertNotNull(contentType, "Content-Type header must be present on OTLP requests");
        assertTrue(contentType.contains(OTLP_CONTENT_TYPE),
                "Content-Type should be application/x-protobuf, got: " + contentType);
    }

    @Test
    public void testOtlpAuthHeaderForwardedToCollector() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(""));

        try (BufferedWriter w = new BufferedWriter(new FileWriter(testFile.toFile()))) {
            w.write("auth header test");
        }

        RecordedRequest request = server.takeRequest(10, TimeUnit.SECONDS);
        assertNotNull(request, "Expected a request to the mock collector");

        // The destination was constructed with Authorization: Bearer test-token
        String authHeader = request.getHeader("Authorization");
        assertEquals("Bearer test-token", authHeader,
                "Authorization header should be forwarded verbatim to the collector");
    }

    @Test
    public void testOtlpRequestIsHttpPost() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(""));

        try (BufferedWriter w = new BufferedWriter(new FileWriter(testFile.toFile()))) {
            w.write("method check");
        }

        RecordedRequest request = server.takeRequest(10, TimeUnit.SECONDS);
        assertNotNull(request, "Expected a request to the mock collector");
        assertEquals("POST", request.getMethod(),
                "OTLP exporter must use HTTP POST");
    }

    @Test
    public void testOtlpRequestBodyIsNonEmpty() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(""));

        try (BufferedWriter w = new BufferedWriter(new FileWriter(testFile.toFile()))) {
            w.write("body size check");
        }

        RecordedRequest request = server.takeRequest(10, TimeUnit.SECONDS);
        assertNotNull(request, "Expected a request to the mock collector");
        assertTrue(request.getBodySize() > 0,
                "OTLP request body must not be empty — it should contain serialised LogRecords");
    }

    @Test
    public void testOtlpRetryOnServerError() throws Exception {
        // First response: 503 (simulate collector unavailable)
        // Second response: 200 (collector recovers)
        server.enqueue(new MockResponse().setResponseCode(503).setBody(""));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(""));

        try (BufferedWriter w = new BufferedWriter(new FileWriter(testFile.toFile()))) {
            w.write("retry test");
        }

        // SDK exporter retries — allow extra time
        RecordedRequest first = server.takeRequest(10, TimeUnit.SECONDS);
        assertNotNull(first, "First request (503) should still arrive at the server");

        RecordedRequest retry = server.takeRequest(15, TimeUnit.SECONDS);
        assertNotNull(retry, "SDK exporter should retry after a 503 response");
        assertEquals(OTLP_LOGS_PATH, retry.getPath(),
                "Retry should target the same endpoint");
    }

    // -------------------------------------------------------------------------
    // Construction-time validation tests (no server needed)
    // -------------------------------------------------------------------------

    @Test
    public void testConstructorRejectsClientCertWithoutClientKey() {
        assertThrows(IllegalArgumentException.class, () ->
                new OpenTelemetryDestination(
                        null,
                        "bad-mtls",
                        "http://localhost:4318/v1/logs",
                        null,
                        null,
                        "/some/cert.crt",   // clientCertPath set
                        null,               // clientKeyPath missing
                        1, 500, 5000,
                        null),
                "Providing clientCertPath without clientKeyPath must throw at construction");
    }

    @Test
    public void testConstructorRejectsMissingCertFile() {
        assertThrows(IllegalArgumentException.class, () ->
                new OpenTelemetryDestination(
                        null,
                        "bad-cert",
                        "http://localhost:4318/v1/logs",
                        null,
                        "/this/path/does/not/exist.crt",  // caCertPath that does not exist
                        null,
                        null,
                        1, 500, 5000,
                        null),
                "A caCertPath pointing to a non-existent file must throw at construction");
    }

    @Test
    public void testInvalidErrorRegexIsHandledGracefully() {
        // An invalid regex must not throw — the class logs a warning and disables the pattern
        assertDoesNotThrow(() ->
                new OpenTelemetryDestination(
                        null,
                        "bad-regex",
                        "http://localhost:4318/v1/logs",
                        null, null, null, null,
                        1, 500, 5000,
                        "[invalid regex"),
                "An invalid errorRegex must not prevent the destination from being constructed");
    }

    @Test
    public void testValidConstructionWithBearerToken() {
        // Must not throw — full happy path construction with bearer token
        assertDoesNotThrow(() ->
                new OpenTelemetryDestination(
                        null,
                        "valid-bearer",
                        "http://localhost:4318/v1/logs",
                        "Authorization: Bearer secret",
                        null, null, null,
                        512, 5000, 10000,
                        null));
    }

    @Test
    public void testMultipleHeadersAreParsed() {
        // Two headers separated by comma — must not throw
        assertDoesNotThrow(() ->
                new OpenTelemetryDestination(
                        null,
                        "multi-headers",
                        "http://localhost:4318/v1/logs",
                        "Authorization: Bearer abc, X-Tenant: acme",
                        null, null, null,
                        1, 500, 5000,
                        null));
    }

    // -------------------------------------------------------------------------
    // Route wiring
    // -------------------------------------------------------------------------

    @Override
    protected RoutesBuilder[] createRouteBuilders() throws IOException {
        final String serverEndpoint = "http://" + server.getHostName() + ":" + server.getPort() + OTLP_LOGS_PATH;

        destinations.add(TEST_DEST);
        destinations.add(testOutDest);

        return new RoutesBuilder[]{
                new FileEvent(
                        FILE_EVENT,
                        testFile.toString(),
                        null,
                        destinations,
                        null,
                        500,
                        dataMapInjections),
                new OpenTelemetryDestination(
                        null,
                        TEST_DEST,
                        serverEndpoint,
                        "Authorization: Bearer test-token",
                        null, null, null,
                        BATCH_SIZE,
                        BATCH_TIMEOUT,
                        REQUEST_TIMEOUT,
                        null),
                new StreamDestination(context, testOutDest, null, componentOptions)
        };
    }
}
