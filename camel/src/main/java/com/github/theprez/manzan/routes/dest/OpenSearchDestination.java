package com.github.theprez.manzan.routes.dest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.github.theprez.manzan.InstanceContext;
import com.github.theprez.manzan.ManzanEventType;
import com.github.theprez.manzan.routes.ManzanRoute;

import org.opensearch.client.RestClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.rest_client.RestClientTransport;

/**
 * OpenSearch destination for Manzan events.
 *
 * <p>Mirrors the structure of {@link ElasticsearchDestination} but targets an OpenSearch
 * cluster using the {@code opensearch-java} client. Authentication supports three modes:
 *
 * <ul>
 *   <li><b>apikey</b> – Bearer API key sent as an {@code Authorization} header (default)</li>
 *   <li><b>basic</b>  – HTTP Basic auth (username + password)</li>
 *   <li><b>sigv4</b>  – AWS Signature Version 4 for Amazon OpenSearch Service and
 *                        OpenSearch Serverless (AOSS)</li>
 * </ul>
 *
 * <p>The {@link DocumentSanitizer} utility is shared with {@link ElasticsearchDestination}
 * to normalise oversized numeric values before indexing.
 *
 * <p><b>Configuration keys (dests.ini):</b>
 * <pre>
 * [my-opensearch]
 * type       = opensearch
 * endpoint   = https://opensearch.internal:9200   # required
 * index      = manzan-events                       # required
 * authType   = apikey                              # optional: apikey | basic | sigv4 (default: apikey)
 * apiKey     = bXlfa2V5                            # required when authType=apikey
 * username   = manzan_writer                       # required when authType=basic
 * password   = s3cur3!                             # required when authType=basic
 * awsRegion  = us-east-1                           # required when authType=sigv4
 * awsService = es                                  # optional when authType=sigv4: es | aoss (default: es)
 * </pre>
 */
public class OpenSearchDestination extends ManzanRoute {

    /** Supported authentication modes. */
    public enum AuthType {
        APIKEY, BASIC, SIGV4;

        public static AuthType from(String value) {
            if (value == null || value.isEmpty()) {
                return APIKEY;
            }
            switch (value.toLowerCase()) {
                case "basic":  return BASIC;
                case "sigv4":  return SIGV4;
                case "apikey": return APIKEY;
                default:
                    throw new IllegalArgumentException(
                            "Unknown authType '" + value + "'. Valid values: apikey, basic, sigv4");
            }
        }
    }

    private final String m_index;
    private final OpenSearchClient m_client;

    /**
     * Constructor. All credential and transport setup happens here so failures
     * surface at startup, not at first message delivery.
     *
     * @param ctx        instance context (for route ID scoping)
     * @param name       destination name from dests.ini
     * @param endpoint   full URL, e.g. {@code https://opensearch.internal:9200}
     * @param index      target index name
     * @param authType   one of {@code apikey}, {@code basic}, {@code sigv4}
     * @param apiKey     API key (required when authType=apikey)
     * @param username   username (required when authType=basic)
     * @param password   password (required when authType=basic)
     * @param awsRegion  AWS region (required when authType=sigv4)
     * @param awsService AWS service name — {@code es} or {@code aoss} (default: {@code es})
     */
    public OpenSearchDestination(
            final InstanceContext ctx,
            final String name,
            final String endpoint,
            final String index,
            final String authType,
            final String apiKey,
            final String username,
            final String password,
            final String awsRegion,
            final String awsService) {

        super(ctx, name);

        m_index = index;

        final AuthType auth = AuthType.from(authType);
        validateCredentials(name, auth, apiKey, username, password, awsRegion);

        final HttpRequestInterceptor interceptor = buildInterceptor(
                name, auth, apiKey, username, password, awsRegion,
                awsService != null && !awsService.isEmpty() ? awsService : "es");

        final RestClient restClient = RestClient
                .builder(HttpHost.create(endpoint))
                .setHttpClientConfigCallback(
                        (HttpAsyncClientBuilder builder) -> builder.addInterceptorLast(interceptor))
                .build();

        final RestClientTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper());

