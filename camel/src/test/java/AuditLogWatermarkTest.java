import com.github.theprez.manzan.InstanceContext;
import com.github.theprez.manzan.routes.event.AuditLog;
import com.github.theprez.manzan.routes.ManzanRoute;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Unit tests for AuditLog watermark SQL scoping.
 *
 * Verifies that the SESSION_ID embedded in the generated SELECT and MERGE
 * statements is specific to the owning instance, so two concurrent instances
 * never share or overwrite each other's high-water mark in AUDJRNTS.
 *
 * No IBM i connection, no Camel context, and no database are required —
 * all assertions operate on the SQL strings constructed at object creation.
 * Private fields are accessed via reflection.
 */
public class AuditLogWatermarkTest {

    private static final String INSTANCE_A    = "audit-wm-test-a";
    private static final String INSTANCE_B    = "audit-wm-test-b";
    private static final String AUDIT_TYPE    = "PASSWORD";
    private static final String ROUTE_NAME    = "audit";
    private static final int    INTERVAL      = 1000;
    private static final int    NUM_TO_PROCESS = 100;
    private static final int    FALLBACK_HOURS = 24;
    private static final List<String>      DESTINATIONS = Collections.singletonList("test-dest");
    private static final Map<String,String> NO_INJECTIONS = Collections.emptyMap();

    @Before
    public void setUp() {
        cleanupTestDirectories();
    }

    @After
    public void tearDown() {
        cleanupTestDirectories();
    }

    // -------------------------------------------------------------------------
    // SQL content tests
    // -------------------------------------------------------------------------

    @Test
    public void testSelectSqlContainsSessionId() throws Exception {
        InstanceContext ctx = InstanceContext.fromEnvironment(
                new String[]{"--instance=" + INSTANCE_A});
        AuditLog route = new AuditLog(ctx, ROUTE_NAME, null, DESTINATIONS,
                INTERVAL, NUM_TO_PROCESS, AUDIT_TYPE, FALLBACK_HOURS, NO_INJECTIONS);

        String sql = getSqlField(route);
        String sessionId = getSessionIdField(route);

        assertNotNull("m_sql must not be null", sql);
        assertNotNull("m_sessionId must not be null", sessionId);
        assertTrue("SELECT SQL must contain SESSION_ID='<id>' in WHERE clause",
                sql.contains("SESSION_ID='" + sessionId + "'"));
    }

    @Test
    public void testSelectSqlContainsAuditTypeInWatermarkClause() throws Exception {
        InstanceContext ctx = InstanceContext.fromEnvironment(
                new String[]{"--instance=" + INSTANCE_A});
        AuditLog route = new AuditLog(ctx, ROUTE_NAME, null, DESTINATIONS,
                INTERVAL, NUM_TO_PROCESS, AUDIT_TYPE, FALLBACK_HOURS, NO_INJECTIONS);

        String sql = getSqlField(route);
        assertTrue("SELECT SQL must contain AUDTYPE='PASSWORD' in watermark subquery",
                sql.contains("AUDTYPE='" + AUDIT_TYPE + "'"));
    }

    @Test
    public void testTwoInstancesProduceDifferentSqlDueToDistinctSessionIds() throws Exception {
        InstanceContext ctxA = InstanceContext.fromEnvironment(
                new String[]{"--instance=" + INSTANCE_A});
        InstanceContext ctxB = InstanceContext.fromEnvironment(
                new String[]{"--instance=" + INSTANCE_B});

        AuditLog routeA = new AuditLog(ctxA, ROUTE_NAME, null, DESTINATIONS,
                INTERVAL, NUM_TO_PROCESS, AUDIT_TYPE, FALLBACK_HOURS, NO_INJECTIONS);
        AuditLog routeB = new AuditLog(ctxB, ROUTE_NAME, null, DESTINATIONS,
                INTERVAL, NUM_TO_PROCESS, AUDIT_TYPE, FALLBACK_HOURS, NO_INJECTIONS);

        String sqlA = getSqlField(routeA);
        String sqlB = getSqlField(routeB);
        String sidA = getSessionIdField(routeA);
        String sidB = getSessionIdField(routeB);

        assertNotEquals("Two instances must generate different session IDs", sidA, sidB);
        assertNotEquals("Two instances must generate different watermark SQL", sqlA, sqlB);

        // Each SQL must reference its own session ID, not the other's
        assertTrue("Instance A's SQL must contain its own session ID",  sqlA.contains(sidA));
        assertTrue("Instance B's SQL must contain its own session ID",  sqlB.contains(sidB));
        assertFalse("Instance A's SQL must not contain instance B's session ID", sqlA.contains(sidB));
        assertFalse("Instance B's SQL must not contain instance A's session ID", sqlB.contains(sidA));
    }

    @Test
    public void testDeprecatedConstructorConstructsWithoutException() throws Exception {
        // The deprecated 8-arg constructor must still be accepted. It delegates to
        // InstanceContext.getDefault() — the default instance config dir will be
        // created as a side-effect but that is cleaned up in tearDown.
        @SuppressWarnings("deprecation")
        AuditLog route = new AuditLog(ROUTE_NAME, null, DESTINATIONS,
                INTERVAL, NUM_TO_PROCESS, AUDIT_TYPE, FALLBACK_HOURS, NO_INJECTIONS);

        assertNotNull("Deprecated AuditLog constructor must succeed", route);

        // SQL must still contain SESSION_ID scoping via the default instance's session
        String sql = getSqlField(route);
        assertTrue("SQL from deprecated constructor must still contain SESSION_ID= clause",
                sql.contains("SESSION_ID='"));
    }

    @Test
    public void testRouteIdIsInstanceScoped() throws Exception {
        InstanceContext ctx = InstanceContext.fromEnvironment(
                new String[]{"--instance=" + INSTANCE_A});
        AuditLog route = new AuditLog(ctx, ROUTE_NAME, null, DESTINATIONS,
                INTERVAL, NUM_TO_PROCESS, AUDIT_TYPE, FALLBACK_HOURS, NO_INJECTIONS);

        String routeId = getRouteId(route);

        // Expected: "manzan-<instance>:<route-name>"
        String expected = "manzan-" + INSTANCE_A + ":" + ROUTE_NAME;
        assertEquals("Route ID must be instance-scoped", expected, routeId);
    }

    // -------------------------------------------------------------------------
    // Reflection helpers
    // -------------------------------------------------------------------------

    private static String getSqlField(AuditLog route) throws Exception {
        Field f = AuditLog.class.getDeclaredField("m_sql");
        f.setAccessible(true);
        return (String) f.get(route);
    }

    private static String getSessionIdField(AuditLog route) throws Exception {
        Field f = AuditLog.class.getDeclaredField("m_sessionId");
        f.setAccessible(true);
        return (String) f.get(route);
    }

    private static String getRouteId(AuditLog route) throws Exception {
        Method m = ManzanRoute.class.getDeclaredMethod("getRouteId");
        m.setAccessible(true);
        return (String) m.invoke(route);
    }

    // -------------------------------------------------------------------------
    // Cleanup
    // -------------------------------------------------------------------------

    private void cleanupTestDirectories() {
        for (String name : new String[]{INSTANCE_A, INSTANCE_B, "default"}) {
            deleteDirectory(new File("/QOpenSys/etc/manzan-" + name));
        }
    }

    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) deleteDirectory(f);
            }
        }
        dir.delete();
    }
}
