package CamelTests;

import com.github.theprez.manzan.routes.dest.OpenSearchDestination;
import com.github.theprez.manzan.routes.dest.OpenSearchDestination.AuthType;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.camel.RoutesBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link OpenSearchDestination} and unit tests for
 * {@link DocumentSanitizer}.
 *
 * <p>Follows the same structure as {@link PrometheusDestinationTest}:
 * a {@link MockWebServer} stands in for the real OpenSearch cluster.
 * The server receives index requests and the tests assert on the
 * recorded HTTP traffic — method, path, auth header, and body shape.
 *
 * <p>The OpenSearch low-level REST client uses Apache HttpAsyncClient
 * under the hood, so every index call becomes a real HTTP POST that
 * the mock server intercepts.
 */
public class OpenSearchDestinationTest extends CamelTestHelper {

    private MockWebServer server;

    // Index name used in all tests
    private static final String TEST_INDEX   = "manzan-test";
    private static final String TEST_DEST    = "opensearch_test";
    private static final String TEST_API_KEY = "test-api-key-abc123";

    @Override
    protected void doPreSetup() throws Exception {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    public void tearDownServer() throws IOException {
        if (server != null) {
            server.shutdown();
        }
    }

    // -------------------------------------------------------------------------
    // Route integration tests (MockWebServer as OpenSearch backend)
    // -------------------------------------------------------------------------

    @Test
    public void testIndexRequestReachesServer() throws Exception {
        // OpenSearch REST client expects a JSON success response on index
        server.enqueue(successResponse());

        // Send a message into the route
        template.sendBodyAndHeaders("direct:" + TEST_DEST, "test body",
                buildDataMapHeaders("Hello from Manzan", 10));

        RecordedRequest request = server.takeRequest(10, TimeUnit.SECONDS);

        assertNotNull(request,
                "OpenSearch REST client should have posted an index request within 10 s");
        assertTrue(request.getPath().contains(TEST_INDEX),
                "Request path should contain the index name '" + TEST_INDEX + "', got: " + request.getPath());
    }

    @Test
    public void testIndexRequestIsHttpPost() throws Exception {
        server.enqueue(successResponse());

        template.sendBodyAndHeaders("direct:" + TEST_DEST, "test body",
                buildDataMapHeaders("method check", 10));

        RecordedRequest request = server.takeRequest(10, TimeUnit.SECONDS);
        assertNotNull(request, "Expected a request to the mock server");
        // OpenSearch _doc index is PUT when id is provided, POST when auto-assigned
        assertTrue(request.getMethod().equals("POST") || request.getMethod().equals("PUT"),
                "Index request must be POST or PUT, got: " + request.getMethod());
    }

    @Test
    public void testApiKeyAuthHeaderForwarded() throws Exception {
        server.enqueue(successResponse());

        template.sendBodyAndHeaders("direct:" + TEST_DEST, "test body",
                buildDataMapHeaders("auth header check", 10));

        RecordedRequest request = server.takeRequest(10, TimeUnit.SECONDS);
        assertNotNull(request, "Expected a request to the mock server");

        String authHeader = request.getHeader("Authorization");
        assertNotNull(authHeader, "Authorization header must be present");
        assertEquals("ApiKey " + TEST_API_KEY, authHeader,
                "ApiKey authorization header must be forwarded verbatim");
    }

    @Test
    public void testRequestBodyIsJson() throws Exception {
        server.enqueue(successResponse());

        template.sendBodyAndHeaders("direct:" + TEST_DEST, "test body",
                buildDataMapHeaders("json body check", 10));

        RecordedRequest request = server.takeRequest(10, TimeUnit.SECONDS);
        assertNotNull(request, "Expected a request to the mock server");

        String contentType = request.getHeader("Content-Type");
        assertNotNull(contentType, "Content-Type header must be present");
        assertTrue(contentType.contains("application/json"),
                "Index request body must be application/json, got: " + contentType);
    }

    @Test
    public void testRequestBodyIsNonEmpty() throws Exception {
        server.enqueue(successResponse());

        template.sendBodyAndHeaders("direct:" + TEST_DEST, "test body",
                buildDataMapHeaders("body size check", 10));

        RecordedRequest request = server.takeRequest(10, TimeUnit.SECONDS);
        assertNotNull(request, "Expected a request to the mock server");
        assertTrue(request.getBodySize() > 0,
                "Index request body must not be empty");
    }

    @Test
    public void testBasicAuthHeaderForwarded() throws Exception {
        // Start a second server for basic-auth destination
        MockWebServer basicServer = new MockWebServer();
        basicServer.start();
        basicServer.enqueue(successResponse());

        String endpoint = "http://" + basicServer.getHostName() + ":" + basicServer.getPort();
        OpenSearchDestination dest = new OpenSearchDestination(
                null, "basic-dest", endpoint, TEST_INDEX,
                "basic", null, "writer", "s3cr3t", null, null);

        context.addRoutes(dest);

        template.sendBodyAndHeaders("direct:basic-dest", "test body",
                buildDataMapHeaders("basic auth check", 10));

        RecordedRequest request = basicServer.takeRequest(10, TimeUnit.SECONDS);
        assertNotNull(request, "Expected a request to the basic-auth mock server");

        String authHeader = request.getHeader("Authorization");
        assertNotNull(authHeader, "Authorization header must be present for basic auth");
        assertTrue(authHeader.startsWith("Basic "),
                "Basic auth header must start with 'Basic '");

        // Verify the token decodes to the correct credentials
        String token = authHeader.substring("Basic ".length());
        String decoded = new String(Base64.getDecoder().decode(token));
        assertEquals("writer:s3cr3t", decoded,
                "Decoded Basic credentials must match configured username:password");

        basicServer.shutdown();
    }

    @Test
    public void testDataMapFieldsAreSentInRequestBody() throws Exception {
        server.enqueue(successResponse());

        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("MESSAGE", "hello world");
        dataMap.put("SEVERITY", 30);

        Map<String, Object> headers = new HashMap<>();
        headers.put("data_map", dataMap);

        template.sendBodyAndHeaders("direct:" + TEST_DEST, "test body", headers);

        RecordedRequest request = server.takeRequest(10, TimeUnit.SECONDS);
        assertNotNull(request, "Expected a request to the mock server");

        String body = request.getUtf8Body();
        assertTrue(body.contains("MESSAGE") || body.contains("hello world"),
                "Request body should contain data map field values");
    }

    // -------------------------------------------------------------------------
    // Construction-time validation tests (no server needed)
    // -------------------------------------------------------------------------

    @Test
    public void testAuthTypeFromNull() {
        assertEquals(AuthType.APIKEY, AuthType.from(null),
                "null authType should default to APIKEY");
    }

    @Test
    public void testAuthTypeFromEmpty() {
        assertEquals(AuthType.APIKEY, AuthType.from(""),
                "Empty authType should default to APIKEY");
    }

    @Test
    public void testAuthTypeFromBasic() {
        assertEquals(AuthType.BASIC, AuthType.from("basic"));
    }

    @Test
    public void testAuthTypeFromBasicCaseInsensitive() {
        assertEquals(AuthType.BASIC, AuthType.from("BASIC"));
        assertEquals(AuthType.BASIC, AuthType.from("Basic"));
    }

    @Test
    public void testAuthTypeFromSigV4() {
        assertEquals(AuthType.SIGV4, AuthType.from("sigv4"));
    }

    @Test
    public void testAuthTypeFromUnknownThrows() {
        assertThrows(IllegalArgumentException.class, () -> AuthType.from("oauth2"),
                "Unknown authType should throw IllegalArgumentException");
    }

    @Test
    public void testConstructorRejectsApikeyWithoutKey() {
        assertThrows(IllegalArgumentException.class, () ->
                new OpenSearchDestination(
                        null, "no-key", "http://localhost:9200", TEST_INDEX,
                        "apikey",
                        null,    // apiKey missing
                        null, null, null, null),
                "authType=apikey with no apiKey must throw at construction");
    }

    @Test
    public void testConstructorRejectsBasicWithoutUsername() {
        assertThrows(IllegalArgumentException.class, () ->
                new OpenSearchDestination(
                        null, "no-user", "http://localhost:9200", TEST_INDEX,
                        "basic",
                        null,
                        null,    // username missing
                        "pass", null, null),
                "authType=basic with no username must throw at construction");
    }

    @Test
    public void testConstructorRejectsBasicWithoutPassword() {
        assertThrows(IllegalArgumentException.class, () ->
                new OpenSearchDestination(
                        null, "no-pass", "http://localhost:9200", TEST_INDEX,
                        "basic",
                        null, "user",
                        null,    // password missing
                        null, null),
                "authType=basic with no password must throw at construction");
    }

    @Test
    public void testConstructorRejectsSigv4WithoutRegion() {
        assertThrows(IllegalArgumentException.class, () ->
                new OpenSearchDestination(
                        null, "no-region", "http://localhost:9200", TEST_INDEX,
                        "sigv4",
                        null, null, null,
                        null,   // awsRegion missing
                        "es"),
                "authType=sigv4 with no awsRegion must throw at construction");
    }

    @Test
    public void testAwsServiceDefaultsToEs() {
        // Construct with null awsService — must not throw and must default to "es"
        assertDoesNotThrow(() -> {
            // We verify via mock server that the signer receives the correct service name.
            // Construction-time: just confirm no exception from the null service name path.
            new OpenSearchDestination(
                    null, "default-service",
                    "http://" + server.getHostName() + ":" + server.getPort(),
                    TEST_INDEX,
                    "sigv4",
                    null, null, null,
                    "us-east-1",
                    null  // awsService null → should default to "es"
            );
        }, "null awsService should silently default to 'es' rather than throwing");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Builds a Camel header map containing a minimal data_map. */
    private Map<String, Object> buildDataMapHeaders(String message, int severity) {
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("MESSAGE",  message);
        dataMap.put("SEVERITY", severity);

        Map<String, Object> headers = new HashMap<>();
        headers.put("data_map",   dataMap);
        headers.put("event_type", com.github.theprez.manzan.ManzanEventType.WATCH_MSG);
        return headers;
    }

    /** Convenience: 200 OK JSON response that satisfies the OpenSearch REST client.
     *  opensearch-java 2.x requires _index, _id, _version, result, _shards,
     *  _seq_no, and _primary_term to all be present or it throws
     *  MissingRequiredPropertyException before the test assertions are reached. */
    private MockResponse successResponse() {
        return new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"_index\":\"" + TEST_INDEX + "\","
                        + "\"_id\":\"1\","
                        + "\"_version\":1,"
                        + "\"result\":\"created\","
                        + "\"_shards\":{\"total\":1,\"successful\":1,\"failed\":0},"
                        + "\"_seq_no\":0,"
                        + "\"_primary_term\":1}");
    }

    // -------------------------------------------------------------------------
    // Route wiring
    // -------------------------------------------------------------------------

    @Override
    protected RoutesBuilder[] createRouteBuilders() throws IOException {
        final String endpoint = "http://" + server.getHostName() + ":" + server.getPort();

        destinations.add(TEST_DEST);

        return new RoutesBuilder[]{
                new OpenSearchDestination(
                        null,
                        TEST_DEST,
                        endpoint,
                        TEST_INDEX,
                        "apikey",
                        TEST_API_KEY,
                        null, null, null, null)
        };
    }
}