        m_client = new OpenSearchClient(transport);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                m_client.shutdown();
            } catch (Exception e) {
                // Best-effort shutdown — do not propagate during JVM exit
                System.err.println("[OpenSearch:" + name + "] Error during shutdown: " + e.getMessage());
            }
        }));
    }

    @Override
    public void configure() {
        from(getInUri())
                .routeId(getRouteId())
                .process(exchange -> {
                    final Map<String, Object> data = DocumentSanitizer.sanitize(getDataMap(exchange));
                    m_client.index(i -> i.index(m_index).document(data));
                });
    }

    @Override
    protected void setEventType(ManzanEventType manzanEventType) {
        // not used — OpenSearch indexing is type-agnostic
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Validates that the credentials required for the chosen auth type are present.
     * Fails fast at construction time with a descriptive message rather than at
     * first message delivery.
     */
    private static void validateCredentials(String name, AuthType auth,
            String apiKey, String username, String password, String awsRegion) {
        switch (auth) {
            case APIKEY:
                if (apiKey == null || apiKey.isEmpty()) {
                    throw new IllegalArgumentException(
                            "[OpenSearch:" + name + "] authType=apikey requires 'apiKey' to be set");
                }
                break;
            case BASIC:
                if (username == null || username.isEmpty()) {
                    throw new IllegalArgumentException(
                            "[OpenSearch:" + name + "] authType=basic requires 'username' to be set");
                }
                if (password == null || password.isEmpty()) {
                    throw new IllegalArgumentException(
                            "[OpenSearch:" + name + "] authType=basic requires 'password' to be set");
                }
                break;
            case SIGV4:
                if (awsRegion == null || awsRegion.isEmpty()) {
                    throw new IllegalArgumentException(
                            "[OpenSearch:" + name + "] authType=sigv4 requires 'awsRegion' to be set");
                }
                break;
        }
    }

    /**
     * Builds the appropriate {@link HttpRequestInterceptor} for the chosen auth type.
     * The interceptor is called once per request on the existing Apache async client.
     * Credentials are captured at construction — nothing is read from config at index time.
     */
    private static HttpRequestInterceptor buildInterceptor(
            String name,
            AuthType auth,
            String apiKey,
            String username,
            String password,
            String awsRegion,
            String awsService) {

        switch (auth) {
            case APIKEY:
                return (request, context) ->
                        request.addHeader("Authorization", "ApiKey " + apiKey);

            case BASIC: {
                // Build the Base64-encoded Basic token once, not per-request
                String credentials = username + ":" + password;
                String encoded = java.util.Base64.getEncoder()
                        .encodeToString(credentials.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                return (request, context) ->
                        request.addHeader("Authorization", "Basic " + encoded);
            }

            case SIGV4:
                return new AwsSigV4Interceptor(awsRegion, awsService);

            default:
                throw new IllegalStateException("Unhandled auth type: " + auth);
        }
    }

    // -------------------------------------------------------------------------
    // Inner class: AWS SigV4 signing interceptor (pure JDK — no AWS SDK signer)
    // -------------------------------------------------------------------------

    /**
     * Apache {@link HttpRequestInterceptor} that signs every outbound request with
     * AWS Signature Version 4 using pure JDK crypto (SHA-256 + HMAC-SHA256).
     *
     * <p>We do not use {@code AWS4Signer} from the AWS SDK because it was designed
     * for the synchronous Apache client: at the interceptor stage the async client
     * has not yet attached the entity to the request, so {@code getEntity()} returns
     * {@code null} and the body hash is always the empty-string SHA-256. This
     * implementation reads the body directly from the {@code Content-Length} /
     * buffered entity that the OpenSearch Java client places on the request before
     * handing it to the interceptor chain.
     */
    private static final class AwsSigV4Interceptor implements HttpRequestInterceptor {

        private static final String ALGORITHM = "AWS4-HMAC-SHA256";
        private static final String HMAC_ALGO  = "HmacSHA256";

        private final String m_region;
        private final String m_service;
        private final DefaultAWSCredentialsProviderChain m_credentialsProvider;

        AwsSigV4Interceptor(String region, String service) {
            m_region  = region;
            m_service = service;
            // Resolves: env vars → system props → ~/.aws/credentials → instance role
            m_credentialsProvider = new DefaultAWSCredentialsProviderChain();
        }

        @Override
        public void process(
                org.apache.http.HttpRequest request,
                org.apache.http.protocol.HttpContext context)
                throws org.apache.http.HttpException, IOException {

            // ---- 1. Buffer body ------------------------------------------------
            byte[] bodyBytes = new byte[0];
            if (request instanceof org.apache.http.HttpEntityEnclosingRequest) {
                final org.apache.http.HttpEntity entity =
                        ((org.apache.http.HttpEntityEnclosingRequest) request).getEntity();
                if (entity != null) {
                    bodyBytes = org.apache.http.util.EntityUtils.toByteArray(entity);
                    // Restore so Apache can still send the body
                    ((org.apache.http.HttpEntityEnclosingRequest) request).setEntity(
                            new org.apache.http.entity.ByteArrayEntity(
                                    bodyBytes,
                                    org.apache.http.entity.ContentType.get(entity)));
                }
            }

            // ---- 2. Timestamps -------------------------------------------------
            final SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd");
            dateFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
            final SimpleDateFormat isoFmt = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
            isoFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
            final Date now     = new Date();
            final String dateStamp = dateFmt.format(now);
            final String amzDate   = isoFmt.format(now);

            // ---- 3. Collect headers to sign ------------------------------------
            // AWS always computes the canonical host without a port suffix, even for
            // non-default ports. Strip any trailing ":port" from the Host header value.
            String host = request.getFirstHeader("Host") != null
                    ? request.getFirstHeader("Host").getValue()
                    : "";
            final int portColon = host.lastIndexOf(':');
            if (portColon >= 0) {
                host = host.substring(0, portColon);
            }
            request.setHeader("x-amz-date", amzDate);

            // Build the canonical + signed headers maps (TreeMap = alphabetical order)
            final TreeMap<String, String> headersToSign = new TreeMap<>();
            headersToSign.put("host",       host);
            headersToSign.put("x-amz-date", amzDate);

            // ---- 4. Canonical request ------------------------------------------
            final String method     = request.getRequestLine().getMethod();
            final String requestUri = request.getRequestLine().getUri();
            final String canonicalUri = requestUri.contains("?")
                    ? requestUri.substring(0, requestUri.indexOf('?'))
                    : requestUri;
            final String canonicalQuery = requestUri.contains("?")
                    ? sortQueryString(requestUri.substring(requestUri.indexOf('?') + 1))
                    : "";

            final StringBuilder canonicalHeadersSb = new StringBuilder();
            final StringBuilder signedHeadersSb     = new StringBuilder();
            for (Map.Entry<String, String> e : headersToSign.entrySet()) {
                canonicalHeadersSb.append(e.getKey()).append(':').append(e.getValue().trim()).append('\n');
                if (signedHeadersSb.length() > 0) signedHeadersSb.append(';');
                signedHeadersSb.append(e.getKey());
            }
            final String signedHeaders = signedHeadersSb.toString();
            final String payloadHash   = sha256Hex(bodyBytes);

            // canonicalHeadersSb already ends with '\n'; AWS spec requires one more '\n'
            // (blank line) before the signed headers line.
            final String canonicalRequest =
                    method + "\n" +
                    canonicalUri + "\n" +
                    canonicalQuery + "\n" +
                    canonicalHeadersSb.toString() + "\n" +
                    signedHeaders + "\n" +
                    payloadHash;

            // ---- 5. String to sign ---------------------------------------------
            final String credentialScope = dateStamp + "/" + m_region + "/" + m_service + "/aws4_request";
            final String stringToSign    =
                    ALGORITHM + "\n" +
                    amzDate + "\n" +
                    credentialScope + "\n" +
                    sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));

            // ---- 6. Signing key ------------------------------------------------
            // Read env vars directly — DefaultAWSCredentialsProviderChain may not see
            // the shell environment on IBM i PASE due to JVM env inheritance differences.
            String accessKeyId = System.getenv("AWS_ACCESS_KEY_ID");
            String secretKey   = System.getenv("AWS_SECRET_ACCESS_KEY");
            if (accessKeyId == null || secretKey == null) {
                final AWSCredentials creds = m_credentialsProvider.getCredentials();
                accessKeyId = creds.getAWSAccessKeyId();
                secretKey   = creds.getAWSSecretKey();
            }
            final byte[] signingKey = getSigningKey(secretKey, dateStamp, m_region, m_service);
            final String signature  = hexEncode(hmacSha256(signingKey, stringToSign.getBytes(StandardCharsets.UTF_8)));

            // ---- 7. Authorization header ---------------------------------------
            final String authorization =
                    ALGORITHM + " " +
                    "Credential=" + accessKeyId + "/" + credentialScope + ", " +
                    "SignedHeaders=" + signedHeaders + ", " +
                    "Signature=" + signature;

            request.setHeader("Authorization", authorization);
        }

        @Override
        public String toString() {
            return "AwsSigV4Interceptor{region=" + m_region + ", service=" + m_service + "}";
        }

        // ---- Crypto helpers ----------------------------------------------------

        private static String sha256Hex(byte[] data) throws IOException {
            try {
                final MessageDigest md = MessageDigest.getInstance("SHA-256");
                return hexEncode(md.digest(data));
            } catch (NoSuchAlgorithmException e) {
                throw new IOException("SHA-256 not available", e);
            }
        }

        private static String sha256Hex(String data) throws IOException {
            return sha256Hex(data.getBytes(StandardCharsets.UTF_8));
        }

        private static byte[] hmacSha256(byte[] key, byte[] data) throws IOException {
            try {
                final Mac mac = Mac.getInstance(HMAC_ALGO);
                mac.init(new SecretKeySpec(key, HMAC_ALGO));
                return mac.doFinal(data);
            } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                throw new IOException("HMAC-SHA256 failed", e);
            }
        }

        private static byte[] getSigningKey(String secretKey, String date, String region, String service)
                throws IOException {
            final byte[] kSecret  = ("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8);
            final byte[] kDate    = hmacSha256(kSecret,  date.getBytes(StandardCharsets.UTF_8));
            final byte[] kRegion  = hmacSha256(kDate,    region.getBytes(StandardCharsets.UTF_8));
            final byte[] kService = hmacSha256(kRegion,  service.getBytes(StandardCharsets.UTF_8));
            return hmacSha256(kService, "aws4_request".getBytes(StandardCharsets.UTF_8));
        }

        private static String hexEncode(byte[] bytes) {
            final StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }

        private static String sortQueryString(String query) {
            if (query == null || query.isEmpty()) return "";
            final List<String> pairs = new ArrayList<>();
            for (String part : query.split("&")) {
                pairs.add(part);
            }
            Collections.sort(pairs);
            final StringBuilder sb = new StringBuilder();
            for (String p : pairs) {
                if (sb.length() > 0) sb.append('&');
                sb.append(p);
            }
            return sb.toString();
        }
    }
}
